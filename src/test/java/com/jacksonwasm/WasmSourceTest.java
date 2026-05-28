package com.jacksonwasm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WasmSourceTest {

    private final ObjectMapper mapper = new ObjectMapper(new WasmJsonFactory());
    private final String json = "{\"source\": \"test\", \"active\": true}";

    @Test
    public void testInputStreamSource() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonNode node = mapper.readTree(in);
        assertEquals("test", node.get("source").asText());
        assertEquals(true, node.get("active").asBoolean());
    }

    @Test
    public void testReaderSource() throws Exception {
        StringReader reader = new StringReader(json);
        JsonNode node = mapper.readTree(reader);
        assertEquals("test", node.get("source").asText());
    }

    @Test
    public void testByteArraySource() throws Exception {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        JsonNode node = mapper.readTree(bytes);
        assertEquals("test", node.get("source").asText());
    }

    @Test
    public void testStringSource() throws Exception {
        JsonNode node = mapper.readTree(json);
        assertEquals("test", node.get("source").asText());
    }
    
    @Test
    public void testCharArraySource() throws Exception {
        char[] chars = json.toCharArray();
        // Jackson ObjectMapper doesn't have readTree(char[]) directly, 
        // but we can use factory.createParser(chars)
        try (var parser = mapper.getFactory().createParser(chars, 0, chars.length)) {
            JsonNode node = mapper.readTree(parser);
            assertEquals("test", node.get("source").asText());
        }
    }

    @Test
    public void testFileSource() throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("wasm-test", ".json");
        tempFile.deleteOnExit();
        java.nio.file.Files.writeString(tempFile.toPath(), json);
        
        JsonNode node = mapper.readTree(tempFile);
        assertEquals("test", node.get("source").asText());
    }

    @Test
    public void testUrlSource() throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("wasm-test-url", ".json");
        tempFile.deleteOnExit();
        java.nio.file.Files.writeString(tempFile.toPath(), json);
        
        java.net.URL url = tempFile.toURI().toURL();
        JsonNode node = mapper.readTree(url);
        assertEquals("test", node.get("source").asText());
    }

    @Test
    public void testCharArrayWithoutOffsetSource() throws Exception {
        char[] chars = json.toCharArray();
        try (var parser = mapper.getFactory().createParser(chars)) {
            JsonNode node = mapper.readTree(parser);
            assertEquals("test", node.get("source").asText());
        }
    }

    @Test
    public void testDataInputSource() throws Exception {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        java.io.DataInputStream dis = new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes));
        try (var parser = mapper.getFactory().createParser((java.io.DataInput) dis)) {
            JsonNode node = mapper.readTree(parser);
            assertEquals("test", node.get("source").asText());
        }
    }
}

