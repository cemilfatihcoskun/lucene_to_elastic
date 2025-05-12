package lucene_to_elastic;

//sudo chown -Rv 1000:0 elasticsearch_database

public class Config {
    public static final String LUCENE_DATABASE_PATH = "lucene_database";
    public static final String ELASTIC_SERVER_URL = "http://localhost:9200";
    public static final String ELASTIC_API_KEY = "NEJWYVBwWUI0QmN0bjZvaEFEYVM6NVlVUGQ4TW1STFdqdkhZTWR6c180Zw==";
    public static final String TABLE_INDEX = "posts";
    public static final String TIMESTAMPS_NAME = "dateTime";
    public static final String UNSTORED_FIELD_NAME = "content";
    public static final int BULK_SIZE = 10000;
}
