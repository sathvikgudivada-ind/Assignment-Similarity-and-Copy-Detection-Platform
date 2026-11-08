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

**Cosine Similarity** compares two word

Data Storage: File System (TXT Files)

Version Control: Git

Platform: GitHub

Operating System: Windows / Linux / macOS

## Compile
Run these commands from the project root:

```bash
javac -d out src/*.java
```

## Run
```bash
java -cp out Main
```

When prompted, enter the sample paths:
- `samples/assignment1.txt`
- `samples/assignment2.txt`

On Windows, these relative paths also work when the terminal is opened in the project root.

## Algorithms
- **Jaccard similarity:** compares the sets of unique words in two assignments.
- **Cosine similarity:** compares word-frequency vectors.

## Current limitations
- This version reads UTF-8 plain-text files (`.txt`) only.
- It does not yet detect paraphrasing or meaning-level similarity.
- A similarity score is an indicator for human review, not a finding of plagiarism.

## Planned improvements
- TF-IDF document vectors
- Paragraph and sentence-level comparison
- Java Swing interface
- JDBC and MySQL storage
- PDF and DOCX text extraction
- Exportable comparison reports

## License
See [LICENSE](LICENSE).
