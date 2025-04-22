package lucene_to_elastic;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

public class LuceneDataAccess {
    private PostAdapter postAdapter;
    
    public LuceneDataAccess() {
        postAdapter = new PostAdapterImpl();
    }
    
    public LocalDateTime findFirstDatasDateTime() throws IOException {
        Directory directory = FSDirectory.open(new File(Config.LUCENE_DATABASE_PATH));
        IndexSearcher searcher = new IndexSearcher(directory);
        
        Sort sort = new Sort(new SortField("datetime", SortField.STRING, false));

        TermRangeQuery query = new TermRangeQuery(
                "datetime",
                "19700101000000000",
                "21000101000000000",
                true,
                true
            );
        
        String oldestDate = "";
        TopDocs topDocs = searcher.search(query, null, 1, sort);
        if (topDocs.totalHits > 0) {
            Document doc = searcher.doc(topDocs.scoreDocs[0].doc);
            oldestDate = doc.get("datetime");
        }
        
        searcher.getIndexReader().close();
        searcher.close();
        directory.close();
        
        if (oldestDate == "") {
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
                int milliseconds = Utils.randomInteger(0, 999999999);
                
                LocalDateTime dateTime = LocalDateTime.of(2000, 1, 1, hours, minutes, seconds, 0).plus(999, ChronoUnit.MILLIS).plusDays(j);
                
                Post post = Post.generateWithTime(dateTime);
                add(writer, post);
                System.out.println(String.format("%d. %s", j * perDay + i, post));
            }
        }
        
        writer.commit();
        writer.close();
        analyzer.close();
        directory.close();
    }

    public void add(IndexWriter writer, Post post) throws IOException {
        Document doc = new Document();
        doc.add(new Field("id", post.getId(), Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("content", post.getContent(), Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("datetime", DateTimeConverter.toString(post.getDateTime()), Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);
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

        System.out.println(String.format("Total: %d", numDocs));
        
        searcher.getIndexReader().close();
        searcher.close();
        directory.close();
    }
    
    private void searchTimeBetween(LocalDateTime beginDateTime, LocalDateTime endDateTime) throws IOException {
        Directory directory = FSDirectory.open(new File(Config.LUCENE_DATABASE_PATH));
        IndexSearcher searcher = new IndexSearcher(directory);
        
        String beginDateTimeStr = DateTimeConverter.toString(beginDateTime);
        String endDateTimeStr = DateTimeConverter.toString(endDateTime);
        
        TermRangeQuery query = new TermRangeQuery(
                "datetime",
                beginDateTimeStr,
                endDateTimeStr,
                true,
                true
        );
        
        TopDocs results = searcher.search(query, Integer.MAX_VALUE);
        
        for (int i = 0; i < results.totalHits; i++) {
            Document doc = searcher.doc(results.scoreDocs[i].doc);
            Post post = postAdapter.luceneToPost(doc);
            
            System.out.println("Transferred the document: " + post);
        }
        
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
    }

    public Post[] searchForDay(LocalDateTime datetime) throws IOException {
        Directory directory = FSDirectory.open(new File(Config.LUCENE_DATABASE_PATH));
        IndexSearcher searcher = new IndexSearcher(directory);
        
        datetime = datetime.withHour(0).withMinute(0).withSecond(0).withNano(0);
        System.out.println(datetime);
        String startDateTime = DateTimeConverter.toString(datetime);
        String endDateTime = DateTimeConverter.toString(datetime.plusDays(1));

        TermRangeQuery query = new TermRangeQuery("datetime", startDateTime, endDateTime, true, false);
        TopDocs results = searcher.search(query, Integer.MAX_VALUE);

        Post[] posts = new Post[results.totalHits];

        for (int i = 0; i < results.totalHits; i++) {
            posts[i] = postAdapter.luceneToPost(searcher.doc(results.scoreDocs[i].doc));
        }

        searcher.getIndexReader().close();
        directory.close();

        return posts;
    }
}
