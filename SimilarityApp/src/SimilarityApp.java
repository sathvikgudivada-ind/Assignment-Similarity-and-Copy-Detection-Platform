import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * AI-Based Assignment Similarity and Copy Detection Platform - desktop application.
 *
 * Run with no arguments to open the window.
 * Command line:  java SimilarityApp fileA fileB
 *                java SimilarityApp --folder folderPath
 */
public class SimilarityApp extends JFrame {

    private static final String NOTE = "Scores show textual overlap only. They are not proof of plagiarism; "
            + "the instructor makes the final decision.";

    private final DatabaseManager db = new DatabaseManager();

    // tab 1
    private final JTextField fileA = new JTextField();
    private final JTextField fileB = new JTextField();
    private final JTextField nameA = new JTextField(), emailA = new JTextField();
    private final JTextField nameB = new JTextField(), emailB = new JTextField();
    private final JCheckBox saveBox = new JCheckBox("Save result to MySQL database");
    private final JLabel jaccardValue = bigLabel(), cosineValue = bigLabel();
    private final JLabel structureValue = bigLabel();
    private final JProgressBar jaccardBar = bar(), cosineBar = bar(), structureBar = bar();
    private final JLabel levelLabel = new JLabel(" ");
    private final JLabel statsLabel = new JLabel(" ");
    private final JTextArea evidence = new JTextArea();
    private final JLabel status = new JLabel("Ready");

    // tab 2
    private final JTextField folderField = new JTextField();
    private final JCheckBox folderSave = new JCheckBox("Save all results to MySQL database");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"File A", "File B", "Jaccard %", "Cosine %", "Code structure %", "Level"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) { return (c == 2 || c == 3) ? Double.class : String.class; }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextArea folderEvidence = new JTextArea();
    private final List<SimilarityEngine.Result> folderResults = new ArrayList<>();
    private final JLabel folderStatus = new JLabel("Choose a folder with the assignments to compare.");

    // tab 3
    private final JTextArea dbOutput = new JTextArea();

    public SimilarityApp() {
        super("Assignment Similarity and Copy Detection");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Compare two files", buildTwoFileTab());
        tabs.addTab("Compare a folder", buildFolderTab());
        tabs.addTab("Database", buildDatabaseTab());
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        JLabel foot = new JLabel("  " + NOTE);
        foot.setFont(foot.getFont().deriveFont(Font.ITALIC, 11f));
        foot.setBorder(new EmptyBorder(4, 8, 6, 8));
        root.add(foot, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(980, 760);
        setMinimumSize(new Dimension(820, 640));
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    // UI pieces
    // ------------------------------------------------------------------

    private JComponent buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(0x1F3A5F));
        p.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel t = new JLabel("Assignment Similarity & Copy Detection");
        t.setForeground(Color.WHITE);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 20f));
        JLabel s = new JLabel("Compares .txt, code, .docx, .pptx, .xlsx, .odt, .rtf, .html and .pdf  |  "
                + "Jaccard + Cosine similarity  |  MySQL via JDBC");
        s.setForeground(new Color(0xC9D6E8));
        s.setFont(s.getFont().deriveFont(12f));
        JPanel col = new JPanel(new GridLayout(2, 1));
        col.setOpaque(false);
        col.add(t);
        col.add(s);
        p.add(col, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildTwoFileTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel inputs = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 3, 3, 3);
        g.fill = GridBagConstraints.HORIZONTAL;

        enableDrop(fileA, nameA, emailA);
        enableDrop(fileB, nameB, emailB);
        addFileRow(inputs, g, 0, "File A", fileA, e -> choose(fileA, nameA));
        addFileRow(inputs, g, 1, "File B", fileB, e -> choose(fileB, nameB));

        JPanel dbFields = new JPanel(new GridLayout(2, 4, 6, 4));
        dbFields.add(new JLabel("Student A name"));  dbFields.add(nameA);
        dbFields.add(new JLabel("Student A email")); dbFields.add(emailA);
        dbFields.add(new JLabel("Student B name"));  dbFields.add(nameB);
        dbFields.add(new JLabel("Student B email")); dbFields.add(emailB);
        emailA.setToolTipText("Optional. If empty, a placeholder address is used.");
        emailB.setToolTipText("Optional. If empty, a placeholder address is used.");
        setDbFieldsEnabled(false);
        saveBox.addActionListener(e -> setDbFieldsEnabled(saveBox.isSelected()));

        JButton compare = new JButton("Compare");
        compare.setFont(compare.getFont().deriveFont(Font.BOLD, 14f));
        compare.addActionListener(e -> runTwoFileCompare());
        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> clearTwoFile());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        actions.add(compare);
        actions.add(clear);
        actions.add(saveBox);

        g.gridx = 0; g.gridy = 2; g.gridwidth = 3; g.weightx = 1;
        inputs.add(dbFields, g);
        g.gridy = 3;
        inputs.add(actions, g);

        JPanel results = new JPanel(new GridLayout(1, 3, 12, 0));
        results.add(scoreCard("Jaccard", "shared unique words", jaccardValue, jaccardBar));
        results.add(scoreCard("Cosine", "word frequency", cosineValue, cosineBar));
        results.add(scoreCard("Code structure", "names ignored, code only", structureValue, structureBar));
        structureValue.setFont(structureValue.getFont().deriveFont(Font.BOLD, 30f));
        jaccardValue.setFont(jaccardValue.getFont().deriveFont(Font.BOLD, 30f));
        cosineValue.setFont(cosineValue.getFont().deriveFont(Font.BOLD, 30f));

        levelLabel.setFont(levelLabel.getFont().deriveFont(Font.BOLD, 14f));
        JPanel info = new JPanel(new GridLayout(2, 1));
        info.add(levelLabel);
        info.add(statsLabel);

        JPanel mid = new JPanel(new BorderLayout(0, 6));
        mid.add(results, BorderLayout.NORTH);
        mid.add(info, BorderLayout.CENTER);

        evidence.setEditable(false);
        evidence.setLineWrap(true);
        evidence.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(evidence);
        sp.setBorder(BorderFactory.createTitledBorder("Matching sentences (evidence for the reviewer)"));

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.add(inputs, BorderLayout.NORTH);
        top.add(mid, BorderLayout.CENTER);

        p.add(top, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        p.add(status, BorderLayout.SOUTH);
        return p;
    }

    private JComponent buildFolderTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 3, 3, 3);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.weightx = 0; top.add(new JLabel("Folder"), g);
        g.gridx = 1; g.weightx = 1; top.add(folderField, g);
        JButton browse = new JButton("Browse...");
        browse.addActionListener(e -> chooseFolder());
        g.gridx = 2; g.weightx = 0; top.add(browse, g);

        JButton go = new JButton("Compare all files");
        go.setFont(go.getFont().deriveFont(Font.BOLD, 14f));
        go.addActionListener(e -> runFolderCompare());
        JPanel act = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        act.add(go);
        act.add(folderSave);
        g.gridx = 0; g.gridy = 1; g.gridwidth = 3; top.add(act, g);

        table.setAutoCreateRowSorter(true);
        table.setRowHeight(24);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showFolderEvidence();
        });
        table.setDefaultRenderer(Double.class, (tbl, val, sel, foc, r, c) -> {
            JLabel l = new JLabel(String.format("%.2f", (Double) val), SwingConstants.RIGHT);
            l.setOpaque(true);
            double d = (Double) val;
            Color bg = d > 80 ? new Color(0xF8C9C9) : d > 60 ? new Color(0xFBE3C2) : d > 40 ? new Color(0xFFF4C2) : Color.WHITE;
            l.setBackground(sel ? tbl.getSelectionBackground() : bg);
            l.setBorder(new EmptyBorder(0, 4, 0, 8));
            return l;
        });
        folderEvidence.setEditable(false);
        folderEvidence.setLineWrap(true);
        folderEvidence.setWrapStyleWord(true);
        JScrollPane evid = new JScrollPane(folderEvidence);
        evid.setBorder(BorderFactory.createTitledBorder("Matching sentences for the selected pair"));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), evid);
        split.setResizeWeight(0.6);

        p.add(top, BorderLayout.NORTH);
        p.add(split, BorderLayout.CENTER);
        p.add(folderStatus, BorderLayout.SOUTH);
        return p;
    }

    private JComponent buildDatabaseTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(12, 12, 12, 12));
        JButton test = new JButton("Test connection");
        JButton create = new JButton("Create tables");
        JButton assigns = new JButton("View stored assignments");
        JButton results = new JButton("View stored results");
        test.addActionListener(e -> dbAction(() -> { db.testConnection(); return null; }));
        create.addActionListener(e -> dbAction(() -> { db.createTables(); System.out.println("Tables are ready."); return null; }));
        assigns.addActionListener(e -> dbAction(() -> { db.printAllAssignments(); return null; }));
        results.addActionListener(e -> dbAction(() -> { db.printAllResults(); return null; }));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.add(test); bar.add(create); bar.add(assigns); bar.add(results);
        dbOutput.setEditable(false);
        dbOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        dbOutput.setText("MySQL settings are read from the environment variables DB_URL, DB_USER, DB_PASSWORD,\n"
                + "or from the file db.properties in the folder you run the program from.\n");
        p.add(bar, BorderLayout.NORTH);
        p.add(new JScrollPane(dbOutput), BorderLayout.CENTER);
        return p;
    }

    private static JLabel bigLabel() {
        JLabel l = new JLabel("-", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 34f));
        return l;
    }

    private static JProgressBar bar() {
        JProgressBar b = new JProgressBar(0, 100);
        b.setStringPainted(false);
        b.setPreferredSize(new Dimension(100, 12));
        return b;
    }

    private static JPanel scoreCard(String title, String sub, JLabel value, JProgressBar bar) {
        JPanel c = new JPanel(new BorderLayout(0, 4));
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title + "  (" + sub + ")"), new EmptyBorder(4, 10, 8, 10)));
        c.add(value, BorderLayout.CENTER);
        c.add(bar, BorderLayout.SOUTH);
        return c;
    }

    private void addFileRow(JPanel p, GridBagConstraints g, int row, String label, JTextField f,
                            java.awt.event.ActionListener browse) {
        g.gridy = row; g.gridwidth = 1;
        g.gridx = 0; g.weightx = 0; p.add(new JLabel(label), g);
        g.gridx = 1; g.weightx = 1; p.add(f, g);
        JButton b = new JButton("Browse...");
        b.addActionListener(browse);
        g.gridx = 2; g.weightx = 0; p.add(b, g);
        f.setToolTipText("Type a path, click Browse, or drag a file here");
    }

    private void setDbFieldsEnabled(boolean on) {
        for (JTextField f : Arrays.asList(nameA, emailA, nameB, emailB)) f.setEnabled(on);
    }

    private void enableDrop(JTextField field, JTextField nameField, JTextField emailField) {
        field.setTransferHandler(new TransferHandler() {
            @Override public boolean canImport(TransferSupport s) { return s.isDataFlavorSupported(DataFlavor.javaFileListFlavor); }
            @Override @SuppressWarnings("unchecked") public boolean importData(TransferSupport s) {
                try {
                    List<File> files = (List<File>) s.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        field.setText(files.get(0).getAbsolutePath());
                        if (nameField.getText().trim().isEmpty()) nameField.setText(stem(files.get(0).getName()));
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    private void choose(JTextField field, JTextField nameField) {
        JFileChooser fc = new JFileChooser(lastDir(field));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            field.setText(fc.getSelectedFile().getAbsolutePath());
            if (nameField.getText().trim().isEmpty()) nameField.setText(stem(fc.getSelectedFile().getName()));
        }
    }

    private void chooseFolder() {
        JFileChooser fc = new JFileChooser(lastDir(folderField));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            folderField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private File lastDir(JTextField f) {
        String t = f.getText().trim();
        if (!t.isEmpty()) {
            File x = new File(t);
            return x.isDirectory() ? x : x.getParentFile();
        }
        return null;
    }

    private static String stem(String fileName) {
        int i = fileName.lastIndexOf('.');
        return i > 0 ? fileName.substring(0, i) : fileName;
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void clearTwoFile() {
        for (JTextField f : Arrays.asList(fileA, fileB, nameA, emailA, nameB, emailB)) f.setText("");
        jaccardValue.setText("-"); cosineValue.setText("-"); structureValue.setText("-");
        jaccardBar.setValue(0); cosineBar.setValue(0); structureBar.setValue(0);
        levelLabel.setText(" "); statsLabel.setText(" "); evidence.setText("");
        status.setText("Ready");
    }

    private static String cleanPath(String s) {
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
        if (s.startsWith("file:///")) {
            try { s = new File(new java.net.URI(s.replace(" ", "%20"))).getPath(); } catch (Exception ignored) { }
        }
        return s;
    }

    private void runTwoFileCompare() {
        final String pa = cleanPath(fileA.getText()), pb = cleanPath(fileB.getText());
        if (pa.isEmpty() || pb.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please choose both files.", "Missing file", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final boolean save = saveBox.isSelected();
        final String[] who = {nameA.getText().trim(), emailA.getText().trim(), nameB.getText().trim(), emailB.getText().trim()};
        status.setText("Comparing...");
        new SwingWorker<SimilarityEngine.Result, Void>() {
            String saveMessage = "";
            @Override protected SimilarityEngine.Result doInBackground() throws Exception {
                SimilarityEngine.Doc a = load(new File(pa));
                SimilarityEngine.Doc b = load(new File(pb));
                SimilarityEngine.Result r = SimilarityEngine.compare(a, b);
                if (save) {
                    db.createTables();
                    int ua = db.findOrCreateUser(orDefault(who[0], stem(a.name)), emailOrDefault(who[1], orDefault(who[0], stem(a.name))));
                    int ub = db.findOrCreateUser(orDefault(who[2], stem(b.name)), emailOrDefault(who[3], orDefault(who[2], stem(b.name))));
                    int ida = db.saveAssignment(ua, a.name, a.text);
                    int idb = db.saveAssignment(ub, b.name, b.text);
                    db.saveResult(ida, idb, "Jaccard", round2(r.jaccard));
                    db.saveResult(ida, idb, "Cosine", round2(r.cosine));
                    if (r.structure >= 0) db.saveResult(ida, idb, "CodeStructure", round2(r.structure));
                    saveMessage = "  |  Saved to database.";
                }
                return r;
            }
            @Override protected void done() {
                try {
                    show(get());
                    status.setText("Done." + saveMessage);
                } catch (Exception ex) {
                    status.setText("Error");
                    error(ex);
                }
            }
        }.execute();
    }

    private void show(SimilarityEngine.Result r) {
        jaccardValue.setText(String.format("%.2f%%", r.jaccard));
        cosineValue.setText(String.format("%.2f%%", r.cosine));
        jaccardBar.setValue((int) Math.round(r.jaccard));
        cosineBar.setValue((int) Math.round(r.cosine));
        if (r.structure >= 0) {
            structureValue.setText(String.format("%.2f%%", r.structure));
            structureBar.setValue((int) Math.round(r.structure));
            structureBar.setForeground(colorFor(r.structure));
        } else {
            structureValue.setText("n/a");
            structureBar.setValue(0);
        }
        Color c = colorFor(Math.max(Math.max(r.jaccard, r.cosine), r.structure));
        jaccardBar.setForeground(colorFor(r.jaccard));
        cosineBar.setForeground(colorFor(r.cosine));
        levelLabel.setText(r.level());
        levelLabel.setForeground(c.darker());
        statsLabel.setText(String.format("File A: %d words   |   File B: %d words   |   shared unique words: %d",
                r.wordsA, r.wordsB, r.sharedWords));
        evidence.setText(evidenceText(r));
        evidence.setCaretPosition(0);
    }

    private static String evidenceText(SimilarityEngine.Result r) {
        if (r.commonSentences.isEmpty()) {
            return "No identical sentences were found (sentences of 6 or more words).\n"
                    + "The score comes from shared words and word frequency.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(r.commonSentences.size()).append(" identical sentence(s) found in both files:\n\n");
        int n = 0;
        for (String s : r.commonSentences) {
            sb.append(++n).append(". ").append(s).append("\n\n");
            if (n >= 100) { sb.append("... (first 100 shown)"); break; }
        }
        return sb.toString();
    }

    private static Color colorFor(double s) {
        if (s > 80) return new Color(0xC62828);
        if (s > 60) return new Color(0xEF6C00);
        if (s > 40) return new Color(0xF9A825);
        return new Color(0x2E7D32);
    }

    private void runFolderCompare() {
        final String path = cleanPath(folderField.getText());
        if (path.isEmpty() || !new File(path).isDirectory()) {
            JOptionPane.showMessageDialog(this, "Please choose a folder.", "Missing folder", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final boolean save = folderSave.isSelected();
        tableModel.setRowCount(0);
        folderResults.clear();
        folderEvidence.setText("");
        folderStatus.setText("Reading files...");
        new SwingWorker<List<SimilarityEngine.Result>, String>() {
            final List<String> skipped = new ArrayList<>();
            String saveMessage = "";
            @Override protected List<SimilarityEngine.Result> doInBackground() throws Exception {
                List<SimilarityEngine.Doc> docs = new ArrayList<>();
                File[] files = new File(path).listFiles(f -> f.isFile() && DocumentExtractor.isSupported(f));
                if (files == null) files = new File[0];
                Arrays.sort(files);
                for (File f : files) {
                    publish("Reading " + f.getName() + " ...");
                    try {
                        SimilarityEngine.Doc d = load(f);
                        docs.add(d);
                    } catch (Exception ex) {
                        skipped.add(f.getName() + " (" + ex.getMessage() + ")");
                    }
                }
                List<SimilarityEngine.Result> out = new ArrayList<>();
                int[] ids = new int[docs.size()];
                if (save && !docs.isEmpty()) {
                    db.createTables();
                    for (int i = 0; i < docs.size(); i++) {
                        SimilarityEngine.Doc d = docs.get(i);
                        String nm = stem(d.name);
                        int u = db.findOrCreateUser(nm, emailOrDefault("", nm));
                        ids[i] = db.saveAssignment(u, d.name, d.text);
                    }
                }
                for (int i = 0; i < docs.size(); i++) {
                    for (int j = i + 1; j < docs.size(); j++) {
                        SimilarityEngine.Result r = SimilarityEngine.compare(docs.get(i), docs.get(j));
                        out.add(r);
                        if (save) {
                            db.saveResult(ids[i], ids[j], "Jaccard", round2(r.jaccard));
                            db.saveResult(ids[i], ids[j], "Cosine", round2(r.cosine));
                            if (r.structure >= 0) db.saveResult(ids[i], ids[j], "CodeStructure", round2(r.structure));
                        }
                    }
                    publish("Comparing " + (i + 1) + " of " + docs.size() + " ...");
                }
                if (save) saveMessage = "  |  Saved to database.";
                out.sort(Comparator.comparingDouble((SimilarityEngine.Result r) -> Math.max(Math.max(r.jaccard, r.cosine), r.structure)).reversed());
                folderCount = docs.size();
                return out;
            }
            int folderCount;
            @Override protected void process(List<String> chunks) { folderStatus.setText(chunks.get(chunks.size() - 1)); }
            @Override protected void done() {
                try {
                    List<SimilarityEngine.Result> res = get();
                    folderResults.addAll(res);
                    for (SimilarityEngine.Result r : res) {
                        tableModel.addRow(new Object[]{r.nameA, r.nameB, round2(r.jaccard), round2(r.cosine),
                                r.structure >= 0 ? String.format("%.2f", r.structure) : "-", r.level()});
                    }
                    String msg = folderCount + " files read, " + res.size() + " pairs compared (highest first)." + saveMessage;
                    if (!skipped.isEmpty()) msg += "  Skipped: " + skipped.size() + " (" + skipped.get(0) + ")";
                    folderStatus.setText(msg);
                    if (!skipped.isEmpty()) folderEvidence.setText("Skipped files:\n" + String.join("\n", skipped));
                    if (folderCount < 2) folderStatus.setText("Need at least 2 readable files in the folder. " + msg);
                } catch (Exception ex) {
                    folderStatus.setText("Error");
                    error(ex);
                }
            }
        }.execute();
    }

    private void showFolderEvidence() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int model = table.convertRowIndexToModel(row);
        if (model < folderResults.size()) folderEvidence.setText(evidenceText(folderResults.get(model)));
        folderEvidence.setCaretPosition(0);
    }

    private void dbAction(Callable<Void> action) {
        dbOutput.setText("Working...\n");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                PrintStream old = System.out;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    System.setOut(new PrintStream(bos, true, "UTF-8"));
                    action.call();
                } catch (Exception e) {
                    System.out.println("Database error: " + e.getMessage());
                } finally {
                    System.setOut(old);
                }
                try { return bos.toString("UTF-8"); } catch (Exception e) { return bos.toString(); }
            }
            @Override protected void done() {
                try { dbOutput.setText(get()); } catch (Exception e) { dbOutput.setText(e.toString()); }
                dbOutput.setCaretPosition(0);
            }
        }.execute();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static SimilarityEngine.Doc load(File f) throws Exception {
        String text = DocumentExtractor.extract(f);
        SimilarityEngine.Doc d = new SimilarityEngine.Doc(f.getName(), text);
        if (d.tokens.isEmpty()) throw new Exception("no readable words in " + f.getName());
        return d;
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private static String orDefault(String v, String def) { return v == null || v.isEmpty() ? def : v; }

    private static String emailOrDefault(String email, String name) {
        if (email != null && !email.trim().isEmpty()) return email.trim();
        return name.toLowerCase().replaceAll("[^a-z0-9]+", ".").replaceAll("^\\.|\\.$", "") + "@unknown.local";
    }

    private void error(Exception ex) {
        Throwable t = ex.getCause() != null ? ex.getCause() : ex;
        String m = t.getMessage() == null ? t.toString() : t.getMessage();
        JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------------
    // Entry point (window or command line)
    // ------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && !args[0].equals("--folder")) {
            cliCompare(args[0], args[1]);
            return;
        }
        if (args.length == 2) {
            cliFolder(args[1]);
            return;
        }
        try {
            for (UIManager.LookAndFeelInfo i : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(i.getName())) { UIManager.setLookAndFeel(i.getClassName()); break; }
            }
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new SimilarityApp().setVisible(true));
    }

    private static void cliCompare(String a, String b) throws Exception {
        SimilarityEngine.Result r = SimilarityEngine.compare(load(new File(a)), load(new File(b)));
        System.out.printf("Words           : %d and %d (shared unique: %d)%n", r.wordsA, r.wordsB, r.sharedWords);
        System.out.printf("Jaccard similarity : %.2f%%%n", r.jaccard);
        System.out.printf("Cosine similarity  : %.2f%%%n", r.cosine);
        if (r.structure >= 0) System.out.printf("Code structure     : %.2f%%%n", r.structure);
        System.out.println("Level              : " + r.level());
        System.out.println("Identical sentences: " + r.commonSentences.size());
        System.out.println(NOTE);
    }

    private static void cliFolder(String dir) throws Exception {
        File[] files = new File(dir).listFiles(f -> f.isFile() && DocumentExtractor.isSupported(f));
        if (files == null) { System.out.println("Not a folder: " + dir); return; }
        Arrays.sort(files);
        List<SimilarityEngine.Doc> docs = new ArrayList<>();
        for (File f : files) {
            try { docs.add(load(f)); } catch (Exception e) { System.out.println("Skipped " + f.getName() + ": " + e.getMessage()); }
        }
        for (int i = 0; i < docs.size(); i++) {
            for (int j = i + 1; j < docs.size(); j++) {
                SimilarityEngine.Result r = SimilarityEngine.compare(docs.get(i), docs.get(j));
                System.out.printf("%-22s %-22s Jaccard %6.2f%%  Cosine %6.2f%%  Structure %s%n", r.nameA, r.nameB, r.jaccard, r.cosine,
                        r.structure >= 0 ? String.format("%.2f%%", r.structure) : "n/a");
            }
        }
    }
}
