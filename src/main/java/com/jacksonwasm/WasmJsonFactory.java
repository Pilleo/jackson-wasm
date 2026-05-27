package com.jacksonwasm;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class WasmJsonFactory extends MessagePackFactory {

    private static final ThreadLocal<WasmBridge> bridgePool = ThreadLocal.withInitial(() -> new WasmBridge());

    public WasmJsonFactory() {
        super();
    }

    public WasmJsonFactory(WasmJsonFactory src) {
        super(src);
    }

    @Override
    public WasmJsonFactory copy() {
        return new WasmJsonFactory(this);
    }

    @Override
    public JsonParser createParser(byte[] data, int offset, int len) throws IOException {
        byte[] inputBytes;
        if (offset == 0 && len == data.length) {
            inputBytes = data;
        } else {
            inputBytes = new byte[len];
            System.arraycopy(data, offset, inputBytes, 0, len);
        }

        try {
            WasmBridge bridge = bridgePool.get();
            byte[] msgpackData = bridge.parse(inputBytes);
            return super.createParser(msgpackData, 0, msgpackData.length);
        } catch (IOException e) {
            throw new JsonParseException(null, "Failed to parse JSON via Wasm: " + e.getMessage(), e);
        }
    }

    @Override
    public JsonParser createParser(byte[] data) throws IOException {
        return createParser(data, 0, data.length);
    }

    @Override
    public JsonParser createParser(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return createParser(buffer.toByteArray(), 0, buffer.size());
    }

    @Override
    public JsonParser createParser(Reader r) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = r.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        return createParser(data, 0, data.length);
    }

    @Override
    public JsonParser createParser(String content) throws IOException {
        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        return createParser(data, 0, data.length);
    }
    
    @Override
    public JsonParser createParser(char[] content, int offset, int len) throws IOException {
        byte[] data = new String(content, offset, len).getBytes(StandardCharsets.UTF_8);
        return createParser(data, 0, data.length);
    }
}
