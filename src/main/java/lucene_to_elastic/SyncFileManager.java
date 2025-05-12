package lucene_to_elastic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.Logger;

public class SyncFileManager {
    private static final String LAST_SYNC_DATETIME_FILE = "lastSyncContent.txt";

    
    public static void saveLastSync(String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LAST_SYNC_DATETIME_FILE))) {
            writer.write(content);
            writer.flush();
            
            Logger logger = Logger.getGlobal();
            logger.info(String.format("%s is stored in synchronization file.", content));
        }
    }
    
    public static String loadLastSync() throws IOException {
        Logger logger = Logger.getGlobal();
        
        File file = new File(LAST_SYNC_DATETIME_FILE);

        if (!file.exists()) {
            logger.info(String.format("%s does not exist. It will be created after some data transfer.", LAST_SYNC_DATETIME_FILE));
            return "";
        }
        
        

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String last = reader.readLine().trim();
            logger.info(String.format("Last content %s is retrieved from sync file.", last));
            return last;
        }
    }
    
    
    public static LocalDateTime loadLastSyncDateTime(LocalDateTime firstDate) throws IOException {
        Logger logger = Logger.getGlobal();
        
        File file = new File(LAST_SYNC_DATETIME_FILE);

        if (!file.exists()) {
            SyncFileManager.saveLastSyncDateTime(firstDate);
            logger.info(String.format("%s does not exist. So it created and starts in %s", LAST_SYNC_DATETIME_FILE, firstDate));
            return firstDate;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return DateTimeConverter.toLocalDateTime(reader.readLine().trim());
        }
    }


  


    public static void saveLastSyncDateTime(LocalDateTime dateTime) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LAST_SYNC_DATETIME_FILE))) {
            writer.write(DateTimeConverter.toString(dateTime));
            writer.flush();
        }
    }
    
    public static void clean() {
        Logger logger = Logger.getGlobal();
        
        File file = new File(LAST_SYNC_DATETIME_FILE);
        if (file.exists()) {
            file.delete();
            logger.info(String.format("Synchronization file %s is deleted.", LAST_SYNC_DATETIME_FILE));
        } else {
            // TODO: I am not sure if this is needed
            //logger.info(String.format("Synchronization file %s does not exists.", LAST_SYNC_DATETIME_FILE));
        }
    }
}
