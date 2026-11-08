import java.util.ArrayList;
import java.util.List;

public class TextPreprocessor {
    public List<String> process(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String cleaned = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isEmpty()) {
            return List.of();
        }

        String[] tokens = cleaned.split(" ");
        List<String> words = new ArrayList<>();

        for (String token : tokens) {
            if (!token.isBlank()) {
                words.add(token);
            }
        }
        return words;
    }
}
