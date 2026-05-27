package com.jacksonwasm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WasmTypeMappingTest {

    private ObjectMapper mapper = new ObjectMapper(new WasmJsonFactory());

    @Test
    public void testStringHandling() throws Exception {
        String json = "{\"simple\":\"value\", \"emoji\":\"😎\", \"escaped\":\"line\\nbreak\", \"empty\":\"\"}";
        JsonNode node = mapper.readTree(json);
        
        assertEquals("value", node.get("simple").asText());
        assertEquals("😎", node.get("emoji").asText());
        assertEquals("line\nbreak", node.get("escaped").asText());
        assertEquals("", node.get("empty").asText());
    }

    @Test
    public void testNumberHandling() throws Exception {
        String json = "{\"int\": 42, \"negative\": -100, \"largeInt\": 9007199254740991, \"float\": 3.14159, \"scientific\": 1.23e-4}";
        JsonNode node = mapper.readTree(json);
        
        assertEquals(42, node.get("int").asInt());
        assertEquals(-100, node.get("negative").asInt());
        assertEquals(9007199254740991L, node.get("largeInt").asLong());
        assertEquals(3.14159, node.get("float").asDouble(), 0.000001);
        assertEquals(0.000123, node.get("scientific").asDouble(), 0.0000001);
    }

    @Test
    public void testBooleanAndNull() throws Exception {
        String json = "{\"isTrue\": true, \"isFalse\": false, \"isNull\": null}";
        JsonNode node = mapper.readTree(json);
        
        assertTrue(node.get("isTrue").isBoolean());
        assertTrue(node.get("isTrue").asBoolean());
        
        assertTrue(node.get("isFalse").isBoolean());
        assertTrue(!node.get("isFalse").asBoolean());
        
        assertTrue(node.get("isNull").isNull());
    }

    @Test
    public void testNestedStructures() throws Exception {
        String json = "{\"array\": [1, \"two\", {\"nested\": true}], \"object\": {\"a\": {\"b\": {\"c\": 3}}}}";
        JsonNode node = mapper.readTree(json);
        
        assertTrue(node.get("array").isArray());
        assertEquals(3, node.get("array").size());
        assertEquals(1, node.get("array").get(0).asInt());
        assertEquals("two", node.get("array").get(1).asText());
        assertTrue(node.get("array").get(2).get("nested").asBoolean());
        
        assertEquals(3, node.get("object").get("a").get("b").get("c").asInt());
    }
}
