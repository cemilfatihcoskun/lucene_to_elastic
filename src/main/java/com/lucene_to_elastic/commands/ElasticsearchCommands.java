package com.lucene_to_elastic.commands;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.lucene_to_elastic.services.ElasticsearchService;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;

@ShellComponent
public class ElasticsearchCommands {
    private final ElasticsearchService elasticsearchService;
    
    @Autowired
    public ElasticsearchCommands(ElasticsearchService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }
    
    @ShellMethod("Prints all documents.")
    public void elasticsearchPrintAllDocuments() {
        System.out.println("Elasticsearch all documents: ");
        
        try {
            elasticsearchService.printAll();
        } catch (ElasticsearchException | IOException e) {
            e.printStackTrace();
        }
        
        System.out.println(String.format("Total: %d", elasticsearchService.count()));
    }
    
    @ShellMethod("Removes all documents.")
    public void elasticsearchRemoveAllDocuments() {
        try {
            elasticsearchService.deleteAll();
        } catch (ElasticsearchException | IOException e) {
            e.printStackTrace();
        }
    }
}
