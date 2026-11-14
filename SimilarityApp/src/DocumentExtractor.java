import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Extracts plain text from many file formats.
 *
 * Supported: plain text and source code (.txt .java .py .c .cpp .js .html .csv .md ...),
 * Word (.docx), PowerPoint (.pptx), Excel (.xlsx), OpenDocument (.odt .odp .ods),
 * RTF (.rtf), HTML (.html .htm) and PDF (.pdf).
 *
 * PDF: if the Apache PDFBox JAR is in the lib folder it is used (best results).
 * Otherwise a built-in basic extractor is used, which works for ordinary text PDFs.
 * Scanned PDFs (pictures of text) contain no text and cannot be read.
 */
public class DocumentExtractor {

    private static final long MAX_ENTRY_BYTES = 80L * 1024 * 1024;

    public static String extract(File file) throws IOException {
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            throw new IOException("File does not exist or is not readable: " + file);
        }
        String ext = extensionOf(file.getName());
        switch (ext) {
            case "docx": case "dotx":
                return fromDocx(file);
            case "pptx": case "potx":
                return fromPptx(file);
            case "xlsx": case "xltx":
                return fromXlsx(file);
            case "odt": case "odp": case "ods":
                return fromOpenDocument(file);
            case "pdf":
                return fromPdf(file);
            case "rtf":
                return fromRtf(readText(file));
            case "html": case "htm": case "xhtml":
                return stripTags(readText(file), true);
            case "doc": case "ppt": case "xls":
                throw new IOException("Old Office format (." + ext + ") is not supported. "
                        + "Open it in Word/PowerPoint/Excel and use Save As to ." + ext + "x");
            case "zip": case "rar": case "7z": case "exe": case "jar": case "class":
            case "png": case "jpg": case "jpeg": case "gif": case "bmp": case "mp3": case "mp4":
                throw new IOException("Unsupported file type: ." + ext);
            default:
                return readText(file);
        }
    }

    /** True if the extension is one the program can read (used when scanning folders). */
    public static boolean isSupported(File f) {
        String ext = extensionOf(f.getName());
        switch (ext) {
            case "doc": case "ppt": case "xls": case "zip": case "rar": case "7z": case "exe":
            case "jar": case "class": case "png": case "jpg": case "jpeg": case "gif":
            case "bmp": case "mp3": case "mp4": case "db": case "properties":
                return false;
            default:
                return !f.getName().startsWith(".");
        }
    }

    public static String extensionOf(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase();
    }

    // ------------------------------------------------------------------
    // Plain text
    // ------------------------------------------------------------------

    private static String readText(File file) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        int nul = 0;
        for (int i = 0; i < Math.min(data.length, 4096); i++) if (data[i] == 0) nul++;
        if (nul > 8) {
            throw new IOException("This looks like a binary file, not text: " + file.getName());
        }
        // strip UTF-8 BOM
        int off = (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB
                && (data[2] & 0xFF) == 0xBF) ? 3 : 0;
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data, off, data.length - off)).toString();
        } catch (CharacterCodingException e) {
            return new String(data, off, data.length - off, "windows-1252");
        }
    }

    // ------------------------------------------------------------------
    // Office Open XML / OpenDocument (ZIP based)
    // ------------------------------------------------------------------

    private static String fromDocx(File f) throws IOException {
        try (ZipFile zip = new ZipFile(f)) {
            StringBuilder sb = new StringBuilder();
            appendXml(zip, "word/document.xml", sb, "</w:p>", "<w:tab/>", "<w:br/>");
            for (int i = 1; i <= 6; i++) {          // headers and footers
                appendXml(zip, "word/header" + i + ".xml", sb, "</w:p>", "<w:tab/>", "<w:br/>");
                appendXml(zip, "word/footer" + i + ".xml", sb, "</w:p>", "<w:tab/>", "<w:br/>");
            }
            appendXml(zip, "word/footnotes.xml", sb, "</w:p>", "<w:tab/>", "<w:br/>");
            return sb.toString();
        } catch (java.util.zip.ZipException e) {
            throw new IOException("Not a valid .docx file (is it an old .doc renamed?): " + f.getName());
        }
    }

    private static String fromPptx(File f) throws IOException {
        try (ZipFile zip = new ZipFile(f)) {
            List<String> slides = new ArrayList<>();
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                String n = en.nextElement().getName();
                if (n.startsWith("ppt/slides/slide") && n.endsWith(".xml")) slides.add(n);
            }
            Collections.sort(slides, (a, b) -> Integer.compare(slideNo(a), slideNo(b)));
            StringBuilder sb = new StringBuilder();
            for (String s : slides) appendXml(zip, s, sb, "</a:p>", null, "<a:br/>");
            for (String s : new ArrayList<>(slides)) {      // speaker notes
                String notes = s.replace("ppt/slides/slide", "ppt/notesSlides/notesSlide");
                appendXml(zip, notes, sb, "</a:p>", null, "<a:br/>");
            }
            return sb.toString();
        } catch (java.util.zip.ZipException e) {
            throw new IOException("Not a valid .pptx file: " + f.getName());
        }
    }

    private static int slideNo(String name) {
        Matcher m = Pattern.compile("(\\d+)\\.xml$").matcher(name);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static String fromXlsx(File f) throws IOException {
        try (ZipFile zip = new ZipFile(f)) {
            StringBuilder sb = new StringBuilder();
            appendXml(zip, "xl/sharedStrings.xml", sb, "</si>", null, null);
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {              // inline strings on sheets
                String n = en.nextElement().getName();
                if (n.startsWith("xl/worksheets/sheet") && n.endsWith(".xml")) {
                    String xml = readEntry(zip, n);
                    Matcher m = Pattern.compile("<is>(.*?)</is>", Pattern.DOTALL).matcher(xml);
                    while (m.find()) sb.append(unescape(m.group(1).replaceAll("<[^>]+>", ""))).append('\n');
                }
            }
            return sb.toString();
        } catch (java.util.zip.ZipException e) {
            throw new IOException("Not a valid .xlsx file: " + f.getName());
        }
    }

    private static String fromOpenDocument(File f) throws IOException {
        try (ZipFile zip = new ZipFile(f)) {
            StringBuilder sb = new StringBuilder();
            appendXml(zip, "content.xml", sb, "</text:p>", "<text:tab/>", "<text:line-break/>");
            return sb.toString();
        } catch (java.util.zip.ZipException e) {
            throw new IOException("Not a valid OpenDocument file: " + f.getName());
        }
    }

    private static void appendXml(ZipFile zip, String entry, StringBuilder out,
                                  String paragraphEnd, String tab, String lineBreak) throws IOException {
        ZipEntry ze = zip.getEntry(entry);
        if (ze == null) return;
        String xml = readEntry(zip, entry);
        if (paragraphEnd != null) xml = xml.replace(paragraphEnd, paragraphEnd + "\n");
        if (tab != null) xml = xml.replace(tab, " ");
        if (lineBreak != null) xml = xml.replace(lineBreak, "\n");
        if (entry.contains("content.xml")) {
            xml = xml.replace("</text:h>", "</text:h>\n");
        }
        out.append(unescape(xml.replaceAll("<[^>]+>", ""))).append('\n');
    }

    private static String readEntry(ZipFile zip, String name) throws IOException {
        ZipEntry ze = zip.getEntry(name);
        if (ze == null) return "";
        try (InputStream in = zip.getInputStream(ze)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > MAX_ENTRY_BYTES) throw new IOException("File is too large to read.");
                bos.write(buf, 0, n);
            }
            return bos.toString("UTF-8");
        }
    }

    // ------------------------------------------------------------------
    // HTML / RTF
    // ------------------------------------------------------------------

    private static String stripTags(String html, boolean dropScripts) {
        if (dropScripts) {
            html = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ");
        }
        html = html.replaceAll("(?i)<br\\s*/?>|</p>|</div>|</li>|</h[1-6]>|</tr>", "\n");
        return unescape(html.replaceAll("<[^>]+>", " "));
    }

    private static String fromRtf(String rtf) {
        String s = rtf.replaceAll("\\\\'[0-9a-fA-F]{2}", " ");
        s = s.replaceAll("\\\\(par|line|tab)\\b ?", "\n");
        s = s.replaceAll("\\{\\\\\\*[^{}]*\\}", " ");
        s = s.replaceAll("\\\\[a-zA-Z]+-?\\d* ?", "");
        s = s.replaceAll("[{}]", "");
        return s;
    }

    private static String unescape(String s) {
        s = s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&nbsp;", " ").replace("&amp;", "&");
        Matcher m = Pattern.compile("&#(x?[0-9a-fA-F]+);").matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String g = m.group(1);
            int cp;
            try {
                cp = g.startsWith("x") ? Integer.parseInt(g.substring(1), 16) : Integer.parseInt(g);
            } catch (NumberFormatException e) {
                cp = ' ';
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(cp))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // PDF
    // ------------------------------------------------------------------

    private static String fromPdf(File f) throws IOException {
        String text = null;
        try {
            text = pdfWithPdfBox(f);          // best quality when the PDFBox JAR is available
        } catch (ClassNotFoundException e) {
            // JAR not present: use the built-in extractor below
        }
        if (text == null) text = BasicPdfText.extract(Files.readAllBytes(f.toPath()));
        if (text.trim().isEmpty()) {
            throw new IOException("No readable text found in " + f.getName()
                    + ". It may be a scanned PDF (pictures of text). "
                    + "Adding the PDFBox JAR to the lib folder can help with some PDFs.");
        }
        return text;
    }

    /** Uses Apache PDFBox through reflection so the project compiles without the JAR. */
    private static String pdfWithPdfBox(File f) throws ClassNotFoundException, IOException {
        Class<?> docClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
        Class<?> stripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");
        Object doc = null;
        try {
            try {   // PDFBox 3.x
                Class<?> loader = Class.forName("org.apache.pdfbox.Loader");
                Method load = loader.getMethod("loadPDF", File.class);
                doc = load.invoke(null, f);
            } catch (ClassNotFoundException | NoSuchMethodException e) {   // PDFBox 2.x
                Method load = docClass.getMethod("load", File.class);
                doc = load.invoke(null, f);
            }
            Object stripper = stripperClass.getDeclaredConstructor().newInstance();
            Method getText = stripperClass.getMethod("getText", docClass);
            return (String) getText.invoke(stripper, doc);
        } catch (ReflectiveOperationException e) {
            Throwable c = e.getCause() != null ? e.getCause() : e;
            throw new IOException("PDF could not be read: " + c.getMessage());
        } finally {
            if (doc != null) {
                try { docClass.getMethod("close").invoke(doc); } catch (Exception ignored) { }
            }
        }
    }

    /** A small PDF text extractor (no libraries). Handles common text PDFs. */
    static class BasicPdfText {
        private static final Pattern STREAM = Pattern.compile(
                "obj(.{0,2000}?)stream\\r?\\n", Pattern.DOTALL);

        static String extract(byte[] pdf) {
            String raw = new String(pdf, StandardCharsets.ISO_8859_1);
            StringBuilder out = new StringBuilder();
            int pos = 0;
            while (true) {
                int s = raw.indexOf("stream", pos);
                if (s < 0) break;
                int dataStart = s + 6;
                if (raw.startsWith("\r\n", dataStart)) dataStart += 2;
                else if (raw.startsWith("\n", dataStart)) dataStart += 1;
                int e = raw.indexOf("endstream", dataStart);
                if (e < 0) break;
                int dictStart = Math.max(raw.lastIndexOf("obj", s), 0);
                String dict = raw.substring(dictStart, s);
                pos = e + 9;
                if (dict.contains("/Image") || dict.contains("/XRef") || dict.contains("/FontFile")) continue;
                byte[] data = java.util.Arrays.copyOfRange(pdf, dataStart, e);
                byte[] decoded = decode(data, dict);
                if (decoded == null) continue;
                String content = new String(decoded, StandardCharsets.ISO_8859_1);
                if (content.contains("BT")) out.append(textFromContent(content));
            }
            return out.toString();
        }

        private static byte[] decode(byte[] data, String dict) {
            Matcher m = Pattern.compile("/Filter\\s*(\\[[^\\]]*\\]|/\\w+)").matcher(dict);
            List<String> filters = new ArrayList<>();
            if (m.find()) {
                Matcher fm = Pattern.compile("/(\\w+)").matcher(m.group(1));
                while (fm.find()) filters.add(fm.group(1));
            }
            byte[] cur = data;
            try {
                for (String flt : filters) {
                    switch (flt) {
                        case "FlateDecode": case "Fl": cur = inflate(cur); break;
                        case "ASCII85Decode": case "A85": cur = ascii85(cur); break;
                        case "ASCIIHexDecode": case "AHx": cur = asciiHex(cur); break;
                        default: return null;   // unsupported filter
                    }
                }
            } catch (Exception ex) {
                return null;
            }
            return cur;
        }

        private static byte[] inflate(byte[] in) throws DataFormatException {
            Inflater inf = new Inflater();
            inf.setInput(in);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break;
                bos.write(buf, 0, n);
            }
            inf.end();
            return bos.toByteArray();
        }

        private static byte[] ascii85(byte[] in) {
            String s = new String(in, StandardCharsets.ISO_8859_1);
            int st = s.startsWith("<~") ? 2 : 0;
            int en = s.indexOf("~>");
            if (en < 0) en = s.length();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            long tuple = 0;
            int count = 0;
            for (int i = st; i < en; i++) {
                char c = s.charAt(i);
                if (Character.isWhitespace(c)) continue;
                if (c == 'z' && count == 0) { bos.write(0); bos.write(0); bos.write(0); bos.write(0); continue; }
                tuple = tuple * 85 + (c - '!');
                if (++count == 5) {
                    for (int k = 3; k >= 0; k--) bos.write((int) (tuple >> (8 * k)) & 0xFF);
                    tuple = 0; count = 0;
                }
            }
            if (count > 1) {
                for (int k = count; k < 5; k++) tuple = tuple * 85 + 84;
                for (int k = 3; k > 4 - count; k--) bos.write((int) (tuple >> (8 * k)) & 0xFF);
            }
            return bos.toByteArray();
        }

        private static byte[] asciiHex(byte[] in) {
            StringBuilder h = new StringBuilder();
            for (byte b : in) {
                char c = (char) b;
                if (c == '>') break;
                if (Character.digit(c, 16) >= 0) h.append(c);
            }
            if (h.length() % 2 == 1) h.append('0');
            byte[] out = new byte[h.length() / 2];
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) Integer.parseInt(h.substring(2 * i, 2 * i + 2), 16);
            }
            return out;
        }

        /** Reads text-showing operators (Tj, TJ, ', ") between BT and ET. */
        private static String textFromContent(String c) {
            StringBuilder out = new StringBuilder();
            int i = 0, n = c.length();
            StringBuilder cur = new StringBuilder();
            while (i < n) {
                char ch = c.charAt(i);
                if (ch == '(') {
                    int[] end = new int[1];
                    String str = readLiteral(c, i, end);
                    cur.append(str);
                    i = end[0];
                } else if (ch == '<' && i + 1 < n && c.charAt(i + 1) != '<') {
                    int close = c.indexOf('>', i);
                    if (close < 0) break;
                    String hex = c.substring(i + 1, close).replaceAll("\\s", "");
                    if (hex.length() % 2 == 1) hex += "0";
                    StringBuilder hs = new StringBuilder();
                    for (int k = 0; k + 1 < hex.length(); k += 2) {
                        try { hs.append((char) Integer.parseInt(hex.substring(k, k + 2), 16)); }
                        catch (NumberFormatException ignored) { }
                    }
                    cur.append(hs);
                    i = close + 1;
                } else if (c.startsWith("Tj", i) || c.startsWith("TJ", i) || ch == '\'' || ch == '"') {
                    out.append(cur).append(' ');
                    cur.setLength(0);
                    i += (ch == '\'' || ch == '"') ? 1 : 2;
                } else if (c.startsWith("ET", i) || c.startsWith("T*", i) || c.startsWith("Td", i)
                        || c.startsWith("TD", i)) {
                    if (cur.length() > 0) { out.append(cur); cur.setLength(0); }
                    out.append('\n');
                    i += 2;
                } else if (ch == ']' ) {
                    i++;
                } else {
                    // numbers inside TJ arrays: a large negative kerning means a space
                    if (ch == '-' ) {
                        int j = i + 1;
                        while (j < n && (Character.isDigit(c.charAt(j)) || c.charAt(j) == '.')) j++;
                        try {
                            double v = Double.parseDouble(c.substring(i + 1, j));
                            if (v > 200 && cur.length() > 0) cur.append(' ');
                        } catch (NumberFormatException ignored) { }
                        i = Math.max(j, i + 1);
                    } else {
                        i++;
                    }
                }
            }
            return out.toString();
        }

        private static String readLiteral(String c, int start, int[] end) {
            StringBuilder sb = new StringBuilder();
            int depth = 0, i = start;
            while (i < c.length()) {
                char ch = c.charAt(i);
                if (ch == '\\' && i + 1 < c.length()) {
                    char nx = c.charAt(i + 1);
                    switch (nx) {
                        case 'n': sb.append('\n'); i += 2; break;
                        case 'r': sb.append('\r'); i += 2; break;
                        case 't': sb.append('\t'); i += 2; break;
                        case 'b': case 'f': i += 2; break;
                        case '(': case ')': case '\\': sb.append(nx); i += 2; break;
                        default:
                            if (nx >= '0' && nx <= '7') {
                                int j = i + 1, v = 0, cnt = 0;
                                while (j < c.length() && cnt < 3 && c.charAt(j) >= '0' && c.charAt(j) <= '7') {
                                    v = v * 8 + (c.charAt(j) - '0'); j++; cnt++;
                                }
                                sb.append((char) v);
                                i = j;
                            } else {
                                i += 2;
                            }
                    }
                } else if (ch == '(') {
                    if (depth > 0) sb.append(ch);
                    depth++; i++;
                } else if (ch == ')') {
                    depth--; i++;
                    if (depth == 0) break;
                    sb.append(ch);
                } else {
                    sb.append(ch); i++;
                }
            }
            end[0] = i;
            return sb.toString();
        }
    }
}
