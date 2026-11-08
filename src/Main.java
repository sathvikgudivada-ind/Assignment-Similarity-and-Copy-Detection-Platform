import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        FileReaderService reader = new FileReaderService();
        SimilarityService service = new SimilarityService();

        System.out.println("==========================================");
        System.out.println(" ASSIGNMENT SIMILARITY & COPY DETECTION");
        System.out.println("==========================================");

        try {
            System.out.print("Enter first assignment file path: ");
            String path1 = scanner.nextLine().trim();

            System.out.print("Enter second assignment file path: ");
            String path2 = scanner.nextLine().trim();

            String assignment1 = reader.readFile(path1);
            String assignment2 = reader.readFile(path2);

            service.compare(assignment1, assignment2);
        } catch (IOException e) {
            System.out.println("File error: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}
