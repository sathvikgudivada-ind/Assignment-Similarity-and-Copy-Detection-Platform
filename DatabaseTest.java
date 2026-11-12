import java.sql.SQLException;

/**
 * Self-test for the JDBC layer. Connects to MySQL, creates the tables,
 * inserts a user, two assignments and two results, reads the text back,
 * compares it with what was inserted, then deletes the test rows.
 */
public class DatabaseTest {

    private static int failures = 0;

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS" : "FAIL") + " - " + name);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        DatabaseManager db = new DatabaseManager();
        int userId = -1;

        boolean connected = db.testConnection();
        check("Connect to MySQL", connected);
        if (!connected) {
            System.out.println("\nCannot continue. Check that MySQL is running and the "
                    + "credentials in db.properties (or DB_* variables) are correct.");
            System.exit(1);
        }

        try {
            db.createTables();
            check("Create tables", true);

            String email = "dbtest_" + System.currentTimeMillis() + "@example.com";
            userId = db.addUser("DB Test User", email);
            check("Insert user", userId > 0);
            check("findOrCreateUser returns the same user", db.findOrCreateUser("x", email) == userId);

            String text1 = "Machine learning is a branch of artificial intelligence.";
            String text2 = "Machine learning is a field of artificial intelligence. é తెలుగు";
            int a1 = db.saveAssignment(userId, "test1.txt", text1);
            int a2 = db.saveAssignment(userId, "test2.txt", text2);
            check("Insert two assignments", a1 > 0 && a2 > 0 && a1 != a2);

            check("Read assignment 1 back (text matches)", text1.equals(db.getAssignmentContent(a1)));
            check("Read assignment 2 back (text matches, incl. Unicode)", text2.equals(db.getAssignmentContent(a2)));

            int r1 = db.saveResult(a1, a2, "Jaccard", 85.71);
            int r2 = db.saveResult(a1, a2, "Cosine", 94.05);
            check("Insert two similarity results", r1 > 0 && r2 > 0);

            db.printAllAssignments();
            db.printAllResults();
        } catch (SQLException e) {
            check("Unexpected SQL error: " + e.getMessage(), false);
        } finally {
            if (userId > 0) {
                try {
                    db.deleteUser(userId);   // cascades to assignments and results
                    check("Delete test rows", true);
                } catch (SQLException e) {
                    check("Delete test rows: " + e.getMessage(), false);
                }
            }
        }

        System.out.println(failures == 0 ? "\nALL CHECKS PASSED" : "\n" + failures + " CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
