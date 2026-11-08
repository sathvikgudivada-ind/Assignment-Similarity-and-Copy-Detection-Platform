import java.util.List;

public class SimilarityService {
    private final TextPreprocessor preprocessor = new TextPreprocessor();
    private final JaccardSimilarity jaccard = new JaccardSimilarity();
    private final CosineSimilarity cosine = new CosineSimilarity();

    public void compare(String assignment1, String assignment2) {
        List<String> words1 = preprocessor.process(assignment1);
        List<String> words2 = preprocessor.process(assignment2);

        if (words1.isEmpty() || words2.isEmpty()) {
            System.out.println("Cannot compare: one or both assignments contain no usable text.");
            return;
        }

        double jaccardScore = jaccard.calculate(words1, words2);
        double cosineScore = cosine.calculate(words1, words2);

        System.out.println();
        System.out.println("============== SIMILARITY REPORT ==============");
        System.out.printf("Words in assignment 1 : %d%n", words1.size());
        System.out.printf("Words in assignment 2 : %d%n", words2.size());
        System.out.printf("Jaccard similarity    : %.2f%%%n", jaccardScore);
        System.out.printf("Cosine similarity     : %.2f%%%n", cosineScore);
        System.out.println("================================================");
        System.out.println("Note: Similarity scores indicate textual overlap,");
        System.out.println("not proof of plagiarism. A teacher should review");
        System.out.println("the matching content and assignment context.");
    }
}
