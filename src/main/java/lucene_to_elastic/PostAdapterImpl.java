package lucene_to_elastic;

import org.apache.lucene.document.Document;

public class PostAdapterImpl implements PostAdapter {
    @Override
    public Post luceneToPost(Document doc) {
        Post post = new Post();
        post.setId(doc.get("id"));
        post.setContent(doc.get("content"));
        post.setDateTime(DateTimeConverter.toLocalDateTime(doc.get("datetime")));
        return post;
    }
}
