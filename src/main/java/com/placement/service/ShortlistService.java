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
 * Evaluates all candidates against company criteria, ranks eligible candidates,
 * stores history, and exports results to CSV and JSON formats.
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
     * Runs the candidate shortlisting & ranking pipeline.
     */
    @Transactional
    public Shortlist generateShortlist(Long companyId, Double cgpaWeight, Double skillsWeight, Double certsWeight) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NoSuchElementException("Company not found with ID: " + companyId));

        double wCgpa = cgpaWeight != null ? cgpaWeight : 60.0;
        double wSkills = skillsWeight != null ? skillsWeight : 30.0;
        double wCerts = certsWeight != null ? certsWeight : 10.0;

        if (Math.abs((wCgpa + wSkills + wCerts) - 100.0) > 0.001) {
            throw new IllegalArgumentException("Ranking weights must add up to exactly 100%");
        }

        List<Student> allStudents = studentRepository.findAll();

        Shortlist shortlist = new Shortlist();
        shortlist.setCompanyId(company.getId());
        shortlist.setCompanyName(company.getName());
        shortlist.setJobRole(company.getJobRole());
        shortlist.setGeneratedAt(LocalDateTime.now());
        shortlist.setCgpaWeight(wCgpa);
        shortlist.setSkillsWeight(wSkills);
        shortlist.setCertsWeight(wCerts);
        shortlist.setTotalEvaluated(allStudents.size());

        // Snapshot of rules for historical reproduction
        String criteriaSnapshot = "Min CGPA: " + company.getMinCgpa()
                + " | Max Backlogs: " + company.getMaxBacklogs()
                + " | Skills: [" + company.getRequiredSkillsSummary() + "] (" + company.getSkillMatchingMode() + ")"
                + (company.getEligibleDepartments() != null ? " | Depts: [" + company.getEligibleDepartments() + "]" : "")
                + (company.getEligibleGraduationYear() != null ? " | Batch: " + company.getEligibleGraduationYear() : "");
        shortlist.setCriteriaSummary(criteriaSnapshot);

        List<RankingService.RankedCandidate> eligibleRankContainers = new ArrayList<>();
        List<ShortlistCandidate> ineligibleCandidates = new ArrayList<>();

        // Step 1: Evaluate each student
        for (Student student : allStudents) {
            EligibilityService.EvaluationResult eval = eligibilityService.evaluate(student, company);

            if (eval.isEligible()) {
                RankingService.RankedCandidate rc = new RankingService.RankedCandidate(student, eval.getSkillMatchPercentage());
                eligibleRankContainers.add(rc);
            } else {
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

        // Step 2: Rank eligible candidates
        List<RankingService.RankedCandidate> ranked = rankingService.rankCandidates(
                eligibleRankContainers, wCgpa, wSkills, wCerts);

        // Step 3: Add ranked candidates
        for (RankingService.RankedCandidate rc : ranked) {
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

        // Add ineligible candidates
        for (ShortlistCandidate inelig : ineligibleCandidates) {
            shortlist.addCandidate(inelig);
        }

        shortlist.setEligibleCount(ranked.size());
        shortlist.setIneligibleCount(ineligibleCandidates.size());

        return shortlistRepository.save(shortlist);
    }

    public List<Shortlist> getAllShortlists() {
        return shortlistRepository.findAllByOrderByGeneratedAtDesc();
    }

    public Shortlist getShortlistById(Long id) {
        return shortlistRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Shortlist not found with ID: " + id));
    }

    /**
     * Exports shortlist to CSV format using Apache Commons CSV.
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
                for (ShortlistCandidate c : shortlist.getCandidates()) {
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
                            c.getEligible() ? c.getRankingScore() : "-",
                            c.getEligible() ? "ELIGIBLE" : "NOT ELIGIBLE",
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
     * Exports shortlist to pretty JSON.
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
