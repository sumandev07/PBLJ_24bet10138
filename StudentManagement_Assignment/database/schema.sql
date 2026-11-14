CREATE DATABASE IF NOT EXISTS student_management
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE student_management;

CREATE TABLE IF NOT EXISTS students (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    course VARCHAR(120) NOT NULL,
    age INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_students_email UNIQUE (email),
    CONSTRAINT chk_students_age CHECK (age BETWEEN 1 AND 150)
);
