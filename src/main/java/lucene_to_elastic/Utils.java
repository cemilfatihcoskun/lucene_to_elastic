package lucene_to_elastic;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.zip.CRC32;

public class Utils {
    private static Random rand;
        
    static {
        rand = new Random();
    }
    
    public static String uuid() {
        return UUID.randomUUID().toString();
    }
    
    public static String randomNumber() {
        return "" + (rand.nextInt(900000) + 100000);
    }
    
    public static int randomInteger(int min, int max) {
        return rand.nextInt(max - min + 1) + min;
    }

    // TODO: Change ZonedDateTime to LocalDateTime
    // For testing purposes it uses ZonedDateTime
    public static LocalDateTime now() {
        return ZonedDateTime.now().toLocalDateTime();
    }
    
    public static long calculateCheckSum(String data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data.getBytes());
        return crc32.getValue();
    }
}
