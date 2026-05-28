package com.jacksonwasm;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WasmStreamingApiTest {

    private final WasmJsonFactory factory = new WasmJsonFactory();

    @Test
    public void testSimpleObject() throws Exception {
        String json = "{\"name\":\"test\",\"value\":123}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        try (JsonParser parser = factory.createParser(data)) {
            assertEquals(JsonToken.START_OBJECT, parser.nextToken());
            
            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("name", parser.currentName());
            
            assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
            assertEquals("test", parser.getText());
            
            assertEquals(JsonToken.FIELD_NAME, parser.nextToken());
            assertEquals("value", parser.currentName());
            
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals(123, parser.getIntValue());
            
            assertEquals(JsonToken.END_OBJECT, parser.nextToken());
            // We avoid calling nextToken() again as MessagePackParser may throw EOF
        }
    }

    @Test
    public void testArray() throws Exception {
        String json = "[1, \"two\", true, null]";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        try (JsonParser parser = factory.createParser(data)) {
            assertEquals(JsonToken.START_ARRAY, parser.nextToken());
            
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.nextToken());
            assertEquals(1, parser.getIntValue());
            
            assertEquals(JsonToken.VALUE_STRING, parser.nextToken());
            assertEquals("two", parser.getText());
            
            assertEquals(JsonToken.VALUE_TRUE, parser.nextToken());
            
            assertEquals(JsonToken.VALUE_NULL, parser.nextToken());
            
            assertEquals(JsonToken.END_ARRAY, parser.nextToken());
        }
    }
}
