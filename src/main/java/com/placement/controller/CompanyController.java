package com.placement.controller;

import com.placement.model.Company;
import com.placement.service.CompanyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Company Management and Criteria operations.
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
        return companyService.getCompanyById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Company not found with ID: " + id));
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
        Map<String, String> res = new HashMap<>();
        res.put("message", "Company deleted successfully");
        return res;
    }

    @PutMapping("/{id}/toggle-active")
    public Company toggleActive(@PathVariable Long id) {
        return companyService.toggleCompanyActive(id);
    }

    @GetMapping("/{id}/eligible-count")
    public Map<String, Object> getEligibleCount(@PathVariable Long id) {
        int count = companyService.countEligibleCandidates(id);
        Map<String, Object> res = new HashMap<>();
        res.put("companyId", id);
        res.put("eligibleCount", count);
        return res;
    }
}
