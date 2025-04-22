package lucene_to_elastic;

public class PostObjectSizeTest {
    public static void main() {
        int postCount = 100_000;
        int testCount = 5;
        int sum = 0;

        for (int i = 0; i < testCount; i++) {
            long beforeUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            Post[] posts = new Post[postCount];
            long afterUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long usage = afterUsedMem - beforeUsedMem;
            
            for (int j = 0; j < postCount; j++) {
                posts[j] = null;
            }
            posts = null;
            
            sum += usage;
            System.out.println(usage);
        }
        
        System.out.println("Average = " + sum / testCount);
    }
}
