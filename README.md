# Jackson WASM

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Jackson-compatible `JsonFactory` that offloads JSON parsing to a high-performance, sandboxed WebAssembly (WASM) module written in Rust.

## Overview

`jackson-wasm` allows you to leverage the security and performance of Rust's `serde_json` within your existing Jackson-based Java applications. By running the core parser inside a WebAssembly sandbox, you gain an extra layer of protection against memory-related vulnerabilities when processing untrusted JSON.

### Why Jackson WASM?

*   **Security (Sandboxed Execution):** The parser runs in an isolated linear memory space. It has no access to the host system, files, or network.
*   **Memory Safety:** Built on Rust's `serde_json`, minimizing common parsing vulnerabilities.
*   **Seamless Integration:** Plugs directly into Jackson's `ObjectMapper`. Your data-binding code remains unchanged.
*   **Runtime Independence:** Uses [Chicory](https://github.com/dylibso/chicory), a pure-Java WASM runtime, requiring no native libraries (JNI/JNA).

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

## Performance Considerations

While WebAssembly provides a secure sandbox, there is a small overhead for:
1.  Copying memory between the JVM and WASM linear memory.
2.  The intermediate translation from JSON to MessagePack.

For most applications, this overhead is negligible compared to the security benefits, especially when dealing with untrusted inputs.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
