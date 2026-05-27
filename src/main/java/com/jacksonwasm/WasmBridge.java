package com.jacksonwasm;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.Value;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WasmBridge implements AutoCloseable {
    private static final WasmModule MODULE;

    static {
        try (InputStream wasmStream = WasmBridge.class.getResourceAsStream("/wasm_json_core.wasm")) {
            if (wasmStream == null) {
                throw new IllegalStateException("Could not find wasm_json_core.wasm in resources");
            }
            MODULE = Parser.parse(wasmStream);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Instance instance;
    private final Memory memory;

    public WasmBridge() {
        this.instance = Instance.builder(MODULE).build();
        this.memory = instance.memory();
    }

    private int callInt(String name, Value... args) {
        long[] rawArgs = new long[args.length];
        for (int i = 0; i < args.length; i++) {
            rawArgs[i] = args[i].raw();
        }
        long[] results = instance.export(name).apply(rawArgs);
        if (results == null || results.length == 0) {
            return 0;
        }
        return (int) results[0];
    }

    public byte[] parse(byte[] jsonBytes) throws IOException {
        int len = jsonBytes.length;

        // 1. Allocate memory in Wasm for input
        int inputPtr = callInt("allocate", Value.i32(len));
        if (inputPtr < 0) {
            throw new IOException("Invalid memory allocation offset returned from Wasm: " + inputPtr);
        }
        
        try {
            // Validate input bounds BEFORE writing
            long maxMemoryBytes = (long) memory.pages() * 65536;
            if ((long) inputPtr + len > maxMemoryBytes) {
                throw new IOException("Input memory write out of bounds: inputPtr=" + inputPtr + ", len=" + len + ", memory size=" + maxMemoryBytes);
            }

            // 2. Write JSON to Wasm memory
            memory.write(inputPtr, jsonBytes);

            // 3. Call Rust parser
            int resultPtr = callInt("parse_to_msgpack", Value.i32(inputPtr), Value.i32(len));
            if (resultPtr < 0) {
                throw new IOException("Wasm parse error. Invalid memory offset returned: " + resultPtr);
            }

            // Validate result header bounds BEFORE reading length prefix
            if ((long) resultPtr + 4 > maxMemoryBytes) {
                throw new IOException("Result header read out of bounds: resultPtr=" + resultPtr + ", memory size=" + maxMemoryBytes);
            }

            // 4. Read MessagePack length prefix (4 bytes, Little Endian)
            byte[] lenHeader = memory.readBytes(resultPtr, 4);
            int msgpackLen = ByteBuffer.wrap(lenHeader).order(ByteOrder.LITTLE_ENDIAN).getInt();

            if (msgpackLen < 0) {
                throw new IOException("Negative MessagePack length returned from Wasm: " + msgpackLen);
            }
            
            // Mitigate JVM OutOfMemoryError by limiting max MessagePack payload size to 50MB
            if (msgpackLen > 50 * 1024 * 1024) {
                throw new IOException("MessagePack payload exceeds maximum limit of 50MB: " + msgpackLen);
            }

            // Validate total result payload bounds BEFORE reading actual data
            if ((long) resultPtr + 4 + msgpackLen > maxMemoryBytes) {
                throw new IOException("Result data read out of bounds: resultPtr=" + resultPtr + ", msgpackLen=" + msgpackLen + ", memory size=" + maxMemoryBytes);
            }

            // 5. Read actual MessagePack data
            byte[] msgpackData = memory.readBytes(resultPtr + 4, msgpackLen);

            // 6. Deallocate the result buffer in Rust (Rust allocated 4 + msgpackLen)
            callInt("deallocate", Value.i32(resultPtr), Value.i32(4 + msgpackLen));

            return msgpackData;
        } finally {
            // 7. Deallocate input buffer
            callInt("deallocate", Value.i32(inputPtr), Value.i32(len));
        }
    }

    @Override
    public void close() {
        // Any resources that need closing
    }
}
