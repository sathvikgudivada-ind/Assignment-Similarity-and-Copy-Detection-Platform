-- Optional: run manually if you prefer creating the schema in MySQL yourself.
-- (DatabaseManager.createTables() creates the same tables automatically.)

CREATE DATABASE IF NOT EXISTS assignment_similarity
  DEFAULT CHARACTER SET utf8mb4;
USE assignment_similarity;

CREATE TABLE IF NOT EXISTS users (
  user_id    INT AUTO_INCREMENT PRIMARY KEY,
  user_name  VARCHAR(100) NOT NULL,
  email      VARCHAR(150) NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assignments (
  assignment_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id       INT NOT NULL,
  file_name     VARCHAR(255) NOT NULL,
  file_content  LONGTEXT NOT NULL,
  submitted_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS similarity_results (
  result_id             INT AUTO_INCREMENT PRIMARY KEY,
  assignment1_id        INT NOT NULL,
  assignment2_id        INT NOT NULL,
  algorithm             VARCHAR(20) NOT NULL,   -- 'Jaccard' or 'Cosine'
  similarity_percentage DECIMAL(5,2) NOT NULL,
  compared_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (assignment1_id) REFERENCES assignments(assignment_id) ON DELETE CASCADE,
  FOREIGN KEY (assignment2_id) REFERENCES assignments(assignment_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
