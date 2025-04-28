package lucene_to_elastic;

import java.io.IOException;
import java.time.LocalDate;
import java.util.logging.Logger;

public class LuceneDataGeneratorThread extends Thread {
    private Logger logger;
    private LocalDate executionDate;
    
    public LuceneDataGeneratorThread() {
        this.logger = Logger.getGlobal();
        executionDate = LocalDate.MAX;
    }
    
    @Override
    public void run() {
        LuceneDataAccess lucene = new LuceneDataAccess();
        try {
            while (true) {
                if (Utils.now().toLocalDate().compareTo(executionDate) >= 0) {
                    logger.info(String.format("Only lucene data generator is stopped in day %s.", LocalDate.now()));
                    Thread.currentThread().interrupt();
                    break;
                }
                
                Post post = Post.generate();
                logger.info(String.format("%s data is generated.", post));
                lucene.add(post);
                Thread.sleep(5000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Only Lucene data generator was interrupted.");
        }
    }

    public void setExecutionDay(LocalDate executionDate) {
        this.executionDate = executionDate;
        logger.info(String.format("Only Lucene Data Generator's execution date (%s) is set.", executionDate));
    }
}
