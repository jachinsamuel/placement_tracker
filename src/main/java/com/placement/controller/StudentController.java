package com.placement.controller;

import com.placement.model.Student;
import com.placement.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * REST Controller for Student Management operations.
 * 
 * Beginner Java Explanation:
 * A Controller connects user actions in the browser to Java methods.
 * Each method handles an HTTP route:
 *   - GET    /api/students          -> Get all students
 *   - GET    /api/students/{id}     -> Get student by ID
 *   - POST   /api/students          -> Add new student
 *   - PUT    /api/students/{id}     -> Update student details
 *   - DELETE /api/students/{id}     -> Remove student
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public List<Student> getAllStudents(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return studentService.searchStudents(search);
        }
        if (department != null && !department.trim().isEmpty()) {
            return studentService.filterByDepartment(department);
        }
        return studentService.getAllStudents();
    }

    @GetMapping("/{id}")
    public Student getStudentById(@PathVariable Long id) {
        Optional<Student> studentOpt = studentService.getStudentById(id);
        if (!studentOpt.isPresent()) {
            throw new NoSuchElementException("Student not found with ID: " + id);
        }
        return studentOpt.get();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Student createStudent(@RequestBody Student student) {
        return studentService.createStudent(student);
    }

    @PutMapping("/{id}")
    public Student updateStudent(@PathVariable Long id, @RequestBody Student student) {
        return studentService.updateStudent(id, student);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Student deleted successfully");
        return response;
    }

    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }

        try {
            int count = studentService.importStudentsFromCsv(file.getInputStream());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Import completed successfully");
            response.put("importedCount", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to import CSV: " + e.getMessage(), e);
        }
    }
}
