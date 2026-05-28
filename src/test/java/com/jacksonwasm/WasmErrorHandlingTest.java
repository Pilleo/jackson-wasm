package com.jacksonwasm;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class WasmErrorHandlingTest {

    private ObjectMapper mapper = new ObjectMapper(new WasmJsonFactory());

    @Test
    public void testMalformedJsonThrowsParseException() {
        String invalidJson = "{\"key\": \"value\""; // Missing closing brace
        
        assertThrows(JsonParseException.class, () -> {
            mapper.readTree(invalidJson);
        });
    }

    @Test
    public void testInvalidNumberThrowsParseException() {
        String invalidJson = "{\"number\": 12.34.56}"; 
        
        assertThrows(JsonParseException.class, () -> {
            mapper.readTree(invalidJson);
        });
    }

    @Test
    public void testTrailingCommaThrowsParseException() {
        // Standard JSON does not allow trailing commas
        String invalidJson = "{\"key\": \"value\",}"; 
        
        assertThrows(JsonParseException.class, () -> {
            mapper.readTree(invalidJson);
        });
    }

    @Test
    public void testUnquotedKeysThrowsParseException() {
        // Standard JSON requires quoted keys
        String invalidJson = "{key: \"value\"}"; 
        
        assertThrows(JsonParseException.class, () -> {
            mapper.readTree(invalidJson);
        });
    }

    @Test
    public void testTruncatedJsonThrowsParseException() {
        String truncatedJson = "{\"key\": "; 
        
        assertThrows(JsonParseException.class, () -> {
            mapper.readTree(truncatedJson);
        });
    }

    @Test
    public void testInvalidUtf8ThrowsParseException() {
        // 0xFF is an invalid UTF-8 byte
        byte[] invalidUtf8 = "{\"key\": \"".getBytes();
        byte[] full = new byte[invalidUtf8.length + 2];
        System.arraycopy(invalidUtf8, 0, full, 0, invalidUtf8.length);
        full[invalidUtf8.length] = (byte) 0xFF;
        full[invalidUtf8.length + 1] = (byte) '\"';
        
        assertThrows(JsonParseException.class, () -> {
            mapper.readTree(full);
        });
    }
}
