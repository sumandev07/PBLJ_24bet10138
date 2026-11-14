package com.example.studentmanagement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByCourseIgnoreCaseOrderByIdAsc(String course);
}
