# AI-Based Assignment Similarity and Copy Detection Platform

## Project Overview
This project is a Java-based console application that compares student assignment text files and calculates how similar they are. It uses Jaccard similarity and Cosine similarity, and can store users, assignments and results in a MySQL database using JDBC.

The scores show textual overlap only. They are not proof of plagiarism, and the final decision stays with the instructor.

Developed at SRM University-AP, Department of CSE (CSE-E), under the guidance of Dr. Tirthankar Sengupta.

## Features
- Compares two assignment text files
- Text preprocessing: lowercase conversion, symbol removal, tokenization
- Jaccard similarity on sets of unique words
- Cosine similarity on word-frequency vectors
- Similarity scores shown as percentages
- Stores users, assignments and similarity results in MySQL through JDBC
- Menu to view stored assignments and results
- Error handling for missing, unreadable or empty files
- Database self-test that prints PASS or FAIL

## Technologies Used
- Java
- JDBC (mysql-connector-j)
- MySQL
- Object-Oriented Programming
- Text preprocessing, Jaccard similarity, Cosine similarity

## Project Structure
```
src/
  Main.java                 console entry point
  FileReaderService.java    checks and reads files
  TextPreprocessor.java     cleans text and creates tokens
  JaccardSimilarity.java    Jaccard similarity
  CosineSimilarity.java     Cosine similarity
  SimilarityService.java    coordinates the comparison
  DatabaseManager.java      JDBC connection and database operations
  DatabaseTest.java         database self-test
  DatabaseMenu.java         console menu for the database
samples/                    sample assignment files
schema.sql                  MySQL table definitions
db.properties.example       database settings template
```

## Requirements
- Java JDK 8 or higher
- MySQL Server (needed only for the database features)
- MySQL Connector/J JAR file placed in a `lib` folder

## Installation and Execution
1. Clone or download this repository.
2. For database features, start MySQL and copy `db.properties.example` to `db.properties`, then enter your MySQL user name and password. Do not upload `db.properties` to GitHub.
3. Put the MySQL connector JAR in a `lib` folder.

Compile and run (Windows):
```
javac -d out src/*.java
java -cp "out;lib/*" Main
java -cp "out;lib/*" DatabaseTest
```
On Linux or macOS use `:` instead of `;` in the classpath.

Sample output:
```
Jaccard similarity    : 85.71%
Cosine similarity     : 94.05%
```

## Data Storage
The data is stored in a MySQL database named `assignment_similarity` with three tables:
- `users` - user_id, user_name, email, created_at
- `assignments` - assignment_id, user_id, file_name, file_content, submitted_at
- `similarity_results` - result_id, assignment1_id, assignment2_id, algorithm, similarity_percentage, compared_at

Each comparison stores two result rows, one for Jaccard and one for Cosine. All database operations use prepared statements.

## Object-Oriented Programming Concepts
- **Encapsulation:** each class keeps its data and methods together (for example, DatabaseManager keeps the connection details private).
- **Abstraction:** the user starts a comparison without needing to know how the similarity is calculated internally.
- **Modularity:** separate classes handle file reading, preprocessing, similarity calculation and database access.
- **Exception handling:** try-catch and try-with-resources handle file and database errors.

## Project Team
| Name | Roll Number |
| --- | --- |
| Shaik Nadeem | AP25110010598 |
| Pavan Santhosh | AP25110011006 |
| T. Prasad | AP25110010960 |
| G. Sathvik | AP25110010728 |
| B. Vamsidar Reddy | AP25110011024 |
| K. Joshan Kumar | AP25110011077 |

## GitHub Repository
https://github.com/sathvikgudivada-ind/Assignment-Similarity-and-Copy-Detection-Platform
