package lucene_to_elastic;

import org.apache.lucene.document.Document;

public class PostAdapterImpl implements PostAdapter {
    @Override
    public Post luceneToPost(Document doc) {
        Post post = new Post();
        post.setId(doc.get("id"));
        // TODO: Currently useless only returns empty string
        post.setContent(doc.get(Config.UNSTORED_FIELD_NAME));
        post.setDateTime(DateTimeConverter.toLocalDateTime(doc.get(Config.TIMESTAMPS_NAME)));
        return post;
    }
}
