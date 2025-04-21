package lucene_to_elastic;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Random;

public class Utils {
    private static Random rand;
    private static DateTimeFormatter luceneDateTimeFormatter;
    
    static {
        rand = new Random();
    }
    
    public static String now() {
        return localDateTimeToLuceneFormat(LocalDateTime.now());
    }
    
    public static String localDateTimeToLuceneFormat(LocalDateTime datetime) {
        return DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(datetime);
    }
    
    public static String randomNumber() {
        return "" + (rand.nextInt(900000) + 100000);
    }
    
    public static int randomInteger(int min, int max) {
        return rand.nextInt(max - min + 1) + min;
    }

    public static LocalDateTime luceneFormatToLocalDateTime(String datetime) {
        String dt = datetime.substring(0, 14) + "." + datetime.substring(14, 17);
        return LocalDateTime.parse(dt, DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSS"));
    }
}
