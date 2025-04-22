package lucene_to_elastic;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeConverter {
    public static LocalDateTime toLocalDateTime(String datetime) {
        if (datetime.length() != 17) {
            new Exception("DateTime string length must be exactly 17.");
        }
        
        String dt = datetime.substring(0, 14) + "." + datetime.substring(14, 17);
        return LocalDateTime.parse(dt, DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSS"));
    }
    
    public static String toString(LocalDateTime datetime) {
        return DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(datetime);
    }
    
}
