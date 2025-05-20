package com.lucene_to_elastic.services;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lucene_to_elastic.entities.Entry;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ElasticsearchService {
    private final ElasticsearchClient client;
    
    // TODO: Not working right now
    @Value("${elasticsearch.indexName}")
    private String indexName = "posts";
    
    private final int BULK_SIZE = 10000;

    @Autowired
    public ElasticsearchService(ElasticsearchClient client) throws IOException {
        this.client = client;
        createTableIndexIfNotExists();
    }
    
    
    private void createTableIndexIfNotExists() throws IOException {
        boolean exists = client.indices().exists(e -> e.index(indexName)).value();
        if (exists) {
            return;
        }
        
        client.indices().create(c -> c
                .index(indexName)
                .mappings(m -> m
                        .properties("title", p -> p.text(t -> t))
                        .properties("content", p -> p.text(t -> t)))
        );
        
        log.debug(String.format("Index %s did not exist. It created now.", indexName));
    }

    public void printAll() throws ElasticsearchException, IOException {
        client.indices().refresh(r -> r.index(indexName));
        
        int total = 0;

        log.info(String.format("Total = %d documents.", total));
    }
    
    public int count()  {
        return -1;
    }

    public void deleteAll() throws ElasticsearchException, IOException {
        boolean exists = client.indices().exists(e -> e.index(indexName)).value();
        if (exists) {
            client.indices().delete(d -> d.index(indexName));
            log.info("All documents are deleted.");
        } else {
            log.warn("There is no document to delete.");
        }
    }

    public void bulk(List<Entry> entries) throws ElasticsearchException, IOException {
        int totalEntries = entries.size();
        
        for (int processedEntries = 0; processedEntries < totalEntries; processedEntries += BULK_SIZE) {
            int end = Math.min(processedEntries + BULK_SIZE, totalEntries);
            List<Entry> batch = entries.subList(processedEntries, end);

            BulkRequest.Builder br = new BulkRequest.Builder();

            for (Entry entry : batch) {
                br.operations(op -> op.index(idx -> idx.index(indexName)
                        .id(entry.getId())
                        .document(entry)));
            }

            BulkRequest req = br.build();
            BulkResponse res = client.bulk(req);

            if (res.errors()) {
                for (BulkResponseItem item : res.items()) {
                    if (item.error() != null) {
                        log.error(String.format("Bulk error: %s", item.error().reason()));
                    }
                }
            }
        }

        log.info(String.format("Total %d documents processed.", totalEntries));
    }

}
