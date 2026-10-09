package com.placement.service;

import com.placement.model.Company;
import com.placement.model.Shortlist;
import com.placement.model.ShortlistCandidate;
import com.placement.model.Skill;
import com.placement.model.SkillMatchingMode;
import com.placement.model.Student;
import com.placement.repository.CompanyRepository;
import com.placement.repository.ShortlistRepository;
import com.placement.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for Shortlist Generation pipeline and Export correctness.
 * Covers cases 11, 12, and 14.
 */
@ExtendWith(MockitoExtension.class)
public class ShortlistAndExportTest {

    @Mock
    private ShortlistRepository shortlistRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private StudentRepository studentRepository;

    @Spy
    private EligibilityService eligibilityService = new EligibilityService();

    @Spy
    private RankingService rankingService = new RankingService();

    @InjectMocks
    private ShortlistService shortlistService;

    private Company testCompany;

    @BeforeEach
    public void setUp() {
        testCompany = new Company("AlphaCorp", "Tech Lead", 8.0, 0, SkillMatchingMode.ALL, "CSE", 2027);
        testCompany.setId(10L);
        testCompany.addRequiredSkill(new Skill("Java"));
    }

    @Test
    @DisplayName("Case 11: Company with no eligible candidates yields 0 eligible and all ineligible")
    public void testCompanyWithNoEligibleCandidates() {
        Student failingStudent = new Student("STU-100", "Low CGPA", "CSE", 6.0, 0, "low@test.com", 2027);

        when(companyRepository.findById(10L)).thenReturn(Optional.of(testCompany));
        when(studentRepository.findAll()).thenReturn(List.of(failingStudent));
        when(shortlistRepository.save(any(Shortlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shortlist response = shortlistService.generateShortlist(10L, 60.0, 30.0, 10.0);

        assertEquals(1, response.getTotalEvaluated());
        assertEquals(0, response.getEligibleCount(), "Eligible count should be 0");
        assertEquals(1, response.getIneligibleCount(), "Ineligible count should be 1");
        assertEquals(1, response.getCandidates().size());
        assertFalse(response.getCandidates().get(0).getEligible());
        assertTrue(response.getCandidates().get(0).getRejectionReasons().contains("CGPA"));
    }

    @Test
    @DisplayName("Case 12: Shortlist generation with empty database handles gracefully")
    public void testDatabaseContainingNoStudents() {
        when(companyRepository.findById(10L)).thenReturn(Optional.of(testCompany));
        when(studentRepository.findAll()).thenReturn(Collections.emptyList());
        when(shortlistRepository.save(any(Shortlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shortlist response = shortlistService.generateShortlist(10L, 60.0, 30.0, 10.0);

        assertEquals(0, response.getTotalEvaluated());
        assertEquals(0, response.getEligibleCount());
        assertEquals(0, response.getIneligibleCount());
        assertTrue(response.getCandidates().isEmpty());
    }

    @Test
    @DisplayName("Case 14: CSV and JSON export output correctness")
    public void testCsvAndJsonExportCorrectness() {
        Student goodStudent = new Student("STU-PASS", "Top Scorer", "CSE", 9.0, 0, "pass@test.com", 2027);
        goodStudent.addSkill(new Skill("Java"));

        Student failStudent = new Student("STU-FAIL", "Failing Student", "CSE", 6.0, 0, "fail@test.com", 2027);

        Shortlist shortlist = new Shortlist();
        shortlist.setId(99L);
        shortlist.setCompanyId(testCompany.getId());
        shortlist.setCompanyName(testCompany.getName());
        shortlist.setJobRole(testCompany.getJobRole());
        shortlist.setTotalEvaluated(2);
        shortlist.setEligibleCount(1);
        shortlist.setIneligibleCount(1);

        ShortlistCandidate c1 = new ShortlistCandidate();
        c1.setId(1L);
        c1.setStudentId(goodStudent.getStudentId());
        c1.setStudentName(goodStudent.getFullName());
        c1.setDepartment(goodStudent.getDepartment());
        c1.setCgpa(goodStudent.getCgpa());
        c1.setActiveBacklogs(0);
        c1.setSkillsSummary("Java");
        c1.setMatchedSkillsSummary("Java");
        c1.setSkillMatchPercentage(100.0);
        c1.setRankingScore(84.0);
        c1.setRankNumber(1);
        c1.setEligible(true);
        c1.setRejectionReasons("");
        shortlist.addCandidate(c1);

        ShortlistCandidate c2 = new ShortlistCandidate();
        c2.setId(2L);
        c2.setStudentId(failStudent.getStudentId());
        c2.setStudentName(failStudent.getFullName());
        c2.setDepartment(failStudent.getDepartment());
        c2.setCgpa(failStudent.getCgpa());
        c2.setActiveBacklogs(0);
        c2.setSkillsSummary("None");
        c2.setMatchedSkillsSummary("None");
        c2.setSkillMatchPercentage(0.0);
        c2.setRankingScore(0.0);
        c2.setRankNumber(null);
        c2.setEligible(false);
        c2.setRejectionReasons("CGPA below 8.0");
        shortlist.addCandidate(c2);

        when(shortlistRepository.findById(99L)).thenReturn(Optional.of(shortlist));

        // Test CSV Export
        StringWriter csvWriter = new StringWriter();
        shortlistService.exportToCsv(99L, csvWriter);
        String csvContent = csvWriter.toString();

        assertNotNull(csvContent);
        assertTrue(csvContent.contains("Rank,Student ID,Full Name,Department,CGPA"));
        assertTrue(csvContent.contains("STU-PASS"));
        assertTrue(csvContent.contains("ELIGIBLE"));
        assertTrue(csvContent.contains("STU-FAIL"));
        assertTrue(csvContent.contains("NOT ELIGIBLE"));
        assertTrue(csvContent.contains("CGPA below 8.0"));

        // Test JSON Export
        String jsonContent = shortlistService.exportToJson(99L);
        assertNotNull(jsonContent);
        assertTrue(jsonContent.contains("\"companyName\" : \"AlphaCorp\""));
        assertTrue(jsonContent.contains("\"studentId\" : \"STU-PASS\""));
        assertTrue(jsonContent.contains("\"studentId\" : \"STU-FAIL\""));
    }
}
