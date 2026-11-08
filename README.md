# Assignment Similarity and Copy Detection Platform

Assignment Similarity and Copy Detection System is a Java application that serves the purpose of comparing two assignment text files and finding similarities in those files.

The system employs text processing algorithms to calculate the similarity percentage between two assignments. It can be used for detecting copied content from one assignment to another.

This comparison will be done by employing two different algorithms Jaccard Similarity and Cosine Similarity.
## Requirements
- JDK 11 or later
- Terminal or an IDE such as IntelliJ IDEA, Eclipse, or VS Code with Java support

## Project structure
```text
src/
  Main.java
  FileReaderService.java
  TextPreprocessor.java
  JaccardSimilarity.java
  CosineSimilarity.java
  SimilarityService.java
samples/
  assignment1.txt
  assignment2.txt
```

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
