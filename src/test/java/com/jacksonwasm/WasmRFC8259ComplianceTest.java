package com.jacksonwasm;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

public class WasmRFC8259ComplianceTest {

    private ObjectMapper mapper = new ObjectMapper(new WasmJsonFactory());
    private File testDir = new File("src/test/resources/JSONTestSuite/test_parsing");

    @Test
    public void runComplianceSuite() throws Exception {
        if (!testDir.exists() || !testDir.isDirectory()) {
            System.out.println("JSONTestSuite not found at " + testDir.getAbsolutePath() + ". Skipping compliance test.");
            return;
        }

        int passedY = 0;
        int passedN = 0;
        int passedI = 0;
        
        try (Stream<Path> paths = Files.list(Paths.get(testDir.toURI()))) {
            for (Path p : (Iterable<Path>) paths::iterator) {
                File file = p.toFile();
                if (!file.getName().endsWith(".json")) {
                    continue;
                }

                String filename = file.getName();
                
                try {
                    // Try to parse the file
                    mapper.readTree(file);
                    
                    // If we reach here, parsing succeeded
                    if (filename.startsWith("n_")) {
                        // This should have failed!
                        System.err.println("FAILED (Should have rejected): " + filename);
                        fail("Expected failure for file: " + filename);
                    } else if (filename.startsWith("y_")) {
                        passedY++;
                    } else if (filename.startsWith("i_")) {
                        passedI++;
                    }
                } catch (Exception e) {
                    // Parsing failed
                    if (filename.startsWith("y_")) {
                        // This should have succeeded!
                        System.err.println("FAILED (Should have accepted): " + filename + " - " + e.getMessage());
                        fail("Expected success for file: " + filename + ", but got: " + e.getMessage());
                    } else if (filename.startsWith("n_")) {
                        // Correctly rejected
                        // Ensure it's a Jackson exception or IOException, not a raw Wasm panic
                        if (!(e instanceof JsonParseException || e.getMessage().contains("Failed to parse JSON via Wasm"))) {
                             System.err.println("FAILED (Wrong exception type): " + filename + " - " + e.getClass().getName());
                             fail("Expected JsonParseException for file: " + filename + " but got " + e.getClass().getName());
                        }
                        passedN++;
                    } else if (filename.startsWith("i_")) {
                        passedI++;
                    }
                }
            }
        }
        
        System.out.println("Compliance Test Results:");
        System.out.println("  Passed Valid (y_): " + passedY);
        System.out.println("  Passed Invalid (n_): " + passedN);
        System.out.println("  Implementation Defined (i_): " + passedI);
    }
}
