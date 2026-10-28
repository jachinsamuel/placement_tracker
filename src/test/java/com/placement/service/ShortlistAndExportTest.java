package com.placement.service;

import com.placement.model.*;
import com.placement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for Shortlist Generation pipeline and Export correctness in PlacementService.
 */
@ExtendWith(MockitoExtension.class)
public class ShortlistAndExportTest {

    @Mock
    private ShortlistRepository shortlistRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private PlacementService placementService;

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

        Shortlist response = placementService.generateShortlist(10L, 60.0, 30.0, 10.0);

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

        Shortlist response = placementService.generateShortlist(10L, 60.0, 30.0, 10.0);

        assertEquals(0, response.getTotalEvaluated());
        assertEquals(0, response.getEligibleCount());
        assertEquals(0, response.getIneligibleCount());
        assertTrue(response.getCandidates().isEmpty());
    }

    @Test
    @DisplayName("Case 14: CSV and JSON export output correctness")
    public void testCsvAndJsonExportCorrectness() {
        Student passingStudent = new Student("STU-200", "Top Scorer", "CSE", 9.5, 0, "top@test.com", 2027);
        passingStudent.addSkill(new Skill("Java"));

        when(companyRepository.findById(10L)).thenReturn(Optional.of(testCompany));
        when(studentRepository.findAll()).thenReturn(List.of(passingStudent));
        when(shortlistRepository.save(any(Shortlist.class))).thenAnswer(invocation -> {
            Shortlist s = invocation.getArgument(0);
            s.setId(100L);
            return s;
        });

        Shortlist generated = placementService.generateShortlist(10L, 60.0, 30.0, 10.0);
        when(shortlistRepository.findById(100L)).thenReturn(Optional.of(generated));

        StringWriter writer = new StringWriter();
        placementService.exportToCsv(100L, writer);
        String csvOutput = writer.toString();

        assertTrue(csvOutput.contains("STU-200"));
        assertTrue(csvOutput.contains("Top Scorer"));
        assertTrue(csvOutput.contains("ELIGIBLE"));

        String jsonOutput = placementService.exportToJson(100L);
        assertTrue(jsonOutput.contains("Top Scorer"));
        assertTrue(jsonOutput.contains("AlphaCorp"));
    }
}
