package com.lucene_to_elastic.services;

import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DocumentTransferService {
    private final LuceneService luceneService;
    private final ElasticsearchService elasticsearchService;
    private final SynchronizationService synchronizationService;
    
    public DocumentTransferService(
            LuceneService luceneService, 
            ElasticsearchService elasticsearchService,
            SynchronizationService synchronizationService
    ) {
        this.luceneService = luceneService;
        this.elasticsearchService = elasticsearchService;
        this.synchronizationService = synchronizationService;
    }

    public void start() throws CorruptIndexException, IOException {
        luceneService.processDocuments(entries -> {
            try {
                elasticsearchService.bulk(entries);
                synchronizationService.saveLastSync(entries.getLast().getContent());
                String msg = String.format("%d entries are transferred.", entries.size());
                log.info(msg);
            } catch (ElasticsearchException | IOException e) {
                log.error(e.getMessage());
            }
        }, synchronizationService.loadLastSync());
    }
}
