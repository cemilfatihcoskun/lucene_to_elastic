package lucene_to_elastic;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

public class LuceneDataAccess {
    private PostAdapter postAdapter;
    private Logger logger;
    
    public LuceneDataAccess() {
        postAdapter = new PostAdapterImpl();
        logger = Logger.getGlobal();
    }
    
    public LocalDateTime findFirstDatasDateTime() throws IOException {
        Directory directory = FSDirectory.open(new File(Config.LUCENE_DATABASE_PATH));
        IndexSearcher searcher = new IndexSearcher(directory);
        
        Sort sort = new Sort(new SortField(Config.TIMESTAMPS_NAME, SortField.STRING, false));

        TermRangeQuery query = new TermRangeQuery(
                Config.TIMESTAMPS_NAME,
                "19700101000000000",
                "21000101000000000",
                true,
                true
            );
        
        String oldestDate = "";
        TopDocs topDocs = searcher.search(query, null, 1, sort);
        if (topDocs.totalHits > 0) {
            Document doc = searcher.doc(topDocs.scoreDocs[0].doc);
            oldestDate = doc.get(Config.TIMESTAMPS_NAME);
        }
        
        searcher.getIndexReader().close();
        searcher.close();
        directory.close();
        
        if (oldestDate == "") {
            logger.warning("Lucene database is empty.");
            new Exception("Lucene database is empty.");
        }
        
        return DateTimeConverter.toLocalDateTime(oldestDate);
    }

    public void generateAndStoreRandomPosts(int days, int perDay) throws IOException {
        Directory directory = FSDirectory.open(new File(Config.LUCENE_DATABASE_PATH));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriter writer = new IndexWriter(directory, analyzer);
        
        for (int j = 0; j < days; j++) {
            for (int i = 0; i < perDay; i++) {
                int hours = Utils.randomInteger(0, 23);
                int minutes = Utils.randomInteger(0, 59);
                int seconds = Utils.randomInteger(0, 59);
                int milliseconds = Utils.randomInteger(0, 999);
                
                LocalDateTime dateTime = LocalDateTime.of(2025, 1, 1, hours, minutes, seconds, 0).plus(milliseconds, ChronoUnit.MILLIS).plusDays(j);
                
                Post post = Post.generateWithTime(dateTime);
                add(writer, post);
                System.out.println(String.format("%d. %s", j * perDay + i, post));
            }
        }
        
        logger.info(String.format("Random posts generated for %d days %d per day.", days, perDay));
        
        writer.commit();
        writer.close();
        analyzer.close();
        directory.close();
    }

    public void add(IndexWriter writer, Post post) throws IOException {
        Document doc = new Document();
        doc.add(new Field("id", post.getId(), Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field(Config.UNSTORED_FIELD_NAME, post.getContent(), Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field(Config.TIMESTAMPS_NAME, DateTimeConverter.toString(post.getDateTime()), Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);
        logger.info(String.format("%s is indexed.", post));
    }

    public void printAll()
            throws CorruptIndexException, LockObtainFailedException, IOException, InterruptedException {

        Directory directory = FSDirectory.open(new File(Config.LUCENE_DATABASE_PATH));
        IndexSearcher searcher = new IndexSearcher(directory);
        
        int numDocs = searcher.getIndexReader().maxDoc();
        for (int i = 0; i < numDocs; i++) {
            Document doc = searcher.doc(i);
           
            if (doc != null) {
                Post post = postAdapter.luceneToPost(doc);
                System.out.println(post);
            }
        }

        System.out.println(String.format("Total: %d.", numDocs));
        
        searcher.getIndexReader().close();
        searcher.close();
        directory.close();
    }
    
    public void removeAll() throws IOException {
        Directory directory = FSDirectory.open(new File(Config.LUCENE_DATABASE_PATH));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriter writer = new IndexWriter(directory, analyzer);
        
        writer.deleteAll();
        writer.commit();
        
        writer.close();
        analyzer.close();
        directory.close();
        
        logger.info(String.format("Index %s is deleted.", Config.TABLE_INDEX));
    }

    public void add(Post post) throws IOException {
        Directory directory = FSDirectory.open(new File(Config.LUCENE_DATABASE_PATH));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriter writer = new IndexWriter(directory, analyzer);
        
        add(writer, post);
        
        writer.close();
        analyzer.close();
        directory.close();
    }

    public void f() throws IOException, InterruptedException {
        //removeAll();
        //generateAndStoreRandomPosts(3, 100);
        //add(new Post().setContent("111111").setId("c81f111f-112c-4a0f-8c80-b550ce39a917").setDateTime(Utils.now()));
        //printAll();
        
        SyncFileManager.clean();
        
        Directory directory = FSDirectory.open(new File(Config.LUCENE_DATABASE_PATH));
        IndexReader reader = IndexReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        
        TermEnum terms = reader.terms();
        int count = 0;
        
        String lastProcessedTerm = SyncFileManager.loadLastSync();
        boolean startProcessing = lastProcessedTerm == "";
        String content = "";
        
        int i = 0;
        while (terms.next()) {
            Term term = terms.term();
            if (!term.field().equals(Config.UNSTORED_FIELD_NAME)) {
                continue;
            }
            
            content = term.text();
            
            if (!startProcessing && content.compareTo(lastProcessedTerm) <= 0) {
                continue;
            }

            
            Query query = new TermQuery(new Term(Config.UNSTORED_FIELD_NAME, content));
            
            TopDocs topDocs = searcher.search(query, null, Integer.MAX_VALUE);
            
            if (topDocs.totalHits > 0) {
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document document = searcher.doc(scoreDoc.doc);
                    System.out.println(document.get("id"));
                    System.out.println(content);
                    System.out.println(document.get(Config.TIMESTAMPS_NAME));
                    System.out.println();
                    
                    
                    i++;
                    if (i % 100 == 0) {
                        count++;
                        SyncFileManager.saveLastSync(content + " " + i);
                    }
                }
            }
            
            //System.out.println(count);
            
        }
        
        SyncFileManager.saveLastSync(content);
        
        System.out.println(count);
        
        searcher.close();
        reader.close();
        directory.close();
    }
}
