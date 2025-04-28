package lucene_to_elastic;

import java.io.IOException;
import java.util.logging.Logger;

public class LuceneAndElasticsearchDataGenerator implements Runnable {
    @Override
    public void run() {
        Logger logger = Logger.getGlobal();
        LuceneDataAccess lucene = new LuceneDataAccess();
        ElasticsearchDataAccess elastic;
        try {
            elastic = new ElasticsearchDataAccess();
            
            while (true) {
                try {
                    Post post = Post.generate();
                    logger.info(String.format("%s data is generated.", post));
                    lucene.add(post);
                    elastic.indexDocument(post);
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.info("Lucene and Elasticsearch data generator stopped.");
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
}
