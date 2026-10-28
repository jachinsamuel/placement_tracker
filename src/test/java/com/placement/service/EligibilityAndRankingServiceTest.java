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
 * Unit Tests for Eligibility Rule Engine and Candidate Ranking in PlacementService.
 */
public class EligibilityAndRankingServiceTest {

    private PlacementService placementService;
    private Skill javaSkill;
    private Skill sqlSkill;
    private Skill pythonSkill;
    private Company baseCompany;

    @BeforeEach
    public void setUp() {
        placementService = new PlacementService(null, null, null, null);

        javaSkill = new Skill("Java");
        sqlSkill = new Skill("SQL");
        pythonSkill = new Skill("Python");

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

        PlacementService.EvaluationResult result = placementService.evaluateEligibility(student, baseCompany);

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

        PlacementService.EvaluationResult result = placementService.evaluateEligibility(student, baseCompany);

        assertFalse(result.isEligible(), "Student with low CGPA must not be eligible");
        assertTrue(result.getRejectionReasons().get(0).contains("CGPA"));
    }

    @Test
    @DisplayName("Case 3: Student with too many active backlogs should be rejected")
    public void testStudentWithTooManyBacklogs() {
        Student student = new Student("STU-003", "Rohan", "CSE", 9.0, 1, "rohan@test.com", 2027);
        student.addSkill(javaSkill);
        student.addSkill(sqlSkill);

        PlacementService.EvaluationResult result = placementService.evaluateEligibility(student, baseCompany);

        assertFalse(result.isEligible(), "Student with active backlog must not be eligible");
        assertTrue(result.getRejectionReasonsSummary().contains("Active backlogs"));
    }

    @Test
    @DisplayName("Case 4: Student missing a required skill in ALL mode should be rejected")
    public void testStudentMissingRequiredSkill() {
        Student student = new Student("STU-004", "Kabir", "CSE", 8.0, 0, "kabir@test.com", 2027);
        student.addSkill(javaSkill);

        PlacementService.EvaluationResult result = placementService.evaluateEligibility(student, baseCompany);

        assertFalse(result.isEligible(), "Student missing SQL must be rejected");
        assertTrue(result.getRejectionReasonsSummary().contains("Missing required skills"));
        assertEquals(50.0, result.getSkillMatchPercentage(), 0.01);
    }

    @Test
    @DisplayName("Case 5: Student in ANY mode qualifying with 1 matching skill")
    public void testSkillMatchingAnyModeSuccess() {
        Company anyCompany = new Company("AnyCorp", "Analyst", 6.0, 2, SkillMatchingMode.ANY, "CSE", 2027);
        anyCompany.addRequiredSkill(javaSkill);
        anyCompany.addRequiredSkill(pythonSkill);

        Student student = new Student("STU-005", "Meera", "CSE", 7.0, 0, "meera@test.com", 2027);
        student.addSkill(pythonSkill);

        PlacementService.EvaluationResult result = placementService.evaluateEligibility(student, anyCompany);

        assertTrue(result.isEligible(), "Student with Python must be eligible in ANY mode");
        assertEquals(50.0, result.getSkillMatchPercentage(), 0.01);
    }

    @Test
    @DisplayName("Case 6: Student in ineligible department should be rejected")
    public void testStudentIneligibleDepartment() {
        Student student = new Student("STU-006", "Vikram", "MECH", 8.5, 0, "vikram@test.com", 2027);
        student.addSkill(javaSkill);
        student.addSkill(sqlSkill);

        PlacementService.EvaluationResult result = placementService.evaluateEligibility(student, baseCompany);

        assertFalse(result.isEligible());
        assertTrue(result.getRejectionReasonsSummary().contains("Department 'MECH' is not in eligible departments"));
    }

    @Test
    @DisplayName("Case 7: Student with wrong graduation year should be rejected")
    public void testStudentWrongGraduationYear() {
        Student student = new Student("STU-007", "Neha", "IT", 8.5, 0, "neha@test.com", 2026);
        student.addSkill(javaSkill);
        student.addSkill(sqlSkill);

        PlacementService.EvaluationResult result = placementService.evaluateEligibility(student, baseCompany);

        assertFalse(result.isEligible());
        assertTrue(result.getRejectionReasonsSummary().contains("Graduation year"));
    }

    @Test
    @DisplayName("Case 8: Student failing multiple criteria should record all rejection reasons")
    public void testStudentFailingMultipleCriteria() {
        Student student = new Student("STU-008", "Amit", "CIVIL", 6.0, 2, "amit@test.com", 2025);

        PlacementService.EvaluationResult result = placementService.evaluateEligibility(student, baseCompany);

        assertFalse(result.isEligible());
        assertEquals(5, result.getRejectionReasons().size());
    }

    @Test
    @DisplayName("Case 9: Correct weighted score calculation out of 100")
    public void testRankingScoreCalculation() {
        Student student = new Student("STU-009", "Pooja", "CSE", 8.5, 0, "pooja@test.com", 2027);
        student.setCertificationsScore(80.0);

        double score = placementService.calculateRankingScore(student, 100.0, 60.0, 30.0, 10.0);
        assertEquals(89.0, score, 0.01);
    }

    @Test
    @DisplayName("Case 10: Candidate ranking order and CGPA tie-breaking")
    public void testCandidateRankingAndTieBreaking() {
        Student stu1 = new Student("STU-010", "First", "CSE", 8.0, 0, "s1@test.com", 2027);
        Student stu2 = new Student("STU-011", "Second Higher CGPA", "CSE", 9.0, 0, "s2@test.com", 2027);
        Student stu3 = new Student("STU-012", "Third Lower CGPA", "CSE", 7.0, 0, "s3@test.com", 2027);

        stu1.setCertificationsScore(0.0);
        stu2.setCertificationsScore(0.0);
        stu3.setCertificationsScore(100.0);

        List<PlacementService.RankedCandidate> list = new ArrayList<>();
        list.add(new PlacementService.RankedCandidate(stu1, 100.0));
        list.add(new PlacementService.RankedCandidate(stu2, 70.0));
        list.add(new PlacementService.RankedCandidate(stu3, 94.0));

        List<PlacementService.RankedCandidate> ranked = placementService.rankCandidates(list, 60.0, 30.0, 10.0);

        assertEquals(3, ranked.size());
        assertEquals(1, ranked.get(0).getRank());
        assertEquals(2, ranked.get(1).getRank());
        assertEquals(3, ranked.get(2).getRank());
    }
}
