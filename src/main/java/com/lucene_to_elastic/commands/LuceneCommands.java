package com.lucene_to_elastic.commands;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.lucene_to_elastic.entities.Entry;
import com.lucene_to_elastic.services.LuceneService;

@ShellComponent
public class LuceneCommands {
    private final LuceneService luceneService;

    @Autowired
    public LuceneCommands(LuceneService luceneService) {
        this.luceneService = luceneService;
    }

    @ShellMethod("Add a document with given content. Usage: addDocument --content <content>")
    public String luceneAddDocument(String content) {
        try {
            String id = UUID.randomUUID().toString();
            Entry entry = new Entry();
            entry.setId(id);
            entry.setContent(content);
            luceneService.addDocument(entry);
            return String.format("Document %s is added.", id);
        } catch (IOException exception) {
            return "Document could not be added.";
        }
    }
    
    @ShellMethod("Removes a document with given id. Usage: removeDocumentById --id <id>")
    public String luceneRemoveDocumentById(String id) {
        try {
            luceneService.removeDocumentById(id);
            return String.format("Document with id %s is removed.", id);
        } catch (IOException exception) {
            return "Document could not be removed.";
        }
    }
    
    @ShellMethod("Removes all documents.")
    public void luceneRemoveAllDocuments() {
        try {
            luceneService.removeAllDocuments();
        } catch (Exception exception) {
            LoggerFactory.getLogger(LuceneCommands.class).info("Documents could not be removed.");
        }
    }
    
    @ShellMethod("Prints all documents.")
    public void lucenePrintAllDocuments() {
        try {
            luceneService.printAllDocuments();
        } catch (Exception exception) {
            LoggerFactory.getLogger(LuceneCommands.class).info("Documents could not be printed.");
        }
    }
    
    @ShellMethod("Search documents by field and text. Usage: search --field <field> --text <text>")
    public String luceneSearch(String field, String text) {
        try {
            List<Entry> results = luceneService.search(field, text);
            if (results.isEmpty()) {
                return "No matching entries found.";
            }
            StringBuilder sb = new StringBuilder(String.format("Found entries (%d):\n", results.size()));
            for (Entry entry : results) {
                sb.append(entry).append("\n");
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        } catch (IOException e) {
            return "Error searching entries: " + e.getMessage();
        }
    }
}
