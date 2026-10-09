package com.placement.service;

import com.placement.model.Company;
import com.placement.model.Shortlist;
import com.placement.model.Student;
import com.placement.repository.CompanyRepository;
import com.placement.repository.ShortlistRepository;
import com.placement.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Module 6: Dashboard and Placement Analytics Service.
 * Computes high-level summaries, department distributions, and company statistics.
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

    public Map<String, Object> getOverallStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalStudents = studentRepository.count();
        long totalCompanies = companyRepository.count();
        List<Shortlist> allShortlists = shortlistRepository.findAllByOrderByGeneratedAtDesc();
        long totalShortlists = allShortlists.size();

        int latestEligibleCount = 0;
        String latestShortlistCompany = "None";
        String latestShortlistTime = "-";

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

    public List<Map<String, Object>> getDepartmentDistribution() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Object[]> rows = studentRepository.countStudentsByDepartment();

        for (Object[] row : rows) {
            String dept = (String) row[0];
            Long count = (Long) row[1];
            Map<String, Object> item = new HashMap<>();
            item.put("department", dept != null ? dept : "Unknown");
            item.put("count", count);
            result.add(item);
        }

        return result;
    }

    public List<Map<String, Object>> getCompanyEligibilityStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Company> activeCompanies = companyRepository.findByActiveTrue();

        for (Company company : activeCompanies) {
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
