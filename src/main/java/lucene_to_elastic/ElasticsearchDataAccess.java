package lucene_to_elastic;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

public class ElasticsearchDataAccess {
    private ElasticsearchClient client;
    private final Logger logger = Logger.getGlobal();

    public ElasticsearchDataAccess() throws IOException {
        client = initializeElasticsearchClient();
        logger.info("Elasticsearch connection is established.");
        createTableIndexIfNotExists();
    }
    
    private ElasticsearchClient initializeElasticsearchClient() throws IOException {
        RestClient restClient = RestClient.builder(HttpHost.create(Config.ELASTIC_SERVER_URL))
                .setDefaultHeaders(new Header[] { new BasicHeader("Authorization", "ApiKey " + Config.ELASTIC_API_KEY) }).build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        JacksonJsonpMapper mapper = new JacksonJsonpMapper(objectMapper);

        ElasticsearchTransport transport = new RestClientTransport(restClient, mapper);

        return new ElasticsearchClient(transport);
    }
    
    private void createTableIndexIfNotExists() throws IOException {
        boolean exists = client.indices().exists(e -> e.index(Config.TABLE_INDEX)).value();
        if (!exists) {
            client.indices().create(c -> c
                    .index(Config.TABLE_INDEX)
                    .mappings(m -> m
                            .properties("dateTime", p -> p
                                .date(d -> d
                                    .format("yyyy-MM-dd'T'HH:mm:ss.SSS") // Supports full ISO-8601 like "2000-01-01T12:00:00.000Z"
                                )
                            )
                            .properties("title", p -> p.text(t -> t))
                            .properties("content", p -> p.text(t -> t))
                        )

            );
            logger.info(Config.TABLE_INDEX + " index did not exist. It created now.");
        }
    }
    
    public void indexDocument(Post post) throws ElasticsearchException, IOException {
        try {
            IndexResponse res = client.index(i -> i
                    .index(Config.TABLE_INDEX)
                    .id(post.getId())
                    .document(post)
            );
            System.out.println("Document indexed with ID: " + post.getId());
        } catch (ElasticsearchException e) {
            System.err.println("Failed to index document: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IO Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void elasticReadAll() throws ElasticsearchException, IOException {
        client.indices().refresh(r -> r.index(Config.TABLE_INDEX));
        SearchResponse<Post> res = client.search(s -> s
                .index(Config.TABLE_INDEX)
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
    
    public void removeAll() throws ElasticsearchException, IOException {
        boolean exists = client.indices().exists(e -> e.index(Config.TABLE_INDEX)).value();
        if (exists) {
            client.indices().delete(d -> d.index(Config.TABLE_INDEX));
            logger.info(Config.TABLE_INDEX + " index is deleted.");
        } else {
            logger.info(Config.TABLE_INDEX + " index does not exist.");
        }
    }
    
    public void close() throws IOException {
        client.close();
        logger.info("Elasticsearch connection is closed.");
    }

    public void bulkIndexDocuments(List<Post> posts) throws ElasticsearchException, IOException {
        BulkRequest.Builder br = new BulkRequest.Builder();
        
        for (Post post : posts) {
            br.operations(op -> op.index(idx -> idx
                    .index(Config.TABLE_INDEX)
                    .id(post.getId())
                    .document(post))
            );
        }
        
        BulkRequest req = br.build();
        BulkResponse res = client.bulk(req);
        
        if (res.errors()) {
            logger.warning("Elasticsearch could not bulked documents.");
            
            for (BulkResponseItem item : res.items()) {
                if (item.error() != null) {
                    logger.warning("Bulk error: " + item.error().reason());
                }
            }
        }
    }
}
