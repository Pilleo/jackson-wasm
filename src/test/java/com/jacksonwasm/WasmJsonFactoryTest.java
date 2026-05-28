package com.jacksonwasm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class WasmJsonFactoryTest {

    public record User(String name, int age) {}

    @Test
    public void testBasicParsing() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new WasmJsonFactory());
        String json = "{\"name\": \"Alice\", \"age\": 30}";
        User user = mapper.readValue(json, User.class);
        assertNotNull(user);
        assertEquals("Alice", user.name());
        assertEquals(30, user.age());
    }


    @Test
    public void testNullHandling() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new WasmJsonFactory());
        String jsonNull = "null";
        Object result = mapper.readValue(jsonNull, Object.class);
        assertNull(result);
    }
    
    @Test
    public void testConcurrency() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new WasmJsonFactory());
        int threadCount = 10;
        int requestsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        String json = "{\"name\": \"ThreadUser\", \"age\": " + j + "}";
                        User user = mapper.readValue(json, User.class);
                        if ("ThreadUser".equals(user.name()) && user.age() == j) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        assertEquals(threadCount * requestsPerThread, successCount.get());
    }

    @Test
    public void testFactoryCopy() throws Exception {
        WasmJsonFactory factory = new WasmJsonFactory();
        WasmJsonFactory copy = factory.copy();
        ObjectMapper mapper = new ObjectMapper(copy);
        String json = "{\"copyTest\": true}";
        var node = mapper.readTree(json);
        assertTrue(node.get("copyTest").asBoolean());
    }

    @Test
    public void testExcessivelyNestedJson() throws Exception {
        // Deeply nested JSON array to trigger the Wasm/serde_json recursion limits
        String json = "[".repeat(200) + "1" + "]".repeat(200);
        ObjectMapper mapper = new ObjectMapper(new WasmJsonFactory());
        assertThrows(com.fasterxml.jackson.core.JsonParseException.class, () -> {
            mapper.readTree(json);
        });
    }
}

