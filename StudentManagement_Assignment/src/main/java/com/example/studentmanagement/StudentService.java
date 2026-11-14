package com.example.studentmanagement;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentService {
    private final StudentRepository repository;

    StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Student create(StudentRequest request) {
        return repository.saveAndFlush(new Student(request));
    }

    public List<Student> list() {
        return repository.findAll(Sort.by("id"));
    }

    public Student find(long id) {
        return repository.findById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }

    @Transactional
    public Student update(long id, StudentRequest request) {
        Student student = find(id);
        student.update(request);
        return repository.saveAndFlush(student);
    }

    @Transactional
    public void delete(long id) {
        repository.delete(find(id));
    }

    public List<Student> findByCourse(String course) {
        return repository.findByCourseIgnoreCaseOrderByIdAsc(course.strip());
    }
}
