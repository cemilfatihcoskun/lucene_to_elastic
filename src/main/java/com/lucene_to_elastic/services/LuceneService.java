package com.lucene_to_elastic.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.jline.utils.Log;
import org.springframework.stereotype.Service;

import com.lucene_to_elastic.EntryLuceneDocumentAdapter;
import com.lucene_to_elastic.entities.Entry;

@Service
public class LuceneService {
    private final Directory directory;
    private final Analyzer analyzer;
    
    public LuceneService(Directory directory, Analyzer analyzer) {
        this.directory = directory;
        this.analyzer = analyzer;
    }
    
    public void addDocument(Entry entry) throws IOException {
        IndexWriter writer = new IndexWriter(directory, analyzer);
        
        Document document = EntryLuceneDocumentAdapter.toDocument(entry);
        writer.addDocument(document);
        writer.commit();
        
        writer.close();
        
        Log.info(String.format("Document with id=%s is added.", entry.getId()));
    }
    
    public void printAllDocuments() throws CorruptIndexException, IOException {
        IndexReader reader = IndexReader.open(directory);
        
        for (int i = 0; i < reader.maxDoc(); i++) {
            if (reader.isDeleted(i)) {
                continue;
            }
            
            Document document = reader.document(i);
            Entry entry = EntryLuceneDocumentAdapter.toEntry(document);
            
            System.out.println(entry);
        }
        
        System.out.println(String.format("Total: %d", reader.maxDoc()));
        
        reader.close();
    }
    
    public void removeDocumentById(String id) throws IOException {
        IndexWriter writer = new IndexWriter(directory, analyzer);
        writer.deleteDocuments(new Term("id", id));
        writer.close();
    }
    
    public void removeAllDocuments() throws IOException {
        IndexWriter writer = new IndexWriter(directory, analyzer);
        writer.deleteAll();
        writer.close();
        
        Log.info("All documents are removed.");
    }
    
    public List<Entry> search(String field, String text) throws IOException {
        List<Entry> entries = new ArrayList<>();
        IndexReader reader = IndexReader.open(directory);
        
        IndexSearcher searcher = new IndexSearcher(reader);
        Query query = new TermQuery(new Term(field, text));
        TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document document = searcher.doc(scoreDoc.doc);
            Entry entry = EntryLuceneDocumentAdapter.toEntry(document);
            entries.add(entry);
        }
        
        searcher.close();
        reader.close();
        return entries;
    }

    private final int BULK_COUNT = 10000;
    
    public void processDocuments(Consumer<List<Entry>> consumer, String lastProcessedTerm) throws CorruptIndexException, IOException {
        IndexReader reader = IndexReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        
        List<Entry> entriesBuffer = new ArrayList<>();
        
        TermEnum terms = reader.terms();
        String content = "";

        while (terms.next()) {
            Term term = terms.term();
            if (!term.field().equals("content")) {
                continue;
            }
            
            content = term.text();
            
            if (content.compareTo(lastProcessedTerm) <= 0) {
                continue;
            }
            
            List<Entry> foundEntries = search("content", content);
            entriesBuffer.addAll(foundEntries);
            foundEntries.clear();
            
            if (entriesBuffer.size() >= BULK_COUNT) {
                consumer.accept(entriesBuffer);
                entriesBuffer.clear();
            }
        }
        
        if (!entriesBuffer.isEmpty()) {
            consumer.accept(entriesBuffer);
            entriesBuffer.clear();
        }

        searcher.close();
        reader.close();
    }


    private int documentCount() throws CorruptIndexException, LockObtainFailedException, IOException {
        IndexWriter writer = new IndexWriter(directory, analyzer);
        int count = writer.docCount();
        writer.close();
        return count;
    }

    public void addDocuments(List<Entry> entries) throws CorruptIndexException, LockObtainFailedException, IOException {
        IndexWriter writer = new IndexWriter(directory, analyzer);
        
        for (Entry entry : entries) {
            Document document = EntryLuceneDocumentAdapter.toDocument(entry);
            writer.addDocument(document);
        }
        
        writer.close();
    }
}
