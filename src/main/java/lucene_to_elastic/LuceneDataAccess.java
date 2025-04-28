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
    private Logger logger;
    
    public LuceneDataAccess() {
        postAdapter = new PostAdapterImpl();
        logger = Logger.getGlobal();
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
                
                LocalDateTime dateTime = LocalDateTime.of(2020, 1, 1, hours, minutes, seconds, 0).plus(milliseconds, ChronoUnit.MILLIS).plusDays(j);
                
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
        doc.add(new Field("content", post.getContent(), Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("datetime", DateTimeConverter.toString(post.getDateTime()), Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);
        logger.info(String.format("%s is indexed", post));
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
}
