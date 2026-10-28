-- =====================================================================
-- Automated Library Management System
-- Database Initialization Script (MySQL 8.4 Compatible)
-- =====================================================================

CREATE DATABASE IF NOT EXISTS librarydb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE librarydb;

-- 1. Books Table
CREATE TABLE IF NOT EXISTS books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    total_copies INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_books_isbn (isbn),
    INDEX idx_books_category (category),
    INDEX idx_books_author (author)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Members Table
CREATE TABLE IF NOT EXISTS members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30),
    address VARCHAR(255),
    membership_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_members_code (member_code),
    INDEX idx_members_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Book Issues / Loans Table
CREATE TABLE IF NOT EXISTS book_issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ISSUED',
    late_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_issue_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE RESTRICT,
    CONSTRAINT fk_issue_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE RESTRICT,
    INDEX idx_issues_status (status),
    INDEX idx_issues_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- Repeatable Sample Data Insertion
-- =====================================================================

INSERT INTO books (isbn, title, author, category, total_copies, available_copies, description, status)
VALUES
('978-0134685991', 'Effective Java', 'Joshua Bloch', 'Programming', 5, 4, 'The definitive guide to Java best practices and idioms.', 'AVAILABLE'),
('978-0132350884', 'Clean Code', 'Robert C. Martin', 'Software Engineering', 4, 3, 'A handbook of agile software craftsmanship.', 'AVAILABLE'),
('978-1491950357', 'Designing Data-Intensive Applications', 'Martin Kleppmann', 'Architecture', 3, 3, 'The big ideas behind reliable, scalable, and maintainable systems.', 'AVAILABLE'),
('978-0201633610', 'Design Patterns: Elements of Reusable Object-Oriented Software', 'Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides', 'Design', 2, 1, 'Classic GoF design patterns in object-oriented software.', 'AVAILABLE'),
('978-1617294945', 'Spring in Action, Sixth Edition', 'Craig Walls', 'Frameworks', 4, 4, 'Comprehensive and hands-on guide to Spring and Spring Boot.', 'AVAILABLE')
ON DUPLICATE KEY UPDATE title=VALUES(title);

INSERT INTO members (member_code, name, email, phone, address, membership_date, status)
VALUES
('LIB-MEM-001', 'Alice Johnson', 'alice.johnson@example.com', '+1-555-0101', '123 Maple Street, Springfield', '2025-01-15', 'ACTIVE'),
('LIB-MEM-002', 'Brian Miller', 'brian.miller@example.com', '+1-555-0102', '456 Oak Avenue, Metropolis', '2025-02-20', 'ACTIVE'),
('LIB-MEM-003', 'Clara Vance', 'clara.vance@example.com', '+1-555-0103', '789 Pine Road, Gotham', '2025-03-10', 'ACTIVE'),
('LIB-MEM-004', 'David Kumar', 'david.kumar@example.com', '+1-555-0104', '321 Elm Lane, Star City', '2025-04-05', 'ACTIVE')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Seed a sample loan if not already present
INSERT INTO book_issues (book_id, member_id, issue_date, due_date, return_date, status, late_fee, notes)
SELECT b.id, m.id, CURDATE() - INTERVAL 10 DAY, CURDATE() + INTERVAL 4 DAY, NULL, 'ISSUED', 0.00, 'Semester reading assignment'
FROM books b, members m
WHERE b.isbn = '978-0134685991' AND m.member_code = 'LIB-MEM-001'
  AND NOT EXISTS (
    SELECT 1 FROM book_issues bi WHERE bi.book_id = b.id AND bi.member_id = m.id AND bi.status = 'ISSUED'
  )
LIMIT 1;

-- Seed an overdue sample loan for testing calculation
INSERT INTO book_issues (book_id, member_id, issue_date, due_date, return_date, status, late_fee, notes)
SELECT b.id, m.id, CURDATE() - INTERVAL 20 DAY, CURDATE() - INTERVAL 6 DAY, NULL, 'OVERDUE', 12.00, 'Past due book loan for testing'
FROM books b, members m
WHERE b.isbn = '978-0201633610' AND m.member_code = 'LIB-MEM-002'
  AND NOT EXISTS (
    SELECT 1 FROM book_issues bi WHERE bi.book_id = b.id AND bi.member_id = m.id AND bi.status = 'OVERDUE'
  )
LIMIT 1;
