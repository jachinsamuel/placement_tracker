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
 * Handles Student operations: create, read, update, delete, search, filter, and CSV import.
 */
@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final SkillRepository skillRepository;

    public StudentService(StudentRepository studentRepository, SkillRepository skillRepository) {
        this.studentRepository = studentRepository;
        this.skillRepository = skillRepository;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    @Transactional
    public Student createStudent(Student student) {
        // Validation: Unique Student ID
        if (studentRepository.existsByStudentId(student.getStudentId().trim())) {
            throw new IllegalArgumentException("Student ID '" + student.getStudentId() + "' already exists");
        }

        // Validation: Unique Email
        if (studentRepository.existsByEmail(student.getEmail().trim())) {
            throw new IllegalArgumentException("Email address '" + student.getEmail() + "' is already registered");
        }

        // Map skills entered in the form
        if (student.getSkillNames() != null) {
            student.setSkills(parseSkills(student.getSkillNames()));
        }

        return studentRepository.save(student);
    }

    @Transactional
    public Student updateStudent(Long id, Student incomingData) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Student not found with ID: " + id));

        // Check if student ID changed to an existing ID
        if (!student.getStudentId().equalsIgnoreCase(incomingData.getStudentId().trim())
                && studentRepository.existsByStudentId(incomingData.getStudentId().trim())) {
            throw new IllegalArgumentException("Student ID '" + incomingData.getStudentId() + "' is already in use");
        }

        // Check if email changed to an existing email
        if (!student.getEmail().equalsIgnoreCase(incomingData.getEmail().trim())
                && studentRepository.existsByEmail(incomingData.getEmail().trim())) {
            throw new IllegalArgumentException("Email '" + incomingData.getEmail() + "' is already in use");
        }

        student.setStudentId(incomingData.getStudentId().trim());
        student.setFullName(incomingData.getFullName().trim());
        student.setDepartment(incomingData.getDepartment().trim());
        student.setCgpa(incomingData.getCgpa());
        student.setActiveBacklogs(incomingData.getActiveBacklogs());
        student.setEmail(incomingData.getEmail().trim());
        student.setGraduationYear(incomingData.getGraduationYear());
        student.setCertificationsScore(incomingData.getCertificationsScore() != null ? incomingData.getCertificationsScore() : 0.0);

        if (incomingData.getSkillNames() != null) {
            student.setSkills(parseSkills(incomingData.getSkillNames()));
        }

        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new NoSuchElementException("Student not found with ID: " + id);
        }
        studentRepository.deleteById(id);
    }

    public List<Student> searchStudents(String query) {
        if (query == null || query.trim().isEmpty()) {
            return studentRepository.findAll();
        }
        String clean = query.trim();
        return studentRepository.findByFullNameContainingIgnoreCaseOrStudentIdContainingIgnoreCase(clean, clean);
    }

    public List<Student> filterByDepartment(String department) {
        if (department == null || department.trim().isEmpty() || "ALL".equalsIgnoreCase(department.trim())) {
            return studentRepository.findAll();
        }
        return studentRepository.findByDepartmentIgnoreCase(department.trim());
    }

    /**
     * Imports students from a CSV file.
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
                    continue; // Skip existing duplicate
                }

                double cgpa = 0.0;
                try { cgpa = Double.parseDouble(cgpaStr); } catch (Exception ignored) {}

                int backlogs = 0;
                try { backlogs = Integer.parseInt(backlogsStr); } catch (Exception ignored) {}

                int gradYear = 2026;
                try { gradYear = Integer.parseInt(gradYearStr); } catch (Exception ignored) {}

                double certScore = 0.0;
                try { certScore = Double.parseDouble(certScoreStr); } catch (Exception ignored) {}

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
        for (String h : headers) {
            if (record.isMapped(h)) {
                String val = record.get(h);
                if (val != null) return val.trim();
            }
        }
        return "";
    }

    public Set<Skill> parseSkills(String skillsString) {
        Set<Skill> result = new HashSet<>();
        if (skillsString == null || skillsString.trim().isEmpty()) {
            return result;
        }

        String[] parts = skillsString.split("[,;]");
        for (String part : parts) {
            String name = part.trim();
            if (!name.isEmpty()) {
                Skill skill = skillRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> skillRepository.save(new Skill(name)));
                result.add(skill);
            }
        }
        return result;
    }
}
