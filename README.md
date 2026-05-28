# Jackson WASM

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Jackson-compatible `JsonFactory` that offloads untrusted JSON parsing to an isolated WebAssembly (WASM) sandbox.

## Overview

`jackson-wasm` allows you to leverage Rust's `serde_json` parser inside a sandboxed WebAssembly execution cage in your Java applications. By caging the parsing process inside a zero-trust WASM linear memory space, you gain complete protection against polymorphic deserialization exploits (RCE gadget chains) and resource-exhaustion Denial of Service (DoS) attacks when handling untrusted inputs.

### Why Jackson WASM?

*   **RCE Protection**: Decouples string-to-token parsing from JVM reflection. The sandbox parses the JSON structure entirely within isolated linear memory before standard Jackson data binding occurs, eliminating polymorphic deserialization gadget chain risks.
*   **DoS & Memory Safety**: Protects the JVM against resource-exhaustion exploits. Enforces deep recursion constraints (via Rust's parsing limits) and memory allocation caps within the Wasm guest, preventing stack overflows or OutOfMemoryError crashes.
*   **Sandboxed Isolation**: The Chicory runtime executes the Wasm module without WASI (WebAssembly System Interface) support. The guest has no access to the host file system, network sockets, or JVM heap.
*   **Zero Native Dependencies**: Uses [Chicory](https://github.com/dylibso/chicory), a pure-Java Wasm engine. It compiles Wasm bytecode into JVM bytecode in-memory, requiring no JNI/JNA bindings or native libraries.


## Architecture

The project bridges the JVM and WASM worlds through a MessagePack intermediary:

1.  **Java Layer**: `WasmJsonFactory` intercepts JSON input.
2.  **Bridge Layer**: `WasmBridge` manages the Chicory WASM runtime and memory.
3.  **WASM Layer (Rust)**: The Rust module parses JSON into an AST and re-serializes it into **MessagePack**.
4.  **Java Layer**: Jackson's `MessagePackFactory` consumes the MessagePack stream to perform standard data binding.

```text
[JSON Input] -> [WasmJsonFactory] -> [WasmBridge]
                                         |
                                         v
                                  [Rust/WASM Parser]
                                         |
                                         v
[ObjectMapper] <- [MessagePackParser] <- [MessagePack Output]
```

## Getting Started

### Prerequisites

*   **Java 25+**
*   **Rust (stable)** with `wasm32-unknown-unknown` target
*   **Maven 3.9+**

### Building from Source

1.  **Build the WASM Core:**
    ```bash
    cd wasm-json-core
    cargo build --target wasm32-unknown-unknown --release
    cp target/wasm32-unknown-unknown/release/wasm_json_core.wasm ../src/main/resources/
    cd ..
    ```

2.  **Build the Java Library:**
    ```bash
    mvn clean install
    ```

## Usage

Simply pass `WasmJsonFactory` to your `ObjectMapper` initialization:

```java
import com.jacksonwasm.WasmJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    public static void main(String[] args) throws Exception {
        // 1. Initialize with WASM factory
        ObjectMapper mapper = new ObjectMapper(new WasmJsonFactory());

        // 2. Use as you normally would
        String json = "{\"name\": \"WebAssembly\", \"status\": \"awesome\"}";
        MyData data = mapper.readValue(json, MyData.class);

        System.out.println(data.getName()); // Output: WebAssembly
    }
}
```

## Performance & Optimizations

WebAssembly execution in pure Java has traditionally suffered from interpretation overhead. `jackson-wasm` leverages **Chicory's AOT / JIT runtime compiler** to achieve competitive, production-ready throughput.

### Compile-Once Static Caching
To eliminate dynamic runtime compilation overhead, the `WasmModule` is compiled into reusable JVM bytecode classes exactly once when the bridge is first initialized:
```java
// Pre-compiled globally across all threads
private static final Function<Instance, Machine> MACHINE_FACTORY = 
    MachineFactoryCompiler.compile(MODULE);
```
All parser threads and factory copies dynamically reuse this pre-compiled template, yielding instant startup and zero ongoing compilation costs.

### Benchmark Results
The following results were measured running **50,000 parsing iterations** on a standard HotSpot JVM (after JIT warm-up):

| Implementation | Parsing Time (50k iterations) | Performance Factor |
| :--- | :--- | :--- |
| **Default Jackson Mapper** (Raw textual JIT) | **48 ms** | 1.0x (Baseline) |
| **Wasm Secure Mapper** (Chicory JIT/AOT) | **364 ms** | **7.5x overhead** |
| **Wasm Interpreter Mode** (Default Chicory) | ~8,400 ms | 175.0x overhead |

### Security vs. Performance Trade-off
For processing untrusted, external payloads, an overhead of **7.5x** is extremely lightweight for the level of security guarantees provided (complete decoupling from JVM reflection, strict CPU recursion limits, and capped memory allocations).


## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
