package lucene_to_elastic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getGlobal();
    
    /*
     * 1. Read the last day from a file if not exists create from lucene 
     * 2. Fetch all the data from that day 
     * 3. Insert those into the elasticsearch 
     * 4. Save that day into a file
     */

    // Java MAX INTEGER = 2147483647
    // Seconds in a day = 86400
    // 100 data in a second = 8640000 approx 10 million

    // http://localhost:9200/posts/_search?q=dateTime:[2000-01-01T00:00:00.000%20TO%202000-01-01T23:59:59.999]
    
    public static void main(String[] args) throws Exception {               
        LuceneDataAccess lucene = new LuceneDataAccess();
        //lucene.removeAll();
        //lucene.generateAndStoreRandomPosts(3, 10_000_000);
        //lucene.printAll();

        ElasticsearchDataAccess elastic = new ElasticsearchDataAccess();
        elastic.removeAll();
        elastic.close();
        
        DataTransferManager dataTransferManager = new DataTransferManager();
        SyncDateTimeManager syncDateTimeManager = new SyncDateTimeManager();

        LocalDateTime syncDateTime = lucene.findFirstDatasDateTime();
        syncDateTime = syncDateTimeManager.loadLastSyncDateTime(syncDateTime);
        syncDateTime = syncDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);

        // It stops today
        // Todays data is not going to be transfered
        // Because there is a ongoing data stream today
        // TODO: Computers time could be broken. Think about what will happen then.
        LocalDateTime today = LocalDateTime.of(LocalDate.now(), LocalTime.of(0, 0));
        
        while (!syncDateTime.equals(today)) {
            dataTransferManager.transferDataForDate(syncDateTime);
            
            syncDateTimeManager.saveLastSyncDateTime(syncDateTime);
            syncDateTime = syncDateTime.plusDays(1);
        }
        
        dataTransferManager.close();
    }
}
