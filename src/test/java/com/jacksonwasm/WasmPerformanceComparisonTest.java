package com.jacksonwasm;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class WasmPerformanceComparisonTest {

    public record User(String name, int age) {
    }

    @Test
    @Disabled
    public void runPerformanceComparison() throws Exception {
        String json = "{\"name\": \"Alice\", \"age\": 30}";

        ObjectMapper defaultMapper = new ObjectMapper(); // Default Jackson JsonFactory
        ObjectMapper wasmMapper = new ObjectMapper(new WasmJsonFactory()); // WasmJsonFactory

        int warmUpIterations = 10000;
        int testIterations = 50000;

        // 1. Warm-up Default Mapper
        for (int i = 0; i < warmUpIterations; i++) {
            defaultMapper.readValue(json, User.class);
        }

        // 2. Warm-up Wasm Mapper
        for (int i = 0; i < warmUpIterations; i++) {
            wasmMapper.readValue(json, User.class);
        }

        // 3. Measure Default Mapper
        long startTime = System.nanoTime();
        for (int i = 0; i < testIterations; i++) {
            defaultMapper.readValue(json, User.class);
        }
        long defaultDurationMs = (System.nanoTime() - startTime) / 1000000;

        // 4. Measure Wasm Mapper
        startTime = System.nanoTime();
        for (int i = 0; i < testIterations; i++) {
            wasmMapper.readValue(json, User.class);
        }
        long wasmDurationMs = (System.nanoTime() - startTime) / 1000000;

        System.out.println("=================================================");
        System.out.println("PERFORMANCE COMPARISON (" + testIterations + " iterations):");
        System.out.println("  Default Jackson Mapper : " + defaultDurationMs + " ms");
        System.out.println("  Wasm Secure Mapper    : " + wasmDurationMs + " ms");
        System.out.println("  Factor difference     : " + (double) wasmDurationMs / defaultDurationMs + "x");
        System.out.println("=================================================");
    }
}
