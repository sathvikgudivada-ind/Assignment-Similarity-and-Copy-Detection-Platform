import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Text preprocessing and similarity measures.
 *
 * Preprocessing: lowercase, remove non-letter/digit characters, split into words.
 * Jaccard: shared unique words / all unique words.
 * Cosine: cosine of the angle between the word-frequency vectors.
 */
public class SimilarityEngine {

    /** A document prepared for comparison. Prepare once, compare many times. */
    public static class Doc {
        public final String name;
        public final String text;
        public final List<String> tokens;
        public final Set<String> uniqueWords;
        public final Map<String, Integer> freq;
        public final Set<String> sentenceKeys;       // normalised sentences (for evidence)
        public final List<String> sentences;         // original sentences
        public final Map<String, String> sentenceByKey = new HashMap<>();
        public final boolean isCode;                 // true for source-code files
        public final Map<String, Integer> codeGrams; // structure shingles (names ignored)

        public Doc(String name, String text) {
            this(name, text, isCodeFile(name));
        }

        public Doc(String name, String text, boolean code) {
            this.name = name;
            this.text = text;
            this.tokens = tokenize(text);
            this.uniqueWords = new HashSet<>(tokens);
            this.freq = new HashMap<>();
            for (String t : tokens) freq.merge(t, 1, Integer::sum);
            this.isCode = code;
            this.codeGrams = isCode ? shingles(codeTokens(text), 3) : new HashMap<>();
            this.sentences = new ArrayList<>();
            this.sentenceKeys = new LinkedHashSet<>();
            for (String s : text.split("(?<=[.!?;])\\s+|\\r?\\n+")) {
                String key = String.join(" ", tokenize(s));
                if (key.split(" ").length >= 6) {          // ignore very short lines
                    sentences.add(s.trim());
                    sentenceKeys.add(key);
                    sentenceByKey.putIfAbsent(key, s.trim());
                }
            }
        }
    }

    public static class Result {
        public String nameA, nameB;
        public int wordsA, wordsB, sharedWords;
        public double jaccard, cosine;            // percentages 0-100
        public double structure = -1;             // code structure similarity, -1 if not code
        public List<String> commonSentences = new ArrayList<>();
        public List<String> commonSentencesB = new ArrayList<>();   // same sentences as written in file B

        /** Interpretation band, as in the project report (based on the higher score). */
        public String level() {
            double s = Math.max(Math.max(jaccard, cosine), structure);
            if (s <= 20) return "Low textual similarity";
            if (s <= 40) return "Limited textual similarity";
            if (s <= 60) return "Moderate textual similarity";
            if (s <= 80) return "High textual similarity";
            return "Very high textual similarity";
        }
    }

    public static List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        String cleaned = text.toLowerCase().replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        if (cleaned.isEmpty()) return out;
        for (String w : cleaned.split(" ")) if (!w.isEmpty()) out.add(w);
        return out;
    }

    public static double jaccard(Doc a, Doc b) {
        Set<String> small = a.uniqueWords.size() <= b.uniqueWords.size() ? a.uniqueWords : b.uniqueWords;
        Set<String> large = small == a.uniqueWords ? b.uniqueWords : a.uniqueWords;
        int inter = 0;
        for (String w : small) if (large.contains(w)) inter++;
        int union = a.uniqueWords.size() + b.uniqueWords.size() - inter;
        return union == 0 ? 0 : 100.0 * inter / union;
    }

    public static double cosine(Doc a, Doc b) {
        return cosineMaps(a.freq, b.freq);
    }

    static double cosineMaps(Map<String, Integer> fa, Map<String, Integer> fb) {
        Map<String, Integer> small = fa.size() <= fb.size() ? fa : fb;
        Map<String, Integer> large = small == fa ? fb : fa;
        double dot = 0;
        for (Map.Entry<String, Integer> e : small.entrySet()) {
            Integer o = large.get(e.getKey());
            if (o != null) dot += (double) e.getValue() * o;
        }
        double na = 0, nb = 0;
        for (int v : fa.values()) na += (double) v * v;
        for (int v : fb.values()) nb += (double) v * v;
        double den = Math.sqrt(na) * Math.sqrt(nb);
        return den == 0 ? 0 : 100.0 * dot / den;
    }

    public static Result compare(Doc a, Doc b) {
        Result r = new Result();
        r.nameA = a.name;
        r.nameB = b.name;
        r.wordsA = a.tokens.size();
        r.wordsB = b.tokens.size();
        for (String w : a.uniqueWords) if (b.uniqueWords.contains(w)) r.sharedWords++;
        r.jaccard = jaccard(a, b);
        r.cosine = cosine(a, b);
        if (a.isCode && b.isCode) r.structure = cosineMaps(a.codeGrams, b.codeGrams);
        for (int i = 0; i < a.sentences.size(); i++) {
            String key = String.join(" ", tokenize(a.sentences.get(i)));
            if (b.sentenceKeys.contains(key)) {
                r.commonSentences.add(a.sentences.get(i));
                r.commonSentencesB.add(b.sentenceByKey.get(key));
            }
        }
        return r;
    }

    // ------------------------------------------------------------------
    // Source-code structure (variable/method names, comments, spacing ignored)
    // ------------------------------------------------------------------

    private static final Set<String> CODE_EXT = new HashSet<>(java.util.Arrays.asList(
            "java", "py", "c", "cpp", "cc", "h", "hpp", "cs", "js", "ts", "go", "rb", "php",
            "kt", "swift", "rs", "scala", "m", "r", "sql"));

    private static final Set<String> KEYWORDS = new HashSet<>(java.util.Arrays.asList(
            "abstract", "and", "as", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "def", "default", "del", "do", "double", "elif", "else",
            "enum", "except", "extends", "final", "finally", "float", "for", "from", "func", "function",
            "global", "goto", "if", "implements", "import", "in", "instanceof", "int", "interface",
            "is", "lambda", "let", "long", "new", "nonlocal", "not", "null", "none", "or", "package",
            "pass", "private", "protected", "public", "raise", "return", "short", "signed", "static",
            "struct", "super", "switch", "this", "throw", "throws", "try", "typedef", "unsigned", "var",
            "void", "volatile", "while", "with", "yield", "true", "false", "self", "elseif", "foreach",
            "namespace", "using", "string", "bool", "auto", "include", "define"));

    public static boolean isCodeFile(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 && CODE_EXT.contains(name.substring(i + 1).toLowerCase());
    }

    /** Tokens of a program with comments removed and all names/values replaced by placeholders. */
    public static List<String> codeTokens(String code) {
        String s = code.replaceAll("(?s)/\\*.*?\\*/", " ")          // block comments
                .replaceAll("(?m)//.*$", " ")                           // line comments
                .replaceAll("(?m)(^|\\s)#(?!include|define).*$", " ");  // python/shell comments
        s = s.replaceAll("\"(\\\\.|[^\"\\\\])*\"|'(\\\\.|[^'\\\\])*'", " STR ");  // strings
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "[A-Za-z_][A-Za-z_0-9]*|\\d+(\\.\\d+)?|==|!=|<=|>=|&&|\\|\\||\\+\\+|--|[^\\s]").matcher(s);
        List<String> out = new ArrayList<>();
        while (m.find()) {
            String t = m.group();
            char c = t.charAt(0);
            if (Character.isDigit(c)) out.add("NUM");
            else if (Character.isLetter(c) || c == '_') {
                String low = t.toLowerCase();
                out.add(t.equals("STR") ? "STR" : KEYWORDS.contains(low) ? low : "ID");
            } else out.add(t);
        }
        return out;
    }

    private static Map<String, Integer> shingles(List<String> toks, int n) {
        Map<String, Integer> map = new HashMap<>();
        if (toks.size() < n) n = 1;
        for (int i = 0; i + n <= toks.size(); i++) {
            map.merge(String.join(" ", toks.subList(i, i + n)), 1, Integer::sum);
        }
        return map;
    }
}
