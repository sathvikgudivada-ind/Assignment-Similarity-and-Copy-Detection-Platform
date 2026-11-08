import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JaccardSimilarity {
    public double calculate(List<String> words1, List<String> words2) {
        Set<String> set1 = new HashSet<>(words1);
        Set<String> set2 = new HashSet<>(words2);

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) {
            return 0.0;
        }
        return (double) intersection.size() / union.size() * 100.0;
    }
}
