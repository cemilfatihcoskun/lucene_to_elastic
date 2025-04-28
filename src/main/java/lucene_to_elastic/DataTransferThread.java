package lucene_to_elastic;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.logging.Logger;

public class DataTransferThread implements Runnable {
    @Override
    public void run() {
        try {
            LuceneDataAccess lucene = new LuceneDataAccess();

            DataTransferManager dataTransferManager = new DataTransferManager();

            LocalDateTime syncDateTime = lucene.findFirstDatasDateTime();
            syncDateTime = SyncDateTimeManager.loadLastSyncDateTime(syncDateTime);
            
            // TODO: Currently only works by date. Also use time
            syncDateTime = syncDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
            while (!syncDateTime.toLocalDate().equals(Utils.now().toLocalDate())) {
                dataTransferManager.transferDataForDate(syncDateTime);
                
                SyncDateTimeManager.saveLastSyncDateTime(syncDateTime);
                syncDateTime = syncDateTime.plusDays(1);
            }
            
            Logger logger = Logger.getGlobal();
            logger.info(String.format("Data up to today (%s) is indexed to elasticsearch.", LocalDate.now()));
            
            dataTransferManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}