package com.lucene_to_elastic.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.lucene_to_elastic.services.DocumentTransferService;

@ShellComponent
public class DataTransferCommands {
    private final DocumentTransferService documentTransferService;

    @Autowired
    public DataTransferCommands(DocumentTransferService documentTransferService) {
        this.documentTransferService = documentTransferService;
    }
    
    @ShellMethod("Transfer")
    public String luceneToElasticDataTransfer() {
        try {
            documentTransferService.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return "From Lucene to Elasticsearch data transfer is done succesfully.";
    }
}
