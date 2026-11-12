import java.sql.SQLException;
import java.util.Scanner;

/**
 * Small console menu to use the database layer on its own
 * (without the similarity code): test connection, create tables,
 * view stored assignments and results.
 */
public class DatabaseMenu {

    public static void main(String[] args) {
        DatabaseManager db = new DatabaseManager();
        Scanner sc = new Scanner(System.in);
        try {
            while (true) {
                System.out.println("\n=== Database Menu ===");
                System.out.println("1. Test database connection");
                System.out.println("2. Create tables");
                System.out.println("3. View stored assignments");
                System.out.println("4. View stored comparison results");
                System.out.println("0. Exit");
                System.out.print("Choice: ");
                String choice = sc.nextLine().trim();
                try {
                    switch (choice) {
                        case "1": db.testConnection(); break;
                        case "2": db.createTables(); System.out.println("Tables are ready."); break;
                        case "3": db.printAllAssignments(); break;
                        case "4": db.printAllResults(); break;
                        case "0": return;
                        default: System.out.println("Invalid choice.");
                    }
                } catch (SQLException e) {
                    System.out.println("Database error: " + e.getMessage());
                }
            }
        } finally {
            sc.close();
        }
    }
}
