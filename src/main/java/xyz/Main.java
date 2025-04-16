package xyz;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
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
    
    private static final String TABLE_INDEX = "posts";
    
    /*
     * 1. Generate data
     * 2. Insert data into lucene
     * 3. Insert data into elastic
     * 4. Transfer lucene data to elastic
     */

    public static void main(String[] args) throws Exception {
        // Lucene init
        Directory directory = FSDirectory.open(new File(LUCENE_DATABASE_PATH));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriter writer = new IndexWriter(directory, analyzer);
        IndexSearcher searcher = new IndexSearcher(directory);
        
        // Elastic init
        ElasticsearchClient client = initializeElasticsearchClient();
        createTableIndexIfNotExists(client);
        
        // Doing stuff
        //luceneRemoveAll(writer);
        //generalDo(writer, searcher, client);
        //generateAndStorePostsInLucene(writer);
        
        //luceneReadAll(searcher);
        elasticReadAll(client);
        //luceneRemoveAll(writer);
        //transferLuceneToElasticsearch(searcher, client);
        
       
        

        // Lucene deinit
        writer.close();
        searcher.getIndexReader().close();
        searcher.close();
        analyzer.close();
        directory.close();
        
        // Elastic deinit
        client.close();
    }
    
    public static void generalDo(IndexWriter writer, IndexSearcher searcher, ElasticsearchClient client) throws IOException, InterruptedException {
        // Thread
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

        // Data generation and inserting both lucene and elastic 
        executor.scheduleAtFixedRate(() -> {
            Post post = Post.generate();
            System.out.println(String.format("%s - %d is generated.", post.getDateTime(), post.getContent()));
            
            synchronized (writer) {
                try {
                    luceneWrite(writer, post);
                    elasticWrite(client, post);
                } catch (IOException | ElasticsearchException e) {
                    e.printStackTrace();
                }
            }
            
        }, 0, 5, TimeUnit.SECONDS);

        // Transfering data from lucene to elastic
        executor.scheduleAtFixedRate(() -> {
            try {
                transferLuceneToElasticsearch(searcher, client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 2, TimeUnit.SECONDS);
        
        Thread.sleep(30 * 1000);
        
        writer.close();
        searcher.close();
        client.close();

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
    
    public static void generateAndStorePostsInLucene(IndexWriter writer) throws IOException {
        for (int i = 0; i < 10_000; i++) {
            luceneWrite(writer, Post.generate());
            System.out.println(i + " Written");
        }
        writer.commit();
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
            
            Post post = new Post()
                    .setContent(doc.get("content"))
                    .setDateTime(doc.get("datetime"))
                    .setId(doc.get("id"));

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

    public static void luceneReadAll(IndexSearcher searcher)
            throws CorruptIndexException, LockObtainFailedException, IOException, InterruptedException {

        int numDocs = searcher.getIndexReader().maxDoc();
        for (int i = 0; i < numDocs; i++) {
            Document doc = searcher.doc(i);
            if (doc != null) {
                System.out.println(String.format("%s - %s", doc.get("content"), doc.get("datetime")));
            }
        }

        System.out.println(String.format("Total: %d", numDocs));
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
        client.indices().refresh(r -> r.index("posts"));
        SearchResponse<Post> res = client.search(s -> s
                .index(TABLE_INDEX)
                .query(q -> q.matchAll(t -> t))
                .from(0)
                .size(100)
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
    
    public static void luceneRemoveAll(IndexWriter writer) throws IOException {
        writer.deleteAll();
        writer.commit();
    }
    
}
