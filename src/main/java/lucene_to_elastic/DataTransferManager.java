package lucene_to_elastic;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class DataTransferManager {
    private LuceneDataAccess luceneDataAccess;
    private ElasticsearchDataAccess elasticsearchDataAccess;
    private PostAdapter postAdapter;

    private static final Logger logger = Logger.getGlobal();

    public DataTransferManager() throws IOException {
        this.luceneDataAccess = new LuceneDataAccess();
        this.elasticsearchDataAccess = new ElasticsearchDataAccess();
        this.postAdapter = new PostAdapterImpl();
    }

    public void transferDataForDate(LocalDateTime datetime) throws IOException {
        Directory directory = FSDirectory.open(new File(Config.LUCENE_DATABASE_PATH));
        IndexSearcher searcher = new IndexSearcher(directory);

        datetime = datetime.withHour(0).withMinute(0).withSecond(0).withNano(0);

        String startDateTime = DateTimeConverter.toString(datetime);
        String endDateTime = DateTimeConverter.toString(datetime.plusDays(1));

        TermRangeQuery query = new TermRangeQuery("datetime", startDateTime, endDateTime, true, false);
        TopDocs results = searcher.search(query, Integer.MAX_VALUE);
        
        int dataAmount = results.totalHits;
        dataAmount = 100_000;
        
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < Math.min(dataAmount, results.totalHits); i++) {
            Document doc = searcher.doc(results.scoreDocs[i].doc);
            Post post = postAdapter.luceneToPost(doc);
            posts.add(post);
        }

        if (posts.size() == 0) {
            logger.info(String.format("Day %s has no data.", datetime.toLocalDate()));
        } else {
            elasticsearchDataAccess.bulkIndexDocuments(posts);
            //logger.info(String.format("%s day transactions (%d) are completed.", datetime, results.totalHits));
        }
        
        posts.clear();
        
        searcher.getIndexReader().close();
        searcher.close();
        directory.close();
    }

    public void close() throws IOException {
        elasticsearchDataAccess.close();
    }
}
