src/
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Handles the JDBC connection to MySQL and the storage / retrieval of
 * users, assignments and similarity results.
 *
 * Settings are read in this order:
 *   1. environment variables DB_URL, DB_USER, DB_PASSWORD
 *   2. an optional file named db.properties (keys: db.url, db.user, db.password)
 *   3. default values (local MySQL, user root, empty password)
 */
public class DatabaseManager {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/assignment_similarity"
            + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
            + "&useUnicode=true&characterEncoding=UTF-8";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("db.properties")) {
            props.load(in);
        } catch (IOException e) {
            // file is optional
        }
        this.url = firstNonEmpty(System.getenv("DB_URL"),
                props.getProperty("db.url"), DEFAULT_URL);
        this.user = firstNonEmpty(System.getenv("DB_USER"),
                props.getProperty("db.user"), DEFAULT_USER);
        this.password = firstNonNull(System.getenv("DB_PASSWORD"),
                props.getProperty("db.password"), DEFAULT_PASSWORD);
    }

    public DatabaseManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }

    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null) return v;
        }
        return "";
    }

    // ------------------------------------------------------------------
    // Connection
    // ------------------------------------------------------------------

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public boolean testConnection() {
        try (Connection con = getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            System.out.println("Connected to: " + con.getCatalog());
            System.out.println("Database    : " + meta.getDatabaseProductName()
                    + " " + meta.getDatabaseProductVersion());
            return true;
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Schema
    // ------------------------------------------------------------------

    public void createTables() throws SQLException {
        String users = "CREATE TABLE IF NOT EXISTS users ("
                + "user_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_name VARCHAR(100) NOT NULL, "
                + "email VARCHAR(150) NOT NULL UNIQUE, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        String assignments = "CREATE TABLE IF NOT EXISTS assignments ("
                + "assignment_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT NOT NULL, "
                + "file_name VARCHAR(255) NOT NULL, "
                + "file_content LONGTEXT NOT NULL, "
                + "submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        String results = "CREATE TABLE IF NOT EXISTS similarity_results ("
                + "result_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "assignment1_id INT NOT NULL, "
                + "assignment2_id INT NOT NULL, "
                + "algorithm VARCHAR(20) NOT NULL, "
                + "similarity_percentage DECIMAL(5,2) NOT NULL, "
                + "compared_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (assignment1_id) REFERENCES assignments(assignment_id) ON DELETE CASCADE, "
                + "FOREIGN KEY (assignment2_id) REFERENCES assignments(assignment_id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate(users);
            st.executeUpdate(assignments);
            st.executeUpdate(results);
        }
    }

    // ------------------------------------------------------------------
    // Users
    // ------------------------------------------------------------------

    public int addUser(String name, String email) throws SQLException {
        String sql = "INSERT INTO users (user_name, email) VALUES (?, ?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    public int findOrCreateUser(String name, String email) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE email = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return addUser(name, email);
    }

    // ------------------------------------------------------------------
    // Assignments
    // ------------------------------------------------------------------

    public int saveAssignment(int userId, String fileName, String content) throws SQLException {
        String sql = "INSERT INTO assignments "
                + "(user_id, file_name, file_content) VALUES (?, ?, ?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, fileName);
            ps.setString(3, content);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    public String getAssignmentContent(int assignmentId) throws SQLException {
        String sql = "SELECT file_content FROM assignments WHERE assignment_id = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, assignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    // ------------------------------------------------------------------
    // Similarity results
    // ------------------------------------------------------------------

    public int saveResult(int assignment1Id, int assignment2Id,
                          String algorithm, double percentage) throws SQLException {
        String sql = "INSERT INTO similarity_results "
                + "(assignment1_id, assignment2_id, algorithm, similarity_percentage) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, assignment1Id);
            ps.setInt(2, assignment2Id);
            ps.setString(3, algorithm);
            ps.setDouble(4, percentage);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    // ------------------------------------------------------------------
    // Reports
    // ------------------------------------------------------------------

    public void printAllAssignments() throws SQLException {
        String sql = "SELECT a.assignment_id, u.user_name, a.file_name, a.submitted_at "
                + "FROM assignments a JOIN users u ON a.user_id = u.user_id "
                + "ORDER BY a.assignment_id";
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\n--- Stored Assignments ---");
            System.out.printf("%-5s %-20s %-30s %s%n", "ID", "Student", "File", "Submitted");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%-5d %-20s %-30s %s%n",
                        rs.getInt("assignment_id"), rs.getString("user_name"),
                        rs.getString("file_name"), rs.getTimestamp("submitted_at"));
            }
            if (!any) System.out.println("(no assignments stored)");
        }
    }

    public void printAllResults() throws SQLException {
        String sql = "SELECT r.result_id, a1.file_name AS file1, a2.file_name AS file2, "
                + "r.algorithm, r.similarity_percentage, r.compared_at "
                + "FROM similarity_results r "
                + "JOIN assignments a1 ON r.assignment1_id = a1.assignment_id "
                + "JOIN assignments a2 ON r.assignment2_id = a2.assignment_id "
                + "ORDER BY r.result_id";
        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\n--- Stored Similarity Results ---");
            System.out.printf("%-5s %-22s %-22s %-9s %-9s %s%n",
                    "ID", "File 1", "File 2", "Algorithm", "Score(%)", "Compared");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%-5d %-22s %-22s %-9s %-9.2f %s%n",
                        rs.getInt("result_id"), rs.getString("file1"), rs.getString("file2"),
                        rs.getString("algorithm"), rs.getDouble("similarity_percentage"),
                        rs.getTimestamp("compared_at"));
            }
            if (!any) System.out.println("(no results stored)");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Deletes a user (and, through ON DELETE CASCADE, their assignments and results). */
    public void deleteUser(int userId) throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private int generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) return keys.getInt(1);
            throw new SQLException("No generated key was returned.");
        }
    }
}
