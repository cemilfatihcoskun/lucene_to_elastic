package lucene_to_elastic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.Logger;

public class SyncDateTimeManager {
    private static final String LAST_SYNC_DATETIME_FILE = "lastSyncDatetime.txt";

    public LocalDateTime loadLastSyncDateTime(LocalDateTime firstDate) throws IOException {
        Logger logger = Logger.getGlobal();
        
        File file = new File(LAST_SYNC_DATETIME_FILE);

        if (!file.exists()) {
            logger.info("lastSyncDate.txt does not exist.");
            return firstDate;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return DateTimeConverter.toLocalDateTime(reader.readLine().trim());
        }
    }


    public void saveLastSyncDateTime(LocalDateTime dateTime) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LAST_SYNC_DATETIME_FILE))) {
            writer.write(DateTimeConverter.toString(dateTime));
            writer.flush();
        }
    }

}
