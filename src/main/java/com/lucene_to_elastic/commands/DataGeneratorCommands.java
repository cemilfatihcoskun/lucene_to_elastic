package com.lucene_to_elastic.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.lucene_to_elastic.entities.Entry;
import com.lucene_to_elastic.services.LuceneService;

@ShellComponent
public class DataGeneratorCommands {
    private final LuceneService luceneService;
    
    @Autowired
    public DataGeneratorCommands(LuceneService luceneService) {
        this.luceneService = luceneService;
    }
    
    @ShellMethod
    public String luceneDataGenerate(int days, int dataPerDay) {
        List<Entry> entries = new ArrayList<Entry>();
        try {
            Random random = new Random();
            
            for (int i = 0; i < days; i++) {
                for (int j = 0; j < dataPerDay; j++) {
                    Entry entry = new Entry();
                    entry.setId(UUID.randomUUID().toString());
                    String content = "" + random.nextInt(100000, 1000000);
                    entry.setContent(content);
                    entries.add(entry);
                }
                
                luceneService.addDocuments(entries);
                System.out.println(String.format("Day %d data is generated.", i));
                entries.clear();
            }
            
            return "Datas generated and stored into Lucene.";
        } catch (IOException e) {
            e.printStackTrace();
            return "Datas could not be generated and stored into Lucene.";
        } finally {
            entries.clear();
        }
    }
}
