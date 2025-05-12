package lucene_to_elastic;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Main {
    // Java MAX INTEGER = 2147483647
    // Seconds in a day = 86400
    // 100 data in a second = 8640000 approx 10 million

    // Limitation: 10000 documents in a day

    // http://localhost:9200/posts/_search?q=dateTime:[2025-04-29T00:00:00.000%20TO%202025-04-29T23:59:59.999]&size=10000&sort=dateTime:asc

    public static void clean() throws IOException {
        SyncFileManager.clean();

        LuceneDataAccess lucene = new LuceneDataAccess();
        lucene.removeAll();

        ElasticsearchDataAccess elastic = new ElasticsearchDataAccess();
        elastic.removeAll();
        elastic.close();
    }

    public static void transferAndGenerateAfterDoneTransfer() throws InterruptedException {
        // This assumes transferring speed is greater than generating
        // Though it may be no-brainer

        // Only lucene data generator
        LuceneDataGeneratorThread luceneDataGeneratorThread = new LuceneDataGeneratorThread();
        luceneDataGeneratorThread.start();

        // We start the transferring
        Thread dataTransferThread = new Thread(new DataTransferThread());
        dataTransferThread.start();

        // We are waiting for previous days to be completed
        dataTransferThread.join();

        // Now we are today
        // Today's data is going to be generated
        // When it will become tomorrow
        // Only Lucene data generator is going to stop
        // After that we generate data for both elastic and Lucene
        // TODO: For testing purposes ZonedDate is being used
        luceneDataGeneratorThread.setExecutionDay(Utils.now().toLocalDate().plusDays(1));
        luceneDataGeneratorThread.join();

        // Now data is generated for the both of Lucene and Elasticsearch
        Thread luceneAndElasticsearchdataGeneratorThread = new Thread(new LuceneAndElasticsearchDataGenerator());
        luceneAndElasticsearchdataGeneratorThread.start();

        // We did not transferred yesterdays data to elastic so we are doing it
        dataTransferThread = new Thread(new DataTransferThread());
        dataTransferThread.start();

        // Stop after yesterday's job is done
        dataTransferThread.join();

        // It continue to generate so it does not stop
        // luceneAndElasticsearchdataGeneratorThread.join();
    }

    public static void initLuceneData(int days, int perDay) throws IOException {
        LuceneDataAccess lucene = new LuceneDataAccess();
        lucene.generateAndStoreRandomPosts(days, perDay);
    }

    public static void handleLogConfig() throws SecurityException, IOException {
        FileHandler fh = new FileHandler(String.format("log_%s.log", LocalDateTime.now()));
        fh.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                StringBuilder sb = new StringBuilder();
                sb.append(LocalDateTime.now());
                sb.append(" ");
                sb.append(record.getSourceClassName());
                sb.append(" ");
                sb.append(record.getSourceMethodName());
                sb.append("\n");
                sb.append(record.getLevel());
                sb.append(": ");
                sb.append(record.getMessage());
                sb.append("\n\n");
                return sb.toString();
            }
        });
        Logger.getGlobal().addHandler(fh);
    }

    public static void test() throws Exception {
        Main.clean();
        Main.initLuceneData(10, 100);
        Main.transferAndGenerateAfterDoneTransfer();
    }

    public static void test2() throws Exception {
        // Main.clean();
        // Main.initLuceneData(10, 100);
        // Main.print();

        LuceneDataAccess lucene = new LuceneDataAccess();
        lucene.f();
    }

    public static void print() throws Exception {
        ElasticsearchDataAccess elastic = new ElasticsearchDataAccess();
        LuceneDataAccess lucene = new LuceneDataAccess();

        lucene.printAll();
        // elastic.printAll();

        elastic.close();
    }

    public static void transferByContent() throws IOException, InterruptedException {
        ElasticsearchDataAccess elastic = new ElasticsearchDataAccess();
        
        // Only works for one unstored field
        // Currently only works up until the startup date
        Directory directory = FSDirectory.open(new File(Config.LUCENE_DATABASE_PATH));
        IndexReader reader = IndexReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        
        
        TermEnum terms = reader.terms();

        String lastProcessedTerm = SyncFileManager.loadLastSync();
        boolean startProcessing = lastProcessedTerm == "";
        String content = "";

        ArrayList<Post> posts = new ArrayList<Post>();

        int count = 0;
        while (terms.next()) {
            // Fetches all the values from the content field
            Term term = terms.term();
            if (!term.field().equals(Config.UNSTORED_FIELD_NAME)) {
                continue;
            }

            content = term.text();

            if (!startProcessing && content.compareTo(lastProcessedTerm) < 0) {
                continue;
            }

            Query query = new TermQuery(new Term(Config.UNSTORED_FIELD_NAME, content));

            TopDocs topDocs = searcher.search(query, null, Integer.MAX_VALUE);

            if (topDocs.totalHits <= 0) {
                continue;
            }

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document document = searcher.doc(scoreDoc.doc);
                String id = document.get("id");
                LocalDateTime datetime = DateTimeConverter.toLocalDateTime(document.get(Config.TIMESTAMPS_NAME));
                Post post = new Post().setId(id).setContent(content).setDateTime(datetime);
                
                posts.add(post);
                
                // If there are N elements bulk them
                // TODO: If there is x element in currently and count is
                // greater than N, and the system stops
                // then after your start system thoughts x elements are done so
                // that x elements are not indexed
                // So it indexes the last stored value again
                // I will change it later
                count++;
                if (count % Config.BULK_SIZE == 0) {
                    count = 0;
                    SyncFileManager.saveLastSync(content);
                    elastic.bulkIndexDocuments2(posts);
                    posts.clear();
                }
            }
        }
        
        // Bulk remaining elements
        if (!posts.isEmpty()) {
            SyncFileManager.saveLastSync(content);
            elastic.bulkIndexDocuments2(posts);
            posts.clear();
        }
        

        searcher.close();
        reader.close();
        directory.close();
        
        elastic.close();
    }

    public static void main(String[] args) throws Exception {
        Main.handleLogConfig();
        
        //clean();
        
        /*
        LuceneDataAccess lucene = new LuceneDataAccess();
        lucene.generateAndStoreRandomPosts(3, 10500);
        lucene.printAll();
        */
        
        
        Main.transferByContent();
        
        /*
        ElasticsearchDataAccess elastic = new ElasticsearchDataAccess();
        elastic.printAll();
        elastic.close();
        */
    }
}
