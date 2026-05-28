package com.jacksonwasm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WasmBigValuesTest {

    private final ObjectMapper mapper = new ObjectMapper(new WasmJsonFactory());

    @Test
    public void testBigInteger() throws Exception {
        String json = "{\"big\": 123456789012345678901234567890}";
        JsonNode node = mapper.readTree(json);
        BigInteger expected = new BigInteger("123456789012345678901234567890");
        // We use asText() because the Wasm bridge might serialize large numbers as strings
        // for maximum precision preservation.
        assertEquals(expected, new BigInteger(node.get("big").asText()));
    }

    @Test
    public void testBigDecimal() throws Exception {
        String json = "{\"decimal\": 12345.6789012345678901234567890}";
        JsonNode node = mapper.readTree(json);
        BigDecimal expected = new BigDecimal("12345.6789012345678901234567890");
        assertEquals(expected, new BigDecimal(node.get("decimal").asText()));
    }

    @Test
    public void testScientificNotation() throws Exception {
        String json = "{\"sci\": 1.23456789e+30}";
        JsonNode node = mapper.readTree(json);
        BigDecimal expected = new BigDecimal("1.23456789e+30");
        assertEquals(0, expected.compareTo(new BigDecimal(node.get("sci").asText())));
    }
}
