package lucene_to_elastic;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Main {
    // Java MAX INTEGER = 2147483647
    // Seconds in a day = 86400
    // 100 data in a second = 8640000 approx 10 million
    
    // Limitation: 10000 documents in a day

    // http://localhost:9200/posts/_search?q=dateTime:[2000-01-01T00:00:00.000%20TO%202000-01-01T23:59:59.999]
    
    public static void clean() throws IOException {
        SyncDateTimeManager.clean();
        
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
    
    public static void main(String[] args) throws Exception {
        Main.handleLogConfig();
        Main.clean();
        
        
        Main.initLuceneData(10, 1000);
        Main.transferAndGenerateAfterDoneTransfer();
        
    }
}
