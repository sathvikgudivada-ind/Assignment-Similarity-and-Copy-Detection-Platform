# JDBC Database Module

Database layer for the **AI-Based Assignment Similarity and Copy Detection Platform**
(SRM University-AP, CSE-E). It stores users, assignment files and similarity results
in MySQL using JDBC.

## Contents

| File | Purpose |
| --- | --- |
| `src/DatabaseManager.java` | JDBC connection, table creation, save/retrieve users, assignments, results |
| `src/DatabaseTest.java` | Self-test: insert, read back, compare, delete; prints PASS/FAIL |
| `src/DatabaseMenu.java` | Small console menu to test the connection and view stored data |
| `sql/schema.sql` | Optional manual schema (same as `createTables()`) |
| `db.properties.example` | Template for connection settings |
| `lib/` | Put the MySQL connector JAR here |

## Database: `assignment_similarity`

- `users` (user_id, user_name, email UNIQUE, created_at)
- `assignments` (assignment_id, user_id FK, file_name, file_content, submitted_at)
- `similarity_results` (result_id, assignment1_id FK, assignment2_id FK, algorithm, similarity_percentage, compared_at)

One comparison creates two result rows: one `Jaccard`, one `Cosine`.

## Setup

1. Install and start MySQL.
2. Download `mysql-connector-j` and put the JAR in `lib/`.
3. Copy `db.properties.example` to `db.properties` and set your user/password
   (or set the environment variables `DB_URL`, `DB_USER`, `DB_PASSWORD`).

Settings are read in this order: environment variables, `db.properties`, defaults
(local MySQL, user `root`).

## Compile and run

Windows:

```
javac -d out src/*.java
java -cp "out;lib/*" DatabaseTest
java -cp "out;lib/*" DatabaseMenu
```

Linux / macOS (use `:` instead of `;`):

```
javac -d out src/*.java
java -cp "out:lib/*" DatabaseTest
java -cp "out:lib/*" DatabaseMenu
```

## Using it with the main project

Copy `DatabaseManager.java` into the `src` folder of the main project (it has no
dependency on other classes), keep the connector JAR in `lib/`, and call:

```java
DatabaseManager db = new DatabaseManager();
db.createTables();
int u1 = db.findOrCreateUser("Name", "name@example.com");
int a1 = db.saveAssignment(u1, "assignment1.txt", text1);
int a2 = db.saveAssignment(u1, "assignment2.txt", text2);
db.saveResult(a1, a2, "Jaccard", jaccardPercent);
db.saveResult(a1, a2, "Cosine", cosinePercent);
```

All queries use `PreparedStatement` (no SQL injection) and try-with-resources
(connections close automatically).
