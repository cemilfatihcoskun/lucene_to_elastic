package com.lucene_to_elastic.configuration;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LuceneConfiguration {
    @Bean
    public Directory getDirectory(@Value("${lucene.index.path}") String path) throws IOException {
        return FSDirectory.open(new File(path));
    }
    
    @Bean
    public Analyzer getAnalyzer() {
        return new StandardAnalyzer();
    }
}
