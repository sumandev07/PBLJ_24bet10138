package com.example.studentmanagement;

class StudentNotFoundException extends RuntimeException {
    StudentNotFoundException(long id) {
        super("Student " + id + " was not found.");
    }
}
