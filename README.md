# 📚 Assignment Similarity and Copy Detection Platform

## 1. 📌 Project Overview

The **Assignment Similarity and Copy Detection Platform** is a Java-based application designed to compare two student assignment text files and identify similarities between them.

The system uses text-processing techniques and similarity algorithms to calculate how closely two assignments match. It helps identify potentially copied content and supports academic integrity.

The application compares assignment files using **Jaccard Similarity** and **Cosine Similarity** algorithms.

## 2. ✨ Features

- **Assignment File Comparison:** Compare two student assignment text files.
- **Jaccard Similarity:** Identify similarities between unique words in assignments.
- **Cosine Similarity:** Compare word-frequency vectors to measure textual similarity.
- **Text Preprocessing:** Process and prepare assignment text before comparison.
- **File Reading:** Read assignment content from UTF-8 text files.
- **Similarity Score:** Calculate similarity scores to evaluate matching content.
- **Console-Based Execution:** Run the application through a terminal or command prompt.

## 3. 🛠️ Technologies Used

- **PROGRAMMING LANGUAGE:** Java
- **IDE:** Visual Studio Code (VS Code)
- **JDK VERSION:** JDK 11 or Later
- **DATA STORAGE:** Local File System (TXT Files)
- **VERSION CONTROL:** Git
- **REPOSITORY HOSTING:** GitHub
- **OPERATING SYSTEM:** Windows / Linux / macOS

## 4. 📂 Project Structure

```text
Assignment-Similarity-and-Copy-Detection-Platform/
│
├── src/
│   ├── Main.java
│   ├── FileReaderService.java
│   ├── TextPreprocessor.java
│   ├── JaccardSimilarity.java
│   ├── CosineSimilarity.java
│   └── SimilarityService.java
│
├── samples/
│   ├── assignment1.txt
│   └── assignment2.txt
│
├── .gitignore
├── LICENSE
└── README.md
```

## 5. ⚙️ Working Methodology

1. **Input:** The user provides two assignment text-file paths.
2. **File Reading:** The system reads both assignment files.
3. **Text Preprocessing:** The content is processed and prepared for comparison.
4. **Jaccard Comparison:** The algorithm calculates similarity based on unique words.
5. **Cosine Comparison:** The algorithm calculates similarity based on word frequencies.
6. **Result Generation:** The system calculates and displays similarity results.

## 6. 🧮 Algorithms Used

### A. Jaccard Similarity

**Jaccard Similarity** compares two sets of unique words to determine how much content they have in common.

**Formula:**

`J(A,B) = |A ∩ B| / |A ∪ B|`

**Where:**

- **A:** Unique words in Assignment 1.
- **B:** Unique words in Assignment 2.
- **Intersection:** Common unique words in both assignments.
- **Union:** All unique words present in both assignments.

### B. Cosine Similarity

**Cosine Similarity** compares two word-frequency vectors to measure the similarity between assignment documents.

**Formula:**

`Cosine Similarity = (A · B) / (||A|| × ||B||)`

It measures how similar the word-frequency patterns of two assignments are.

## 7. 💻 System Requirements

### Hardware Requirements

- **Processor:** Intel or AMD processor.
- **RAM:** Minimum 4 GB recommended.
- **Storage:** Sufficient disk space for Java and project files.

### Software Requirements

- **Java:** JDK 11 or later.
- **Operating System:** Windows / Linux / macOS.
- **IDE:** Visual Studio Code / IntelliJ IDEA / Eclipse.
- **Terminal:** Command Prompt or VS Code Terminal.
- **Git:** Required for cloning the repository.

## 8. 🚀 Installation and Execution

### Step 1: Clone the Repository

```bash
git clone https://github.com/sathvikgudivada-ind/Assignment-Similarity-and-Copy-Detection-Platform.git
```

### Step 2: Navigate to the Project Directory

```bash
cd Assignment-Similarity-and-Copy-Detection-Platform
```

### Step 3: Compile the Java Files

```bash
javac -d out src/*.java
```

### Step 4: Execute the Application

```bash
java -cp out Main
```

### Step 5: Provide Input Files

When prompted, enter the paths of the two assignment files:

```text
samples/assignment1.txt
samples/assignment2.txt
```

The application processes the files and displays the similarity results.

## 9. 💾 Data Storage

The application currently uses **plain-text files (.txt)** as input.

- **File Format:** UTF-8 text files.
- **Input Storage:** Assignment files are stored locally.
- **Processing:** Assignment content is read and processed during execution.
- **Database:** No database is currently required.
- **Output:** Similarity results are displayed in the console.

## 10. 🏗️ Object-Oriented Programming (OOP) Concepts

The project follows a modular Java class structure.

- **Encapsulation:** Organizes data and related functionality within classes.
- **Abstraction:** Separates file reading, text preprocessing and similarity calculations into dedicated components.
- **Modularity:** Divides the application into independent classes, making the code easier to understand and maintain.

*Note: OOP concepts should be verified against the actual source-code implementation.*

## 11. ⚠️ Current Limitations

- Supports UTF-8 plain-text (.txt) files only.
- Does not currently detect paraphrasing or meaning-level similarity.
- Similarity scores are indicators for human review and are not definitive proof of plagiarism.

## 12. 🔮 Future Enhancements

- **PDF and DOCX Support:** Enable comparison of different document formats.
- **TF-IDF Algorithm:** Improve document similarity analysis.
- **Sentence-Level Comparison:** Identify similarities between individual sentences.
- **Graphical User Interface:** Develop an interactive interface using Java Swing.
- **Database Integration:** Integrate MySQL using JDBC.
- **Report Generation:** Generate downloadable similarity reports.

## 13. 👥 Project Team

**PROJECT NAME:** Assignment Similarity and Copy Detection Platform

**TEAM MEMBERS:**

- **Member 1:** [Name] – [Roll Number]
- **Member 2:** [Name] – [Roll Number]
- **Member 3:** [Name] – [Roll Number]

**DEPARTMENT:** [Department Name]

**COLLEGE:** [College Name]

## 14. 🔗 GitHub Repository

**Source Code:** [Assignment Similarity and Copy Detection Platform](https://github.com/sathvikgudivada-ind/Assignment-Similarity-and-Copy-Detection-Platform)

## 15. 📝 Conclusion

The **Assignment Similarity and Copy Detection Platform** provides an automated approach to identifying textual similarities between student assignments. By implementing Jaccard Similarity and Cosine Similarity algorithms, the application helps analyze assignment content and supports academic integrity.

The project demonstrates the practical application of Java programming, file handling, text processing, algorithms and object-oriented programming concepts.
