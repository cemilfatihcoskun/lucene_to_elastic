package xyz;

import java.util.UUID;

public class Post {
    private String id;
    private String content;
    private String dateTime;
    
    public Post() {
        
    }
    
    public String getContent() {
        return content;
    }
    
    public String getDateTime() {
        return dateTime;
    }
    
    public Post setContent(String content) {
        this.content = content;
        return this;
    }
    
    public Post setDateTime(String dateTime) {
        this.dateTime = dateTime;
        return this;
    }

    public static Post generate() {
        return new Post()
                .setId(UUID.randomUUID().toString())
                .setContent(Utils.randomNumber())
                .setDateTime(Utils.now());
    }

    public String getId() {
        return id;
    }
    
    @Override
    public String toString() {
        return String.format("Post: %s - %s", dateTime, content);
    }

    public Post setId(String id) {
        this.id = id;
        return this;
    }
    
}
