package com.placement.service;

import com.placement.model.Company;
import com.placement.model.Shortlist;
import com.placement.repository.CompanyRepository;
import com.placement.repository.ShortlistRepository;
import com.placement.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Module 6: Dashboard and Placement Analytics Service.
 * 
 * Beginner Java Explanation:
 * This service calculates simple summary numbers for the top dashboard cards:
 *   - Total registered students
 *   - Total visiting companies
 *   - Total shortlist rounds generated
 *   - Breakdown of students per department (for charts)
 */
@Service
public class DashboardService {

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final ShortlistRepository shortlistRepository;
    private final CompanyService companyService;

    public DashboardService(StudentRepository studentRepository,
                            CompanyRepository companyRepository,
                            ShortlistRepository shortlistRepository,
                            CompanyService companyService) {
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.shortlistRepository = shortlistRepository;
        this.companyService = companyService;
    }

    /**
     * Calculates the overall count statistics for the dashboard cards.
     */
    public Map<String, Object> getOverallStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalStudents = studentRepository.count();
        long totalCompanies = companyRepository.count();
        List<Shortlist> allShortlists = shortlistRepository.findAllByOrderByGeneratedAtDesc();
        long totalShortlists = allShortlists.size();

        int latestEligibleCount = 0;
        String latestShortlistCompany = "None";
        String latestShortlistTime = "-";

        // If at least one shortlist drive has been run, get info about the most recent one
        if (!allShortlists.isEmpty()) {
            Shortlist latest = allShortlists.get(0);
            latestEligibleCount = latest.getEligibleCount();
            latestShortlistCompany = latest.getCompanyName();
            if (latest.getGeneratedAt() != null) {
                latestShortlistTime = latest.getGeneratedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
            }
        }

        stats.put("totalStudents", totalStudents);
        stats.put("totalCompanies", totalCompanies);
        stats.put("totalShortlists", totalShortlists);
        stats.put("latestEligibleCount", latestEligibleCount);
        stats.put("latestShortlistCompany", latestShortlistCompany);
        stats.put("latestShortlistTime", latestShortlistTime);

        return stats;
    }

    /**
     * Groups students by department to draw the donut chart on the frontend.
     */
    public List<Map<String, Object>> getDepartmentDistribution() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Object[]> rows = studentRepository.countStudentsByDepartment();

        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            String dept = (String) row[0];
            Long count = (Long) row[1];

            Map<String, Object> item = new HashMap<>();
            item.put("department", (dept != null) ? dept : "Unknown");
            item.put("count", count);
            result.add(item);
        }

        return result;
    }

    /**
     * Lists active companies with how many students currently qualify for each.
     */
    public List<Map<String, Object>> getCompanyEligibilityStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Company> activeCompanies = companyRepository.findByActiveTrue();

        for (int i = 0; i < activeCompanies.size(); i++) {
            Company company = activeCompanies.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("id", company.getId());
            item.put("name", company.getName());
            item.put("jobRole", company.getJobRole());
            item.put("minCgpa", company.getMinCgpa());
            item.put("eligibleCount", companyService.countEligibleCandidates(company.getId()));
            result.add(item);
        }

        return result;
    }
}
