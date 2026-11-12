# AI-Based Assignment Similarity and Copy Detection Platform

A Java-based console application that compares student assignment text files and calculates how similar they are using **Jaccard similarity** and **Cosine similarity**. Results can be stored in a **MySQL** database using **JDBC**.

B.Tech CSE project, SRM University-AP (CSE-E), guided by Dr. Tirthankar Sengupta.

> The scores show textual overlap only. They are not proof of plagiarism, and the final decision stays with the instructor.

## How it works
1. Read two assignment text files (UTF-8)
2. Preprocess: lowercase, remove symbols and extra spaces, split into words
3. Build a set of unique words (Jaccard) and a word-frequency vector (Cosine)
4. Calculate both similarity percentages and display them
5. Optionally save users, assignments and results to MySQL through JDBC

## Project structure
- `src/Main.java` - console entry point
- `src/FileReaderService.java` - file checking and reading
- `src/TextPreprocessor.java` - text cleaning and tokenization
- `src/JaccardSimilarity.java` - Jaccard similarity
- `src/CosineSimilarity.java` - Cosine similarity
- `src/SimilarityService.java` - coordinates the comparison
- `src/DatabaseManager.java` - JDBC connection and database operations
- `src/DatabaseTest.java` - database self-test (PASS/FAIL)
- `src/DatabaseMenu.java` - console menu for the database
- `samples/` - sample assignment files
- `schema.sql` - MySQL table definitions
- `db.properties.example` - database settings template

## Database (MySQL, JDBC)
Database name: `assignment_similarity`, with three tables: `users`, `assignments`, `similarity_results`.

1. Install and start MySQL
2. Download `mysql-connector-j` and put the JAR in a `lib` folder
3. Copy `db.properties.example` to `db.properties` and enter your MySQL user and password (do not upload `db.properties` to GitHub)

## Compile and run
Windows:

    javac -d out src/*.java
    java -cp "out;lib/*" Main
    java -cp "out;lib/*" DatabaseTest

Linux / macOS: use `:` instead of `;` in the classpath.

## Sample output
    Jaccard similarity    : 85.71%
    Cosine similarity     : 94.05%

## Limitations
- Compares words, not meaning; heavy paraphrasing may not be detected
- Plain-text (.txt) input and console interface only
- English text only

## Future work
TF-IDF and stop-word removal, sentence embeddings, PDF/Word support, web interface, highlighting matching text.

## Team
T. Prasad, Shaik Nadeem, Pavan Santhosh, G. Sathvik, B. Vamsidar Reddy, K. Joshan Kumar
