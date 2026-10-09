package com.placement.service;

import com.placement.model.Company;
import com.placement.model.Skill;
import com.placement.model.SkillMatchingMode;
import com.placement.model.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for Eligibility Rule Engine (Module 3) and Candidate Ranking (Module 4).
 * Covers cases 1 through 10, plus validation edge cases.
 */
public class EligibilityAndRankingServiceTest {

    private EligibilityService eligibilityService;
    private RankingService rankingService;

    private Skill javaSkill;
    private Skill sqlSkill;
    private Skill pythonSkill;

    private Company baseCompany;

    @BeforeEach
    public void setUp() {
        eligibilityService = new EligibilityService();
        rankingService = new RankingService();

        javaSkill = new Skill("Java");
        sqlSkill = new Skill("SQL");
        pythonSkill = new Skill("Python");

        // Example standard company: Min CGPA 7.5, Max Backlogs 0, Skills: Java + SQL (ALL), Depts: CSE, IT, Year: 2027
        baseCompany = new Company(
                "Example Technologies",
                "Software Engineer",
                7.5,
                0,
                SkillMatchingMode.ALL,
                "CSE, IT",
                2027
        );
        baseCompany.addRequiredSkill(javaSkill);
        baseCompany.addRequiredSkill(sqlSkill);
    }

    @Test
    @DisplayName("Case 1: Student who satisfies all criteria should be eligible")
    public void testStudentSatisfiesAllCriteria() {
        Student student = new Student("STU-001", "Aarav", "CSE", 8.5, 0, "aarav@test.com", 2027);
        student.addSkill(javaSkill);
        student.addSkill(sqlSkill);

        EligibilityService.EvaluationResult result = eligibilityService.evaluate(student, baseCompany);

        assertTrue(result.isEligible(), "Student should be eligible");
        assertTrue(result.getRejectionReasons().isEmpty(), "Rejection reasons should be empty");
        assertEquals(100.0, result.getSkillMatchPercentage(), 0.01);
    }

    @Test
    @DisplayName("Case 2: Student whose CGPA is below the minimum should be rejected")
    public void testStudentWithCgpaBelowMinimum() {
        Student student = new Student("STU-002", "Ananya", "CSE", 7.2, 0, "ananya@test.com", 2027);
        student.addSkill(javaSkill);
        student.addSkill(sqlSkill);

        EligibilityService.EvaluationResult result = eligibilityService.evaluate(student, baseCompany);

        assertFalse(result.isEligible(), "Student with low CGPA must not be eligible");
        assertTrue(result.getRejectionReasons().get(0).contains("CGPA"));
    }

    @Test
    @DisplayName("Case 3: Student with too many active backlogs should be rejected")
    public void testStudentWithTooManyBacklogs() {
        Student student = new Student("STU-003", "Rohan", "CSE", 9.0, 1, "rohan@test.com", 2027);
        student.addSkill(javaSkill);
        student.addSkill(sqlSkill);

        EligibilityService.EvaluationResult result = eligibilityService.evaluate(student, baseCompany);

        assertFalse(result.isEligible(), "Student with active backlog must not be eligible");
        assertTrue(result.getRejectionReasonsSummary().contains("Active backlogs"));
    }

    @Test
    @DisplayName("Case 4: Student missing a required skill in ALL mode should be rejected")
    public void testStudentMissingRequiredSkill() {
        Student student = new Student("STU-004", "Kabir", "CSE", 8.0, 0, "kabir@test.com", 2027);
        student.addSkill(javaSkill); // Missing SQL

        EligibilityService.EvaluationResult result = eligibilityService.evaluate(student, baseCompany);

        assertFalse(result.isEligible(), "Student missing SQL must be rejected in ALL mode");
        assertTrue(result.getRejectionReasonsSummary().contains("Missing required skills"));
        assertEquals(50.0, result.getSkillMatchPercentage(), 0.01);
    }

    @Test
    @DisplayName("Case 5: Student from an ineligible department should be rejected")
    public void testStudentFromIneligibleDepartment() {
        Student student = new Student("STU-005", "Vikram", "ECE", 8.2, 0, "vikram@test.com", 2027);
        student.addSkill(javaSkill);
        student.addSkill(sqlSkill);

        EligibilityService.EvaluationResult result = eligibilityService.evaluate(student, baseCompany);

        assertFalse(result.isEligible(), "ECE student must be rejected for CSE/IT company");
        assertTrue(result.getRejectionReasonsSummary().contains("Department 'ECE' is not in eligible departments"));
    }

    @Test
    @DisplayName("Case 6: Student from an ineligible graduation year should be rejected")
    public void testStudentFromIneligibleGraduationYear() {
        Student student = new Student("STU-006", "Sneha", "CSE", 8.5, 0, "sneha@test.com", 2026);
        student.addSkill(javaSkill);
        student.addSkill(sqlSkill);

        EligibilityService.EvaluationResult result = eligibilityService.evaluate(student, baseCompany);

        assertFalse(result.isEligible(), "2026 batch student must be rejected when 2027 is required");
        assertTrue(result.getRejectionReasonsSummary().contains("Graduation year (2026) does not match"));
    }

    @Test
    @DisplayName("Case 7: ANY skill matching mode allows candidate with at least one skill")
    public void testAnySkillMatchingMode() {
        Company anyCompany = new Company("CloudCorp", "DevOps", 7.0, 1, SkillMatchingMode.ANY, "CSE", 2027);
        anyCompany.addRequiredSkill(javaSkill);
        anyCompany.addRequiredSkill(pythonSkill);

        // Student has only Python
        Student student = new Student("STU-007", "Dev", "CSE", 7.5, 0, "dev@test.com", 2027);
        student.addSkill(pythonSkill);

        EligibilityService.EvaluationResult result = eligibilityService.evaluate(student, anyCompany);

        assertTrue(result.isEligible(), "Candidate with one of the required skills should pass in ANY mode");
        assertEquals(50.0, result.getSkillMatchPercentage(), 0.01);
    }

    @Test
    @DisplayName("Case 8: ALL skill matching mode rejects candidate possessing only subset")
    public void testAllSkillMatchingMode() {
        Company allCompany = new Company("StrictCorp", "SDE", 7.0, 0, SkillMatchingMode.ALL, "CSE", 2027);
        allCompany.addRequiredSkill(javaSkill);
        allCompany.addRequiredSkill(pythonSkill);

        // Student has only Java
        Student student = new Student("STU-008", "Raj", "CSE", 8.0, 0, "raj@test.com", 2027);
        student.addSkill(javaSkill);

        EligibilityService.EvaluationResult result = eligibilityService.evaluate(student, allCompany);

        assertFalse(result.isEligible(), "Candidate with only 1 of 2 skills must fail in ALL mode");
    }

    @Test
    @DisplayName("Case 9: Student whose CGPA exactly equals minimum required CGPA is eligible")
    public void testCgpaExactlyAtThreshold() {
        Student student = new Student("STU-009", "BoundaryUser", "CSE", 7.5, 0, "bound@test.com", 2027);
        student.addSkill(javaSkill);
        student.addSkill(sqlSkill);

        EligibilityService.EvaluationResult result = eligibilityService.evaluate(student, baseCompany);

        assertTrue(result.isEligible(), "CGPA 7.5 meeting min CGPA 7.5 must be eligible");
    }

    @Test
    @DisplayName("Case 10: Two candidates with equal ranking scores tie-break by higher CGPA")
    public void testTieBreakingByCgpa() {
        // Candidate 1: CGPA 8.5, Skills 100%, Certs 70%
        // Score = (85 * 0.60) + (100 * 0.30) + (70 * 0.10) = 51.0 + 30.0 + 7.0 = 88.0
        Student s1 = new Student("STU-A", "Candidate A", "CSE", 8.5, 0, "a@test.com", 2027);
        s1.setCertificationsScore(70.0);

        // Candidate 2: CGPA 8.0, Skills 100%, Certs 100%
        // Score = (80 * 0.60) + (100 * 0.30) + (100 * 0.10) = 48.0 + 30.0 + 10.0 = 88.0
        Student s2 = new Student("STU-B", "Candidate B", "CSE", 8.0, 0, "b@test.com", 2027);
        s2.setCertificationsScore(100.0);

        double score1 = rankingService.calculateScore(s1, 100.0, 60.0, 30.0, 10.0);
        double score2 = rankingService.calculateScore(s2, 100.0, 60.0, 30.0, 10.0);

        assertEquals(score1, score2, "Scores must be equal to test tie-breaker");

        List<RankingService.RankedCandidate> list = new ArrayList<>();
        list.add(new RankingService.RankedCandidate(s2, 100.0));
        list.add(new RankingService.RankedCandidate(s1, 100.0));

        List<RankingService.RankedCandidate> ranked = rankingService.rankCandidates(list, 60.0, 30.0, 10.0);

        assertEquals(1, ranked.get(0).getRank());
        assertEquals("STU-A", ranked.get(0).getStudent().getStudentId(),
                "Candidate A with higher CGPA (8.5 vs 8.0) must be ranked higher on tie");
        assertEquals(2, ranked.get(1).getRank());
        assertEquals("STU-B", ranked.get(1).getStudent().getStudentId());
    }

    @Test
    @DisplayName("Case 13: Invalid CGPA values and negative backlogs are detected")
    public void testInvalidInputValidations() {
        Student invalidCgpaStudent = new Student("STU-X", "Invalid", "CSE", -1.0, -2, "x@test.com", 2027);
        EligibilityService.EvaluationResult result = eligibilityService.evaluate(invalidCgpaStudent, baseCompany);

        assertFalse(result.isEligible());
        assertTrue(result.getRejectionReasonsSummary().contains("CGPA"));
        assertTrue(result.getRejectionReasonsSummary().contains("Active backlogs"));
    }
}
