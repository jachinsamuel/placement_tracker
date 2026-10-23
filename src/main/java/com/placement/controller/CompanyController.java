package com.placement.controller;

import com.placement.model.Company;
import com.placement.service.CompanyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * REST Controller for Company Management and Criteria operations.
 * 
 * Beginner Java Explanation:
 * Handles HTTP requests for visiting companies:
 *   - GET    /api/companies               -> List all companies
 *   - GET    /api/companies/{id}          -> Get company by ID
 *   - POST   /api/companies               -> Register new company
 *   - PUT    /api/companies/{id}          -> Update company criteria
 *   - DELETE /api/companies/{id}          -> Delete company
 *   - PUT    /api/companies/{id}/toggle-active -> Toggle drive status
 */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public List<Company> getAllCompanies(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return companyService.getActiveCompanies();
        }
        return companyService.getAllCompanies();
    }

    @GetMapping("/{id}")
    public Company getCompanyById(@PathVariable Long id) {
        Optional<Company> companyOpt = companyService.getCompanyById(id);
        if (!companyOpt.isPresent()) {
            throw new NoSuchElementException("Company not found with ID: " + id);
        }
        return companyOpt.get();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Company createCompany(@RequestBody Company company) {
        return companyService.createCompany(company);
    }

    @PutMapping("/{id}")
    public Company updateCompany(@PathVariable Long id, @RequestBody Company company) {
        return companyService.updateCompany(id, company);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Company deleted successfully");
        return response;
    }

    @PutMapping("/{id}/toggle-active")
    public Company toggleActive(@PathVariable Long id) {
        return companyService.toggleCompanyActive(id);
    }

    @GetMapping("/{id}/eligible-count")
    public Map<String, Object> getEligibleCount(@PathVariable Long id) {
        int count = companyService.countEligibleCandidates(id);
        Map<String, Object> response = new HashMap<>();
        response.put("companyId", id);
        response.put("eligibleCount", count);
        return response;
    }
}
