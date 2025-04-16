package xyz;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class Utils {
    private static Random rand;
    
    static {
        rand = new Random();
    }
    
    public static String now() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
    
    public static String randomNumber() {
        return "" + (rand.nextInt(900000) + 100000);
    }
}
