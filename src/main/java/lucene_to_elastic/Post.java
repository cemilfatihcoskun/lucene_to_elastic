package lucene_to_elastic;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

public class Post {
    private String id;
    private String content;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime dateTime;
    
    public Post() {
        
    }
    
    public String getContent() {
        return content;
    }
    
    public LocalDateTime getDateTime() {
        return dateTime;
    }
    
    public Post setContent(String content) {
        this.content = content;
        return this;
    }
    
    public Post setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
        return this;
    }

    public static Post generate() {
        return new Post()
                .setId(UUID.randomUUID().toString())
                .setContent(Utils.randomNumber())
                .setDateTime(LocalDateTime.now());
    }
    
    public static Post generateWithTime(LocalDateTime dateTime) {
        return new Post()
                .setId(UUID.randomUUID().toString())
                .setContent(Utils.randomNumber())
                .setDateTime(dateTime);
    }

    public String getId() {
        return id;
    }
    
    @Override
    public String toString() {
        return String.format("Post (content=%s) - %s", content, dateTime);
    }

    public Post setId(String id) {
        this.id = id;
        return this;
    }
}
