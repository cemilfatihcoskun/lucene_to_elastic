package lucene_to_elastic;

import org.apache.lucene.document.Document;

public interface PostAdapter {
    Post luceneToPost(Document doc);
}
