import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CosineSimilarity {
    public double calculate(List<String> words1, List<String> words2) {
        Map<String, Integer> vector1 = frequency(words1);
        Map<String, Integer> vector2 = frequency(words2);

        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (Map.Entry<String, Integer> entry : vector1.entrySet()) {
            int value1 = entry.getValue();
            int value2 = vector2.getOrDefault(entry.getKey(), 0);
            dotProduct += (double) value1 * value2;
        }

        for (int value : vector1.values()) {
            magnitude1 += (double) value * value;
        }
        for (int value : vector2.values()) {
            magnitude2 += (double) value * value;
        }

        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2)) * 100.0;
    }

    private Map<String, Integer> frequency(List<String> words) {
        Map<String, Integer> counts = new HashMap<>();
        for (String word : words) {
            counts.put(word, counts.getOrDefault(word, 0) + 1);
        }
        return counts;
    }
}
