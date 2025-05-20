package com.lucene_to_elastic;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.lucene_to_elastic.entities.Entry;

public class EntryLuceneDocumentAdapter {
    public static Document toDocument(Entry entry) {
        Document document = new Document();
        document.add(new Field("id", entry.getId(), Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field("content", entry.getContent(), Field.Store.YES, Field.Index.ANALYZED));
        return document;
    }
    
    public static Entry toEntry(Document document) {
        Entry entry = new Entry();
        entry.setId(document.get("id"));
        entry.setContent(document.get("content"));
        return entry;
    }
}
