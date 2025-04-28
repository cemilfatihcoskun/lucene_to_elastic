package lucene_to_elastic;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Random;

public class Utils {
    private static Random rand;
        
    static {
        rand = new Random();
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
}
