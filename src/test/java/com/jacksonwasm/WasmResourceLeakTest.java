package com.jacksonwasm;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class WasmResourceLeakTest {

    @Test
    public void testFailedParsesDoNotLeakMemory() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new WasmJsonFactory());
        
        // A malformed JSON string that will allocate memory but fail to parse.
        // The WasmBridge must deallocate the input buffer in the finally block.
        String malformedJson = "{\"largeString\": \"" + "a".repeat(10000) + "\"";
        
        // Loop enough times that a leak would exhaust Wasm memory pages or JVM memory
        for (int i = 0; i < 5000; i++) {
            try {
                mapper.readTree(malformedJson);
                fail("Should have thrown exception on malformed JSON");
            } catch (JsonParseException e) {
                // Expected
            } catch (Exception e) {
                // Not expected
                fail("Unexpected exception during parse: " + e.getMessage());
            }
        }
        
        // Now attempt a valid parse to prove the instance is still healthy
        String validJson = "{\"status\": \"healthy\"}";
        String status = mapper.readTree(validJson).get("status").asText();
        
        assertEquals("healthy", status, "Wasm instance should still be healthy and able to parse after 5000 failures.");
    }
}
