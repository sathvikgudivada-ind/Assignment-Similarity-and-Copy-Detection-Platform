# Assignment Similarity and Copy Detection Platform

A Java application with a modern **web interface**. It runs on your own computer, opens in your browser,
and compares student assignments using **Jaccard**, **Cosine** and **Code structure** similarity.
Results can be saved to **MySQL** through **JDBC**.

> The scores show textual overlap only. They are not proof of plagiarism; the instructor
> makes the final decision.

## Features
- **Upload or paste:** drag and drop files, choose a whole folder, or paste text / code straight into the page
- **Many formats:** `.txt`, source code (`.java .py .c .cpp .js ...`), `.docx`, `.pptx`, `.xlsx`, `.odt`, `.rtf`, `.html`, `.pdf`
- **Three scores:** Jaccard (shared unique words), Cosine (word frequency) and **Code structure**
  (variable and method names, spacing and comments are ignored, so copied logic with renamed variables scores 100%)
- **Ranking table** of every pair, highest first, plus a colour **similarity map** for many documents
- **Side-by-side view** with the matching sentences highlighted, and a list of identical sentences as evidence
- **Download CSV** of all results, light / dark theme
- **MySQL:** optional "Save results" tick box and a Database panel (test connection, create tables, view records)

Not supported: old `.doc/.ppt/.xls` (use Save As `.docx/.pptx/.xlsx`) and scanned PDFs (no text to read).

## Run (Windows)
1. Install the Java JDK (8 or higher).
2. Put `mysql-connector-j-xx.jar` in the `lib` folder (see `lib/README.txt`) - only needed for MySQL.
3. Double-click **run.bat**. Keep the black window open; the page opens in your browser
   (or open http://localhost:8080/ yourself).

Linux / macOS: `./run.sh`.  Older desktop-window version: `run-desktop.bat`.

## MySQL settings
Copy `db.properties.example` to `db.properties` and enter your MySQL user and password.
Do not upload `db.properties` to GitHub.

## Files
| File | Purpose |
| --- | --- |
| `web/index.html` | the web page (HTML, CSS and JavaScript - edit here to change the design) |
| `src/SimilarityWeb.java` | local web server and API |
| `src/SimilarityEngine.java` | preprocessing, Jaccard, Cosine, Code structure, matching sentences |
| `src/DocumentExtractor.java` | reads text from all supported formats |
| `src/DatabaseManager.java` | JDBC connection and database operations |
| `src/SimilarityApp.java` | older desktop-window version |

## Privacy
The server listens only on your own computer (127.0.0.1). Files are not sent to the internet.
