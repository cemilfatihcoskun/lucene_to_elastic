package com.lucene_to_elastic.services;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SynchronizationService {
    @Value("${synchronizationFile}")
    private String synchronizationFile;

    public void saveLastSync(String content) throws IOException {
        if (content == null || content.isBlank()) {
            log.warn("Attempting to save blank content.");
        }
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(synchronizationFile))) {
            writer.write(content);
            log.info(String.format("Last processed content has been saved.", content, synchronizationFile));
        }
    }

    public String loadLastSync() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(synchronizationFile))) {
            return reader.readLine();
        } catch (FileNotFoundException e) {
            return "";
        }
    }
    
    public void clean() throws IOException {
        Path path = Paths.get(synchronizationFile);
        Files.deleteIfExists(path);
    }
}
