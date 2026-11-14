import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Web version of the Assignment Similarity and Copy Detection Platform.
 *
 * Starts a small web server on this computer only (127.0.0.1) and opens the page in the browser.
 * The page (web/index.html) does the display; this class does the real work: reading files,
 * calculating similarity and saving to MySQL through JDBC.
 *
 *   java -cp "out;lib/*" SimilarityWeb            (default port 8080)
 *   java -cp "out;lib/*" SimilarityWeb 9000       (another port)
 */
public class SimilarityWeb {

    private static final long MAX_BODY = 150L * 1024 * 1024;
    private static final int MAX_TEXT_SENT = 60000;
    private static final DatabaseManager DB = new DatabaseManager();
    private static final Object OUT_LOCK = new Object();
    private static int port = 8080;
    private static Path webDir;

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) { }
        }
        webDir = Paths.get("web").toAbsolutePath().normalize();
        if (!Files.isRegularFile(webDir.resolve("index.html"))) {
            System.out.println("Cannot find web/index.html. Run this program from the project folder.");
            System.exit(1);
        }
        HttpServer server = null;
        for (int p = port; p < port + 20 && server == null; p++) {
            try {
                server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), p), 0);
                port = p;
            } catch (BindException e) {
                // port busy, try the next one
            }
        }
        if (server == null) {
            System.out.println("No free port found near " + port);
            System.exit(1);
        }
        server.createContext("/", SimilarityWeb::handleStatic);
        server.createContext("/api/compare", ex -> guarded(ex, SimilarityWeb::handleCompare));
        server.createContext("/api/db", ex -> guarded(ex, SimilarityWeb::handleDb));
        server.setExecutor(Executors.newFixedThreadPool(6));
        server.start();

        String url = "http://localhost:" + port + "/";
        System.out.println("==============================================================");
        System.out.println(" Assignment Similarity & Copy Detection is running");
        System.out.println(" Open in your browser:  " + url);
        System.out.println(" Keep this window open while you use the page.");
        System.out.println(" Close this window (or press Ctrl+C) to stop.");
        System.out.println("==============================================================");
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            System.out.println("Could not open the browser automatically. Please open the link above.");
        }
    }

    // ------------------------------------------------------------------
    // HTTP helpers
    // ------------------------------------------------------------------

    private interface Handler { void handle(HttpExchange ex) throws Exception; }

    /** Only accept requests made by our own page (blocks other websites from calling the API). */
    private static void guarded(HttpExchange ex, Handler h) throws IOException {
        try {
            String host = ex.getRequestHeaders().getFirst("Host");
            boolean hostOk = host != null && (host.equals("localhost:" + port) || host.equals("127.0.0.1:" + port));
            if (!hostOk || !"1".equals(ex.getRequestHeaders().getFirst("X-App"))) {
                send(ex, 403, "application/json", "{\"error\":\"Forbidden\"}");
                return;
            }
            h.handle(ex);
        } catch (Throwable t) {
            String m = t.getMessage() == null ? t.toString() : t.getMessage();
            send(ex, 500, "application/json", "{\"error\":" + q(m) + "}");
        }
    }

    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path f = webDir.resolve(path.substring(1)).normalize();
        if (!f.startsWith(webDir) || !Files.isRegularFile(f)) {
            send(ex, 404, "text/plain; charset=utf-8", "Not found");
            return;
        }
        String name = f.getFileName().toString().toLowerCase();
        String type = name.endsWith(".html") ? "text/html; charset=utf-8"
                : name.endsWith(".css") ? "text/css; charset=utf-8"
                : name.endsWith(".js") ? "text/javascript; charset=utf-8"
                : name.endsWith(".svg") ? "image/svg+xml" : "application/octet-stream";
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        send(ex, 200, type, Files.readAllBytes(f));
    }

    private static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        send(ex, code, type, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(code, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    private static byte[] readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            long total = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > MAX_BODY) throw new IOException("Upload is too large (limit 150 MB).");
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        }
    }

    // ------------------------------------------------------------------
    // /api/compare
    // ------------------------------------------------------------------

    private static void handleCompare(HttpExchange ex) throws Exception {
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, "application/json", "{\"error\":\"POST only\"}"); return; }
        Object root = Json.parse(new String(readBody(ex), StandardCharsets.UTF_8));
        Map<String, Object> req = Json.obj(root);
        boolean save = Boolean.TRUE.equals(req.get("save"));
        List<Object> items = Json.arr(req.get("docs"));

        int n = items.size();
        SimilarityEngine.Doc[] docs = new SimilarityEngine.Doc[n];
        String[] names = new String[n];
        String[] errors = new String[n];

        for (int i = 0; i < n; i++) {
            Map<String, Object> it = Json.obj(items.get(i));
            names[i] = str(it.get("name"), "Document " + (i + 1));
            try {
                String text;
                if (it.get("text") != null) {
                    text = str(it.get("text"), "");
                } else {
                    text = extractUpload(names[i], str(it.get("data"), ""));
                }
                boolean code = it.get("code") instanceof Boolean
                        ? (Boolean) it.get("code") : SimilarityEngine.isCodeFile(names[i]);
                SimilarityEngine.Doc d = new SimilarityEngine.Doc(names[i], text, code);
                if (d.tokens.isEmpty()) throw new IOException("No readable words found.");
                docs[i] = d;
            } catch (Exception e) {
                errors[i] = e.getMessage() == null ? e.toString() : e.getMessage();
            }
        }

        // pairs
        List<int[]> idx = new ArrayList<>();
        List<SimilarityEngine.Result> res = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (docs[i] == null || docs[j] == null) continue;
                idx.add(new int[]{i, j});
                res.add(SimilarityEngine.compare(docs[i], docs[j]));
            }
        }
        Integer[] order = new Integer[res.size()];
        for (int k = 0; k < order.length; k++) order[k] = k;
        java.util.Arrays.sort(order, Comparator.comparingDouble((Integer k) -> overall(res.get(k))).reversed());

        String saveMessage = "";
        if (save) {
            try {
                saveToDatabase(docs, idx, res);
                saveMessage = "Saved to the MySQL database.";
            } catch (Exception e) {
                saveMessage = "NOT saved: " + e.getMessage();
            }
        }

        StringBuilder sb = new StringBuilder("{\"docs\":[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"name\":").append(q(names[i]));
            if (docs[i] != null) {
                String t = docs[i].text;
                boolean cut = t.length() > MAX_TEXT_SENT;
                sb.append(",\"words\":").append(docs[i].tokens.size())
                        .append(",\"isCode\":").append(docs[i].isCode)
                        .append(",\"truncated\":").append(cut)
                        .append(",\"text\":").append(q(cut ? t.substring(0, MAX_TEXT_SENT) : t));
            } else {
                sb.append(",\"error\":").append(q(errors[i]));
            }
            sb.append('}');
        }
        sb.append("],\"pairs\":[");
        for (int o = 0; o < order.length; o++) {
            int k = order[o];
            SimilarityEngine.Result r = res.get(k);
            if (o > 0) sb.append(',');
            sb.append("{\"a\":").append(idx.get(k)[0]).append(",\"b\":").append(idx.get(k)[1])
                    .append(",\"jaccard\":").append(num(r.jaccard))
                    .append(",\"cosine\":").append(num(r.cosine))
                    .append(",\"structure\":").append(r.structure >= 0 ? num(r.structure) : "null")
                    .append(",\"shared\":").append(r.sharedWords)
                    .append(",\"level\":").append(q(r.level()))
                    .append(",\"common\":[");
            int c = 0;
            for (String s : r.commonSentences) {
                if (c >= 200) break;
                if (c++ > 0) sb.append(',');
                sb.append(q(s));
            }
            sb.append("],\"commonB\":[");
            c = 0;
            for (String s2 : r.commonSentencesB) {
                if (c >= 200) break;
                if (c++ > 0) sb.append(',');
                sb.append(q(s2));
            }
            sb.append("],\"commonCount\":").append(r.commonSentences.size()).append('}');
        }
        sb.append("],\"saved\":").append(save && saveMessage.startsWith("Saved"))
                .append(",\"message\":").append(q(saveMessage)).append('}');
        send(ex, 200, "application/json; charset=utf-8", sb.toString());
    }

    private static double overall(SimilarityEngine.Result r) {
        return Math.max(Math.max(r.jaccard, r.cosine), r.structure);
    }

    private static String extractUpload(String name, String base64) throws IOException {
        if (base64.isEmpty()) throw new IOException("Empty file.");
        byte[] data;
        try {
            data = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IOException("Upload was damaged.");
        }
        String ext = DocumentExtractor.extensionOf(name);
        Path dir = Files.createTempDirectory("simapp");
        Path f = dir.resolve("upload" + (ext.isEmpty() ? "" : "." + ext));
        try {
            Files.write(f, data);
            return DocumentExtractor.extract(f.toFile());
        } finally {
            Files.deleteIfExists(f);
            Files.deleteIfExists(dir);
        }
    }

    private static void saveToDatabase(SimilarityEngine.Doc[] docs, List<int[]> idx,
                                       List<SimilarityEngine.Result> res) throws Exception {
        synchronized (DB) {
            DB.createTables();
            int[] ids = new int[docs.length];
            for (int i = 0; i < docs.length; i++) {
                if (docs[i] == null) continue;
                String nm = stem(docs[i].name);
                String email = nm.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ".")
                        .replaceAll("^\\.|\\.$", "") + "@unknown.local";
                int u = DB.findOrCreateUser(nm, email);
                ids[i] = DB.saveAssignment(u, docs[i].name, docs[i].text);
            }
            for (int k = 0; k < idx.size(); k++) {
                int a = ids[idx.get(k)[0]], b = ids[idx.get(k)[1]];
                SimilarityEngine.Result r = res.get(k);
                DB.saveResult(a, b, "Jaccard", round2(r.jaccard));
                DB.saveResult(a, b, "Cosine", round2(r.cosine));
                if (r.structure >= 0) DB.saveResult(a, b, "CodeStructure", round2(r.structure));
            }
        }
    }

    // ------------------------------------------------------------------
    // /api/db?action=test|create|assignments|results
    // ------------------------------------------------------------------

    private static void handleDb(HttpExchange ex) throws Exception {
        String q = ex.getRequestURI().getQuery() == null ? "" : ex.getRequestURI().getQuery();
        String action = q.startsWith("action=") ? q.substring(7) : "";
        String output;
        synchronized (OUT_LOCK) {
            PrintStream old = System.out;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                System.setOut(new PrintStream(bos, true, "UTF-8"));
                switch (action) {
                    case "test": DB.testConnection(); break;
                    case "create": DB.createTables(); System.out.println("Tables are ready."); break;
                    case "assignments": DB.printAllAssignments(); break;
                    case "results": DB.printAllResults(); break;
                    default: System.out.println("Unknown action.");
                }
            } catch (Exception e) {
                System.out.println("Database error: " + e.getMessage());
            } finally {
                System.setOut(old);
            }
            output = bos.toString("UTF-8");
        }
        send(ex, 200, "application/json; charset=utf-8", "{\"output\":" + q(output) + "}");
    }

    // ------------------------------------------------------------------
    // small helpers
    // ------------------------------------------------------------------

    private static String stem(String name) {
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private static String num(double v) { return String.format(Locale.ROOT, "%.2f", v); }

    private static String str(Object o, String def) { return o instanceof String ? (String) o : def; }

    /** Quotes a string as JSON. */
    static String q(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case ' ': sb.append("\\u2028"); break;
                case ' ': sb.append("\\u2029"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /** A very small JSON reader (objects, arrays, strings, numbers, booleans, null). */
    static class Json {
        private final String s;
        private int p;
        private Json(String s) { this.s = s; }

        static Object parse(String text) {
            Json j = new Json(text);
            j.ws();
            Object v = j.value();
            j.ws();
            if (j.p != j.s.length()) throw new IllegalArgumentException("Bad JSON");
            return v;
        }

        @SuppressWarnings("unchecked")
        static Map<String, Object> obj(Object o) {
            if (!(o instanceof Map)) throw new IllegalArgumentException("Expected an object");
            return (Map<String, Object>) o;
        }

        @SuppressWarnings("unchecked")
        static List<Object> arr(Object o) {
            if (!(o instanceof List)) throw new IllegalArgumentException("Expected a list");
            return (List<Object>) o;
        }

        private void ws() { while (p < s.length() && Character.isWhitespace(s.charAt(p))) p++; }

        private Object value() {
            if (p >= s.length()) throw new IllegalArgumentException("Bad JSON");
            char c = s.charAt(p);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (s.startsWith("true", p)) { p += 4; return Boolean.TRUE; }
            if (s.startsWith("false", p)) { p += 5; return Boolean.FALSE; }
            if (s.startsWith("null", p)) { p += 4; return null; }
            int st = p;
            while (p < s.length() && "+-0123456789.eE".indexOf(s.charAt(p)) >= 0) p++;
            if (st == p) throw new IllegalArgumentException("Bad JSON");
            return Double.valueOf(s.substring(st, p));
        }

        private Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            p++; ws();
            if (s.charAt(p) == '}') { p++; return m; }
            while (true) {
                ws();
                String k = string();
                ws();
                if (s.charAt(p++) != ':') throw new IllegalArgumentException("Bad JSON");
                ws();
                m.put(k, value());
                ws();
                char c = s.charAt(p++);
                if (c == '}') return m;
                if (c != ',') throw new IllegalArgumentException("Bad JSON");
            }
        }

        private List<Object> array() {
            List<Object> l = new ArrayList<>();
            p++; ws();
            if (s.charAt(p) == ']') { p++; return l; }
            while (true) {
                ws();
                l.add(value());
                ws();
                char c = s.charAt(p++);
                if (c == ']') return l;
                if (c != ',') throw new IllegalArgumentException("Bad JSON");
            }
        }

        private String string() {
            if (s.charAt(p) != '"') throw new IllegalArgumentException("Bad JSON");
            p++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = s.charAt(p++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char e = s.charAt(p++);
                    switch (e) {
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u': sb.append((char) Integer.parseInt(s.substring(p, p + 4), 16)); p += 4; break;
                        default: sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }
    }
}
