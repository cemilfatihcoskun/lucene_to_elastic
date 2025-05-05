package lucene_to_elastic;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
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
        RestClient restClient = RestClient.builder(HttpHost.create(Config.ELASTIC_SERVER_URL)).setDefaultHeaders(
                new Header[] { new BasicHeader("Authorization", "ApiKey " + Config.ELASTIC_API_KEY) }).build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        JacksonJsonpMapper mapper = new JacksonJsonpMapper(objectMapper);

        ElasticsearchTransport transport = new RestClientTransport(restClient, mapper);

        return new ElasticsearchClient(transport);
    }

    private void createTableIndexIfNotExists() throws IOException {
        boolean exists = client.indices().exists(e -> e.index(Config.TABLE_INDEX)).value();
        if (!exists) {
            client.indices().create(c -> c.index(Config.TABLE_INDEX)
                    .mappings(m -> m.properties("dateTime", p -> p.date(d -> d.format("yyyy-MM-dd'T'HH:mm:ss.SSS") // Supports
                                                                                                                   // "2000-01-01T12:00:00.000Z"
                    )).properties("title", p -> p.text(t -> t)).properties("content", p -> p.text(t -> t)))

            );
            logger.info(String.format("Index %s did not exist. It created now.", Config.TABLE_INDEX));
        } else {
            // TODO: Not sure if this is needed
            // logger.info(String.format("Index %s already exist.", Config.TABLE_INDEX));
        }
    }

    public void indexDocument(Post post) throws ElasticsearchException, IOException {
        try {
            client.index(i -> i.index(Config.TABLE_INDEX).id(post.getId()).document(post));
            logger.info(String.format("%s is indexed with id %s.", post, post.getId()));
        } catch (ElasticsearchException e) {
            logger.severe(String.format("Failed to index document: ", e.getMessage()));
            e.printStackTrace();
        } catch (IOException e) {
            logger.severe(String.format("IO Exception: ", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void printAll() throws ElasticsearchException, IOException {
        client.indices().refresh(r -> r.index(Config.TABLE_INDEX));
        
        int total = 0;
        LocalDateTime iter = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
        while (iter.compareTo(Utils.now()) < 0) {
            final LocalDateTime iter2 = iter;
            
            SearchResponse<Post> res = client.search(s -> s
                    .index(Config.TABLE_INDEX)
                    .size(10000)
                    .query(q -> q.range(r -> r.date(d -> d
                            .field("dateTime")
                            .gte(DateTimeConverter.toStringFullISO(iter2))
                            .lt(DateTimeConverter.toStringFullISO(iter2.plusDays(1)))
                            )))
            , Post.class);
            
            List<Hit<Post>> hits = res.hits().hits();
    
            for (Hit<Post> hit : hits) {
                Post post = hit.source();
                System.out.println(post);
            }
            
            if (hits.isEmpty()) {
                System.out.println(String.format("No documents in day %s.", iter.toLocalDate()));
            }
            
            iter = iter.plusDays(1);
            
            total += hits.size();
        }
        
        System.out.println(String.format("Total = %d documents.", total));
    }

    public void removeAll() throws ElasticsearchException, IOException {
        boolean exists = client.indices().exists(e -> e.index(Config.TABLE_INDEX)).value();
        if (exists) {
            client.indices().delete(d -> d.index(Config.TABLE_INDEX));
            logger.info(String.format("Index %s is deleted.", Config.TABLE_INDEX));
        } else {
            logger.info(String.format("Index %s did not exist.", Config.TABLE_INDEX));
        }
    }

    public void close() throws IOException {
        client.close();
        logger.info("Elasticsearch connection is closed.");
    }

    public void bulkIndexDocuments(List<Post> posts) throws ElasticsearchException, IOException {
        int batchSize = 10000;
        boolean isError = false;
        
        for (int i = 0; i < posts.size(); i += batchSize) {
            List<Post> batch = posts.subList(i, Math.min(i + batchSize, posts.size()));
            BulkRequest.Builder br = new BulkRequest.Builder();

            for (Post post : batch) {
                br.operations(op -> op.index(idx -> idx.index(Config.TABLE_INDEX).id(post.getId()).document(post)));
            }

            BulkRequest req = br.build();
            BulkResponse res = client.bulk(req);

            if (res.errors()) {
                isError = true;
                logger.severe("Elasticsearch could not bulk documents.");
                for (BulkResponseItem item : res.items()) {
                    if (item.error() != null) {
                        logger.severe(String.format("Bulk error: %s", item.error().reason()));
                    }
                }
            }
        } 
        
        if (!isError) {
            logger.info(String.format("Day %s documents (%d) are indexed to Elasticsearch.",
                    posts.get(0).getDateTime().toLocalDate(), posts.size()));
        }

    }

    public ArrayList<Post> getDay(LocalDateTime datetime) throws ElasticsearchException, IOException {
        client.indices().refresh(r -> r.index(Config.TABLE_INDEX));

        ArrayList<Post> posts = new ArrayList<>();
        // Used a smaller batch size for better performance
        int size = 2000;
        List<FieldValue> searchAfterValues = null;

        // For safety purposes the number of iterations is limited
        for (int i = 0; i < 10000; i++) {
            final List<FieldValue> currentSearchAfterValues = searchAfterValues;
            
            SearchResponse<Post> res;
            if (currentSearchAfterValues == null) {
                // First request without searchAfter
                res = client.search(s -> s
                    .index(Config.TABLE_INDEX)
                    .size(size)
                    .query(q -> q.range(r -> r.date(d -> d
                        .field("dateTime")
                        .gte(DateTimeConverter.toStringFullISO(datetime))
                        .lt(DateTimeConverter.toStringFullISO(datetime.plusDays(1)))
                    )))
                    .sort(sort -> sort.field(f -> f.field("dateTime").order(SortOrder.Asc)))
                , Post.class);
            } else {
                // Subsequent requests with searchAfter
                res = client.search(s -> s
                    .index(Config.TABLE_INDEX)
                    .size(size)
                    .query(q -> q.range(r -> r.date(d -> d
                        .field("dateTime")
                        .gte(DateTimeConverter.toStringFullISO(datetime))
                        .lt(DateTimeConverter.toStringFullISO(datetime.plusDays(1)))
                    )))
                    .sort(sort -> sort.field(f -> f.field("dateTime").order(SortOrder.Asc)))
                    .searchAfter(currentSearchAfterValues)
                , Post.class);
            }

            List<Hit<Post>> hits = res.hits().hits();

            if (hits.isEmpty()) {
                break;
            }

            for (Hit<Post> hit : hits) {
                Post post = hit.source();
                posts.add(post);
            }

            // Get sort values from the last hit for next iteration
            searchAfterValues = hits.get(hits.size() - 1).sort();
        }

        return posts;
    }
}
