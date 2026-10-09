package com.placement.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a student registered for campus placement.
 * Simple entity with basic attributes, validation, and technical skills.
 */
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String studentId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private Double cgpa;

    @Column(nullable = false)
    private Integer activeBacklogs;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Integer graduationYear;

    /**
     * Score for achievements or certifications (0 to 100), used in ranking.
     */
    @Column(nullable = false)
    private Double certificationsScore = 0.0;

    /**
     * Temporary holder for skills when receiving JSON or forms from the frontend.
     */
    @Transient
    private String skillNames;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "student_skills",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new HashSet<>();

    // Default constructor for JPA
    public Student() {
    }

    // Parameterized constructor
    public Student(String studentId, String fullName, String department, Double cgpa,
                   Integer activeBacklogs, String email, Integer graduationYear) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.department = department;
        this.cgpa = cgpa;
        this.activeBacklogs = activeBacklogs;
        this.email = email;
        this.graduationYear = graduationYear;
        this.certificationsScore = 0.0;
    }

    // Helper method to add a skill
    public void addSkill(Skill skill) {
        if (skill != null) {
            this.skills.add(skill);
        }
    }

    // Helper method to format skills as simple readable text
    public String getSkillsSummary() {
        if (skills == null || skills.isEmpty()) {
            return "None";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Skill s : skills) {
            if (i > 0) sb.append(", ");
            sb.append(s.getName());
            i++;
        }
        return sb.toString();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Double getCgpa() {
        return cgpa;
    }

    public void setCgpa(Double cgpa) {
        this.cgpa = cgpa;
    }

    public Integer getActiveBacklogs() {
        return activeBacklogs;
    }

    public void setActiveBacklogs(Integer activeBacklogs) {
        this.activeBacklogs = activeBacklogs;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public Double getCertificationsScore() {
        return certificationsScore != null ? certificationsScore : 0.0;
    }

    public void setCertificationsScore(Double certificationsScore) {
        this.certificationsScore = certificationsScore != null ? certificationsScore : 0.0;
    }

    public String getSkillNames() {
        if (skillNames != null) return skillNames;
        return getSkillsSummary();
    }

    public void setSkillNames(String skillNames) {
        this.skillNames = skillNames;
    }

    public Set<Skill> getSkills() {
        return skills;
    }

    public void setSkills(Set<Skill> skills) {
        this.skills = skills != null ? skills : new HashSet<>();
    }
}
