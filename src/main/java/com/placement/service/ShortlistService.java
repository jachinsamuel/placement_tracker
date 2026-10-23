package com.placement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.placement.model.Company;
import com.placement.model.Shortlist;
import com.placement.model.ShortlistCandidate;
import com.placement.model.Student;
import com.placement.repository.CompanyRepository;
import com.placement.repository.ShortlistRepository;
import com.placement.repository.StudentRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Writer;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Module 5: Shortlist Generation and Export Service.
 * 
 * Beginner Java Explanation:
 * This service coordinates the full shortlisting process for a company:
 *   Step 1: Fetch the company from database.
 *   Step 2: Fetch all registered students.
 *   Step 3: Evaluate each student using EligibilityService (separate into Eligible vs Ineligible).
 *   Step 4: Rank all eligible students using RankingService (assign #1, #2, #3 based on weighted score).
 *   Step 5: Save the complete shortlist session to database.
 *   Step 6: Provide one-click CSV and JSON exports for download.
 */
@Service
public class ShortlistService {

    private final ShortlistRepository shortlistRepository;
    private final CompanyRepository companyRepository;
    private final StudentRepository studentRepository;
    private final EligibilityService eligibilityService;
    private final RankingService rankingService;
    private final ObjectMapper objectMapper;

    public ShortlistService(ShortlistRepository shortlistRepository,
                            CompanyRepository companyRepository,
                            StudentRepository studentRepository,
                            EligibilityService eligibilityService,
                            RankingService rankingService) {
        this.shortlistRepository = shortlistRepository;
        this.companyRepository = companyRepository;
        this.studentRepository = studentRepository;
        this.eligibilityService = eligibilityService;
        this.rankingService = rankingService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Generates a ranked shortlist of candidates for a visiting company.
     */
    @Transactional
    public Shortlist generateShortlist(Long companyId, Double cgpaWeight, Double skillsWeight, Double certsWeight) {
        // Step 1: Find company in database
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (!companyOpt.isPresent()) {
            throw new NoSuchElementException("Company not found with ID: " + companyId);
        }
        Company company = companyOpt.get();

        // Step 2: Validate ranking weights (default: 60% CGPA, 30% Skills, 10% Certifications)
        double wCgpa = (cgpaWeight != null) ? cgpaWeight : 60.0;
        double wSkills = (skillsWeight != null) ? skillsWeight : 30.0;
        double wCerts = (certsWeight != null) ? certsWeight : 10.0;

        if (Math.abs((wCgpa + wSkills + wCerts) - 100.0) > 0.001) {
            throw new IllegalArgumentException("Ranking weights must add up to exactly 100%");
        }

        // Step 3: Get all registered students
        List<Student> allStudents = studentRepository.findAll();

        // Create the new Shortlist header entity
        Shortlist shortlist = new Shortlist();
        shortlist.setCompanyId(company.getId());
        shortlist.setCompanyName(company.getName());
        shortlist.setJobRole(company.getJobRole());
        shortlist.setGeneratedAt(LocalDateTime.now());
        shortlist.setCgpaWeight(wCgpa);
        shortlist.setSkillsWeight(wSkills);
        shortlist.setCertsWeight(wCerts);
        shortlist.setTotalEvaluated(allStudents.size());

        // Save a human-readable snapshot of criteria
        String criteriaSnapshot = "Min CGPA: " + company.getMinCgpa()
                + " | Max Backlogs: " + company.getMaxBacklogs()
                + " | Skills: [" + company.getRequiredSkillsSummary() + "] (" + company.getSkillMatchingMode() + ")"
                + (company.getEligibleDepartments() != null ? " | Depts: [" + company.getEligibleDepartments() + "]" : "")
                + (company.getEligibleGraduationYear() != null ? " | Batch: " + company.getEligibleGraduationYear() : "");
        shortlist.setCriteriaSummary(criteriaSnapshot);

        List<RankingService.RankedCandidate> eligibleRankContainers = new ArrayList<>();
        List<ShortlistCandidate> ineligibleCandidates = new ArrayList<>();

        // Step 4: Loop through each student and test eligibility
        for (int i = 0; i < allStudents.size(); i++) {
            Student student = allStudents.get(i);
            EligibilityService.EvaluationResult eval = eligibilityService.evaluate(student, company);

            if (eval.isEligible()) {
                // If student passed all rules, add to eligible list for ranking
                RankingService.RankedCandidate rc = new RankingService.RankedCandidate(student, eval.getSkillMatchPercentage());
                eligibleRankContainers.add(rc);
            } else {
                // If student failed any rule, save as ineligible with exact rejection reasons
                ShortlistCandidate inelig = new ShortlistCandidate();
                inelig.setStudentId(student.getStudentId());
                inelig.setStudentName(student.getFullName());
                inelig.setDepartment(student.getDepartment());
                inelig.setCgpa(student.getCgpa());
                inelig.setActiveBacklogs(student.getActiveBacklogs());
                inelig.setSkillsSummary(student.getSkillsSummary());
                inelig.setMatchedSkillsSummary(eval.getMatchedSkillsSummary());
                inelig.setSkillMatchPercentage(eval.getSkillMatchPercentage());
                inelig.setRankingScore(0.0);
                inelig.setRankNumber(null);
                inelig.setEligible(false);
                inelig.setRejectionReasons(eval.getRejectionReasonsSummary());
                ineligibleCandidates.add(inelig);
            }
        }

        // Step 5: Rank eligible candidates (Highest score first, CGPA tie-breaker)
        List<RankingService.RankedCandidate> ranked = rankingService.rankCandidates(
                eligibleRankContainers, wCgpa, wSkills, wCerts);

        // Step 6: Add ranked candidates into the shortlist
        for (int i = 0; i < ranked.size(); i++) {
            RankingService.RankedCandidate rc = ranked.get(i);
            Student s = rc.getStudent();

            ShortlistCandidate candidate = new ShortlistCandidate();
            candidate.setStudentId(s.getStudentId());
            candidate.setStudentName(s.getFullName());
            candidate.setDepartment(s.getDepartment());
            candidate.setCgpa(s.getCgpa());
            candidate.setActiveBacklogs(s.getActiveBacklogs());
            candidate.setSkillsSummary(s.getSkillsSummary());

            EligibilityService.EvaluationResult eval = eligibilityService.evaluate(s, company);
            candidate.setMatchedSkillsSummary(eval.getMatchedSkillsSummary());
            candidate.setSkillMatchPercentage(rc.getSkillMatchPercentage());
            candidate.setRankingScore(rc.getRankingScore());
            candidate.setRankNumber(rc.getRank());
            candidate.setEligible(true);
            candidate.setRejectionReasons("");

            shortlist.addCandidate(candidate);
        }

        // Step 7: Add ineligible candidates into the shortlist
        for (int i = 0; i < ineligibleCandidates.size(); i++) {
            shortlist.addCandidate(ineligibleCandidates.get(i));
        }

        shortlist.setEligibleCount(ranked.size());
        shortlist.setIneligibleCount(ineligibleCandidates.size());

        // Step 8: Save to database and return
        return shortlistRepository.save(shortlist);
    }

    public List<Shortlist> getAllShortlists() {
        return shortlistRepository.findAllByOrderByGeneratedAtDesc();
    }

    public Shortlist getShortlistById(Long id) {
        Optional<Shortlist> shortlistOpt = shortlistRepository.findById(id);
        if (!shortlistOpt.isPresent()) {
            throw new NoSuchElementException("Shortlist not found with ID: " + id);
        }
        return shortlistOpt.get();
    }

    /**
     * Exports shortlist to CSV format.
     */
    public void exportToCsv(Long shortlistId, Writer writer) {
        Shortlist shortlist = getShortlistById(shortlistId);

        try {
            CSVFormat csvFormat = CSVFormat.Builder.create()
                    .setHeader("Rank", "Student ID", "Full Name", "Department", "CGPA",
                            "Active Backlogs", "All Skills", "Matched Skills", "Skill Match %",
                            "Ranking Score", "Eligibility Status", "Rejection Reasons")
                    .build();

            try (CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
                List<ShortlistCandidate> candidates = shortlist.getCandidates();
                for (int i = 0; i < candidates.size(); i++) {
                    ShortlistCandidate c = candidates.get(i);
                    printer.printRecord(
                            c.getRankNumber() != null ? c.getRankNumber() : "-",
                            c.getStudentId(),
                            c.getStudentName(),
                            c.getDepartment(),
                            c.getCgpa(),
                            c.getActiveBacklogs(),
                            c.getSkillsSummary(),
                            c.getMatchedSkillsSummary(),
                            String.format("%.1f%%", c.getSkillMatchPercentage()),
                            Boolean.TRUE.equals(c.getEligible()) ? c.getRankingScore() : "-",
                            Boolean.TRUE.equals(c.getEligible()) ? "ELIGIBLE" : "NOT ELIGIBLE",
                            c.getRejectionReasons() != null ? c.getRejectionReasons() : ""
                    );
                }
                printer.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error exporting CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Exports shortlist to readable JSON format.
     */
    public String exportToJson(Long shortlistId) {
        Shortlist shortlist = getShortlistById(shortlistId);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(shortlist);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing JSON: " + e.getMessage(), e);
        }
    }
}
