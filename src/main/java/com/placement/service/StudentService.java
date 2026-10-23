package com.placement.service;

import com.placement.model.Skill;
import com.placement.model.Student;
import com.placement.repository.SkillRepository;
import com.placement.repository.StudentRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Module 1: Student Management Service.
 * 
 * Beginner Java Explanation:
 * This class handles standard CRUD operations for students:
 *   - Create (Add new student with validations)
 *   - Read   (Get all students, or find student by ID or search)
 *   - Update (Modify an existing student's academic record or skills)
 *   - Delete (Remove a student from the system)
 *   - CSV Import (Load student spreadsheet records directly)
 */
@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final SkillRepository skillRepository;

    public StudentService(StudentRepository studentRepository, SkillRepository skillRepository) {
        this.studentRepository = studentRepository;
        this.skillRepository = skillRepository;
    }

    /**
     * Get all registered students from database.
     */
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    /**
     * Find a student by database ID.
     */
    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    /**
     * Register a new student with validation.
     */
    @Transactional
    public Student createStudent(Student student) {
        // Validation 1: Student ID cannot be empty and must be unique
        String stuId = student.getStudentId() != null ? student.getStudentId().trim() : "";
        if (stuId.isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be empty");
        }
        if (studentRepository.existsByStudentId(stuId)) {
            throw new IllegalArgumentException("Student ID '" + stuId + "' already exists");
        }

        // Validation 2: Email cannot be empty and must be unique
        String email = student.getEmail() != null ? student.getEmail().trim() : "";
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be empty");
        }
        if (studentRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email address '" + email + "' is already registered");
        }

        // Parse technical skills entered by user (e.g. "Java, Python, SQL")
        if (student.getSkillNames() != null) {
            student.setSkills(parseSkills(student.getSkillNames()));
        }

        return studentRepository.save(student);
    }

    /**
     * Update an existing student's details.
     */
    @Transactional
    public Student updateStudent(Long id, Student incomingData) {
        // Step 1: Check if student exists
        Optional<Student> studentOpt = studentRepository.findById(id);
        if (!studentOpt.isPresent()) {
            throw new NoSuchElementException("Student not found with ID: " + id);
        }
        Student student = studentOpt.get();

        String newStudentId = incomingData.getStudentId().trim();
        String newEmail = incomingData.getEmail().trim();

        // Check if student ID changed to an ID already taken by someone else
        if (!student.getStudentId().equalsIgnoreCase(newStudentId) && studentRepository.existsByStudentId(newStudentId)) {
            throw new IllegalArgumentException("Student ID '" + newStudentId + "' is already in use");
        }

        // Check if email changed to an email already taken by someone else
        if (!student.getEmail().equalsIgnoreCase(newEmail) && studentRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email '" + newEmail + "' is already in use");
        }

        // Step 2: Copy updated values into student entity
        student.setStudentId(newStudentId);
        student.setFullName(incomingData.getFullName().trim());
        student.setDepartment(incomingData.getDepartment().trim());
        student.setCgpa(incomingData.getCgpa());
        student.setActiveBacklogs(incomingData.getActiveBacklogs());
        student.setEmail(newEmail);
        student.setGraduationYear(incomingData.getGraduationYear());

        double certScore = (incomingData.getCertificationsScore() != null) ? incomingData.getCertificationsScore() : 0.0;
        student.setCertificationsScore(certScore);

        if (incomingData.getSkillNames() != null) {
            student.setSkills(parseSkills(incomingData.getSkillNames()));
        }

        return studentRepository.save(student);
    }

    /**
     * Delete a student by ID.
     */
    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new NoSuchElementException("Student not found with ID: " + id);
        }
        studentRepository.deleteById(id);
    }

    /**
     * Search students by name or student ID.
     */
    public List<Student> searchStudents(String query) {
        if (query == null || query.trim().isEmpty()) {
            return studentRepository.findAll();
        }
        String cleanQuery = query.trim();
        return studentRepository.findByFullNameContainingIgnoreCaseOrStudentIdContainingIgnoreCase(cleanQuery, cleanQuery);
    }

    /**
     * Filter students by department (e.g. "CSE", "IT").
     */
    public List<Student> filterByDepartment(String department) {
        if (department == null || department.trim().isEmpty() || "ALL".equalsIgnoreCase(department.trim())) {
            return studentRepository.findAll();
        }
        return studentRepository.findByDepartmentIgnoreCase(department.trim());
    }

    /**
     * Imports students from a CSV spreadsheet.
     */
    @Transactional
    public int importStudentsFromCsv(InputStream inputStream) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            CSVFormat format = CSVFormat.Builder.create()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            CSVParser parser = format.parse(reader);

            for (CSVRecord record : parser) {
                String studentId = getCsvValue(record, "studentId", "id");
                String fullName = getCsvValue(record, "fullName", "name");
                String department = getCsvValue(record, "department", "dept");
                String cgpaStr = getCsvValue(record, "cgpa", "gpa");
                String backlogsStr = getCsvValue(record, "activeBacklogs", "backlogs");
                String email = getCsvValue(record, "email", "mail");
                String gradYearStr = getCsvValue(record, "graduationYear", "year");
                String skillsStr = getCsvValue(record, "skills");
                String certScoreStr = getCsvValue(record, "certificationsScore", "certScore");

                if (studentId.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
                    continue; // Skip invalid row
                }

                if (studentRepository.existsByStudentId(studentId) || studentRepository.existsByEmail(email)) {
                    continue; // Skip duplicate record
                }

                double cgpa = 0.0;
                try {
                    cgpa = Double.parseDouble(cgpaStr);
                } catch (Exception ignored) {}

                int backlogs = 0;
                try {
                    backlogs = Integer.parseInt(backlogsStr);
                } catch (Exception ignored) {}

                int gradYear = 2026;
                try {
                    gradYear = Integer.parseInt(gradYearStr);
                } catch (Exception ignored) {}

                double certScore = 0.0;
                try {
                    certScore = Double.parseDouble(certScoreStr);
                } catch (Exception ignored) {}

                Student student = new Student(studentId, fullName, department, cgpa, backlogs, email, gradYear);
                student.setCertificationsScore(certScore);

                if (!skillsStr.isEmpty()) {
                    student.setSkills(parseSkills(skillsStr));
                }

                studentRepository.save(student);
                count++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading CSV file: " + e.getMessage(), e);
        }
        return count;
    }

    private String getCsvValue(CSVRecord record, String... headers) {
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i];
            if (record.isMapped(h)) {
                String val = record.get(h);
                if (val != null) {
                    return val.trim();
                }
            }
        }
        return "";
    }

    /**
     * Splits comma-separated skills and retrieves or creates Skill entities.
     * 
     * Beginner Java logic:
     * - Splits "Java, SQL, Python" into individual words.
     * - Looks up each skill in database.
     * - If found -> uses existing skill.
     * - If not found -> saves new skill to database.
     */
    public Set<Skill> parseSkills(String skillsString) {
        Set<Skill> result = new HashSet<>();
        if (skillsString == null || skillsString.trim().isEmpty()) {
            return result;
        }

        String[] parts = skillsString.split("[,;]");
        for (int i = 0; i < parts.length; i++) {
            String name = parts[i].trim();
            if (!name.isEmpty()) {
                Optional<Skill> existing = skillRepository.findByNameIgnoreCase(name);
                Skill skill;
                if (existing.isPresent()) {
                    skill = existing.get();
                } else {
                    skill = skillRepository.save(new Skill(name));
                }
                result.add(skill);
            }
        }
        return result;
    }
}
