package lucene_to_elastic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
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
import org.elasticsearch.client.RestClient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

//sudo chown -Rv 1000:0 elasticsearch_database

public class Main {
    private static final String LUCENE_DATABASE_PATH = "lucene_database";
    private static final String ELASTIC_SERVER_URL = "http://localhost:9200";
    private static final String ELASTIC_API_KEY = "NEJWYVBwWUI0QmN0bjZvaEFEYVM6NVlVUGQ4TW1STFdqdkhZTWR6c180Zw==";
    
    // This is lucene's format
    private static final String BEGIN_DATETIME = "19900101000000000";
    private static final String END_DATETIME = "20250416200000000";
    
    // The date to be processed
    private static String PROCESSED_DATETIME = "";
    
    private static final String TABLE_INDEX = "posts";
    
    /*
     * 1. Read the last day from a file if not exists create from lucene
     * 2. Fetch all the data from that day
     * 3. Insert those into the elasticsearch
     * 4. Save that day into a file
     * 5. Log that day
     */
    
    // Java MAX INTEGER = 2147483647
    // Seconds in a day = 86400
    // 100 data in a second = 8640000 approx 10 million

    public static void main(String[] args) throws Exception {
        // Elastic init
        //ElasticsearchClient client = initializeElasticsearchClient();
        //createTableIndexIfNotExists(client);
        
        //generateAndStorePostsInLucene();
        
        
        //luceneReadAll(searcher);

        LocalDateTime processingDateTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0).plus(999, ChronoUnit.MILLIS);
        
        //luceneSearchTimeBetween(beginDateTime, endDateTime);
 
        String lucenedt = Utils.localDateTimeToLuceneFormat(processingDateTime);
        LocalDateTime ldt = Utils.luceneFormatToLocalDateTime(lucenedt);
        System.out.println(ldt.equals(processingDateTime));
        
        String dt = loadLastDayDateTime();
        System.out.println(dt);
        
        saveLastDayDateTime(dt);
        
        // Elastic deinit
        //client.close();
    }
    
    public static String loadLastDayDateTime() throws IOException {
        File file = new File("lastdaydatetime.txt");
        
        if (!file.exists()) {
            // TODO: log
            System.out.println("lastdaydatetime.txt has been created.");
            return findFirstDatasDateTimeInLucene();
        }
        
        FileReader fileReader = new FileReader(file);
        try (BufferedReader reader = new BufferedReader(fileReader)) {
            return reader.readLine().trim();   
        }
    }
    
    public static void saveLastDayDateTime(String datetime) throws IOException {
        File file = new File("lastdaydatetime.txt");
        
        if (!file.exists()) {
            // TODO: log that file is created
        }
        
        FileWriter fileWriter = new FileWriter(file);
        try (BufferedWriter writer = new BufferedWriter(fileWriter)) {
            writer.write(datetime);
            writer.flush();
        }
        
        // TODO: log that file is written
    }
    
    
    
    private static String findFirstDatasDateTimeInLucene() throws IOException {
        Directory directory = FSDirectory.open(new File(LUCENE_DATABASE_PATH));
        IndexSearcher searcher = new IndexSearcher(directory);
        
        Sort sort = new Sort(new SortField("dateField", SortField.STRING, true));

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
        } else {
            searcher.getIndexReader().close();
            searcher.close();
            directory.close();
            new Exception("Lucene database is empty.");
        }
        
        searcher.getIndexReader().close();
        searcher.close();
        directory.close();
        
        return oldestDate;
    }

    public static void generateAndStorePostsInLucene() throws IOException {
        Directory directory = FSDirectory.open(new File(LUCENE_DATABASE_PATH));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriter writer = new IndexWriter(directory, analyzer);
        
        for (int j = 0; j < 5; j++) {
            for (int i = 0; i < 10_000; i++) {
                int hours = Utils.randomInteger(0, 23);
                int minutes = Utils.randomInteger(0, 59);
                int seconds = Utils.randomInteger(0, 59);
                int milliseconds = Utils.randomInteger(0, 999999999);
                
                LocalDateTime dateTime = LocalDateTime.of(2000, 1, 1 + j, hours, minutes, seconds, milliseconds);
                
                Post post = Post.generateWithTime(dateTime);
                luceneWrite(writer, post);
                System.out.println(String.format("%d. %s - %s.", j * 10_000 + i, post.getContent(), Utils.localDateTimeToLuceneFormat(dateTime)));
            }
        }
        
        writer.commit();
        writer.close();
        analyzer.close();
        directory.close();
    }

    private static ElasticsearchClient initializeElasticsearchClient() throws IOException {
        RestClient restClient = RestClient.builder(HttpHost.create(ELASTIC_SERVER_URL))
                .setDefaultHeaders(new Header[] { new BasicHeader("Authorization", "ApiKey " + ELASTIC_API_KEY) }).build();

        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        return new ElasticsearchClient(transport);
    }
    
    private static void createTableIndexIfNotExists(ElasticsearchClient client) throws IOException {
        boolean exists = client.indices().exists(e -> e.index(TABLE_INDEX)).value();
        if (!exists) {
            client.indices().create(c -> c
                    .index(TABLE_INDEX)
            );
        }
    }
    
    private static Post luceneDocumentToPost(Document doc) {
        return new Post()
                .setContent(doc.get("content"))
                .setDateTime(doc.get("datetime"))
                .setId(doc.get("id"));
    }

    private static void transferLuceneToElasticsearch(IndexSearcher searcher, ElasticsearchClient client) throws IOException {
        TermRangeQuery query = new TermRangeQuery(
                "datetime",
                BEGIN_DATETIME,
                END_DATETIME,
                true,
                true
        );
        
        TopDocs results = searcher.search(query, Integer.MAX_VALUE);
        
        for (int i = 0; i < results.totalHits; i++) {
            Document doc = searcher.doc(results.scoreDocs[i].doc);
            Post post = luceneDocumentToPost(doc);

            elasticWrite(client, post);
            System.out.println("Transferred the document: " + post);
        }
    }
    
    public static void elasticWrite(ElasticsearchClient client, Post post) throws ElasticsearchException, IOException {
        IndexResponse res = client.index(i -> i
                .index(TABLE_INDEX)
                .id(post.getId())
                .document(post)
        );
    }

    public static void luceneWrite(IndexWriter writer, Post post) throws IOException {
        Document doc = new Document();
        doc.add(new Field("id", post.getId(), Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("content", post.getContent(), Field.Store.YES, Field.Index.ANALYZED));
        doc.add(new Field("datetime", post.getDateTime(), Field.Store.YES, Field.Index.ANALYZED));
        writer.addDocument(doc);
    }

    public static void luceneReadAll()
            throws CorruptIndexException, LockObtainFailedException, IOException, InterruptedException {

        Directory directory = FSDirectory.open(new File(LUCENE_DATABASE_PATH));
        IndexSearcher searcher = new IndexSearcher(directory);
        
        int numDocs = searcher.getIndexReader().maxDoc();
        for (int i = 0; i < numDocs; i++) {
            Document doc = searcher.doc(i);
            if (doc != null) {
                System.out.println(String.format("%s - %s", doc.get("content"), doc.get("datetime")));
            }
        }

        System.out.println(String.format("Total: %d", numDocs));
        
        searcher.getIndexReader().close();
        searcher.close();
        directory.close();
    }
    
    private static void luceneSearchTimeBetween(LocalDateTime beginDateTime, LocalDateTime endDateTime) throws IOException {
        Directory directory = FSDirectory.open(new File(LUCENE_DATABASE_PATH));
        IndexSearcher searcher = new IndexSearcher(directory);
        
        String beginDateTimeStr = Utils.localDateTimeToLuceneFormat(beginDateTime);
        String endDateTimeStr = Utils.localDateTimeToLuceneFormat(endDateTime);
        
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
            Post post = luceneDocumentToPost(doc);
            
            System.out.println("Transferred the document: " + post);
        }
        
        searcher.getIndexReader().close();
        searcher.close();
        directory.close();
    }

    public static void elasticWrite(ElasticsearchClient client) throws ElasticsearchException, IOException {
        String uuid = UUID.randomUUID().toString();
        Post post = Post.generate();

        try {
            IndexResponse res = client.index(i -> i
                    .index(TABLE_INDEX)
                    .id(uuid)
                    .document(post));
            System.out.println("Document indexed with ID: " + uuid);
        } catch (ElasticsearchException e) {
            System.err.println("Failed to index document: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void elasticReadAll(ElasticsearchClient client) throws ElasticsearchException, IOException {
        client.indices().refresh(r -> r.index(TABLE_INDEX));
        SearchResponse<Post> res = client.search(s -> s
                .index(TABLE_INDEX)
                .query(q -> q.matchAll(t -> t))
                .from(0)
                .size(Integer.MAX_VALUE)
        , Post.class);

        List<Hit<Post>> hits = res.hits().hits();
        
        for (int i = 0; i < hits.size(); i++) {
            Post pos = hits.get(i).source();
            System.out.println(String.format("%s - %s", pos.getDateTime(), pos.getContent()));
        }

        System.out.println("Total Size: " + hits.size());
    }
    
    public static void elasticRemoveAll(ElasticsearchClient client) throws ElasticsearchException, IOException {
        client.deleteByQuery(new DeleteByQueryRequest.Builder()
                .index(TABLE_INDEX)
                .query(
                    new Query.Builder()
                        .matchAll(QueryBuilders
                            .matchAll()
                            .build())
                        .build())
                .build());
    }
    
    public static void luceneRemoveAll() throws IOException {
        Directory directory = FSDirectory.open(new File(LUCENE_DATABASE_PATH));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriter writer = new IndexWriter(directory, analyzer);
        
        writer.deleteAll();
        writer.commit();
        
        writer.close();
        analyzer.close();
        directory.close();
    }
    
}
