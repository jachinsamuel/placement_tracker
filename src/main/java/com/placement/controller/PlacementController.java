package com.placement.controller;

import com.placement.model.Company;
import com.placement.model.Shortlist;
import com.placement.model.Student;
import com.placement.service.PlacementService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Single Unified REST Controller for the Placement Portal.
 * 
 * Beginner Java Explanation:
 * All browser HTTP requests arrive here.
 * Instead of 5 separate controllers, this single class routes all operations:
 *   - Students:  GET, POST, PUT, DELETE /api/students
 *   - Companies: GET, POST, PUT, DELETE /api/companies
 *   - Shortlists: POST /api/shortlists/generate, CSV/JSON exports
 *   - Dashboard: GET /api/dashboard/stats, charts
 *   - Exception Handling: Catches errors and returns friendly messages
 */
@RestController
public class PlacementController {

    private final PlacementService placementService;

    public PlacementController(PlacementService placementService) {
        this.placementService = placementService;
    }

    // =========================================================================
    // 1. STUDENT ENDPOINTS (/api/students)
    // =========================================================================

    @GetMapping("/api/students")
    public List<Student> getAllStudents(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return placementService.searchStudents(search);
        }
        if (department != null && !department.trim().isEmpty()) {
            return placementService.filterByDepartment(department);
        }
        return placementService.getAllStudents();
    }

    @GetMapping("/api/students/{id}")
    public Student getStudentById(@PathVariable Long id) {
        Optional<Student> opt = placementService.getStudentById(id);
        if (!opt.isPresent()) {
            throw new NoSuchElementException("Student not found with ID: " + id);
        }
        return opt.get();
    }

    @PostMapping("/api/students")
    @ResponseStatus(HttpStatus.CREATED)
    public Student createStudent(@RequestBody Student student) {
        return placementService.createStudent(student);
    }

    @PutMapping("/api/students/{id}")
    public Student updateStudent(@PathVariable Long id, @RequestBody Student student) {
        return placementService.updateStudent(id, student);
    }

    @DeleteMapping("/api/students/{id}")
    public Map<String, String> deleteStudent(@PathVariable Long id) {
        placementService.deleteStudent(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Student deleted successfully");
        return response;
    }

    @PostMapping(value = "/api/students/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }
        try {
            int count = placementService.importStudentsFromCsv(file.getInputStream());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Import completed successfully");
            response.put("importedCount", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to import CSV: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // 2. COMPANY ENDPOINTS (/api/companies)
    // =========================================================================

    @GetMapping("/api/companies")
    public List<Company> getAllCompanies(@RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return placementService.getActiveCompanies();
        }
        return placementService.getAllCompanies();
    }

    @GetMapping("/api/companies/{id}")
    public Company getCompanyById(@PathVariable Long id) {
        Optional<Company> opt = placementService.getCompanyById(id);
        if (!opt.isPresent()) {
            throw new NoSuchElementException("Company not found with ID: " + id);
        }
        return opt.get();
    }

    @PostMapping("/api/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public Company createCompany(@RequestBody Company company) {
        return placementService.createCompany(company);
    }

    @PutMapping("/api/companies/{id}")
    public Company updateCompany(@PathVariable Long id, @RequestBody Company company) {
        return placementService.updateCompany(id, company);
    }

    @DeleteMapping("/api/companies/{id}")
    public Map<String, String> deleteCompany(@PathVariable Long id) {
        placementService.deleteCompany(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Company deleted successfully");
        return response;
    }

    @PostMapping(value = "/api/companies/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importCompaniesCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty");
        }
        try {
            int count = placementService.importCompaniesFromCsv(file.getInputStream());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Import completed successfully");
            response.put("importedCount", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to import CSV: " + e.getMessage(), e);
        }
    }

    @PutMapping("/api/companies/{id}/toggle-active")
    public Company toggleCompanyActive(@PathVariable Long id) {
        return placementService.toggleCompanyActive(id);
    }

    @GetMapping("/api/companies/{id}/eligible-count")
    public Map<String, Object> getEligibleCount(@PathVariable Long id) {
        int count = placementService.countEligibleCandidates(id);
        Map<String, Object> response = new HashMap<>();
        response.put("companyId", id);
        response.put("eligibleCount", count);
        return response;
    }

    // =========================================================================
    // 3. SHORTLIST ENDPOINTS (/api/shortlists)
    // =========================================================================

    @PostMapping("/api/shortlists/generate/{companyId}")
    public Shortlist generateShortlist(
            @PathVariable Long companyId,
            @RequestBody(required = false) Map<String, Double> weights) {
        Double cgpaWeight = 60.0;
        Double skillsWeight = 30.0;
        Double certsWeight = 10.0;

        if (weights != null) {
            if (weights.containsKey("cgpaWeight")) cgpaWeight = weights.get("cgpaWeight");
            if (weights.containsKey("skillsWeight")) skillsWeight = weights.get("skillsWeight");
            if (weights.containsKey("certsWeight")) certsWeight = weights.get("certsWeight");
        }

        return placementService.generateShortlist(companyId, cgpaWeight, skillsWeight, certsWeight);
    }

    @GetMapping("/api/shortlists")
    public List<Shortlist> getAllShortlists() {
        return placementService.getAllShortlists();
    }

    @GetMapping("/api/shortlists/{id}")
    public Shortlist getShortlistById(@PathVariable Long id) {
        return placementService.getShortlistById(id);
    }

    @GetMapping("/api/shortlists/{id}/export/csv")
    public void exportCsv(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Shortlist shortlist = placementService.getShortlistById(id);
        String filename = shortlist.getCompanyName().replaceAll("[^a-zA-Z0-9]", "_") + "_shortlist.csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        StringWriter writer = new StringWriter();
        placementService.exportToCsv(id, writer);
        response.getWriter().write(writer.toString());
        response.getWriter().flush();
    }

    @GetMapping("/api/shortlists/{id}/export/json")
    public ResponseEntity<String> exportJson(@PathVariable Long id) {
        Shortlist shortlist = placementService.getShortlistById(id);
        String filename = shortlist.getCompanyName().replaceAll("[^a-zA-Z0-9]", "_") + "_shortlist.json";

        String json = placementService.exportToJson(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(json);
    }

    // =========================================================================
    // 4. DASHBOARD ENDPOINTS (/api/dashboard)
    // =========================================================================

    @GetMapping("/api/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getOverallStats() {
        return ResponseEntity.ok(placementService.getOverallStats());
    }

    @GetMapping("/api/dashboard/department-stats")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentDistribution() {
        return ResponseEntity.ok(placementService.getDepartmentDistribution());
    }

    @GetMapping("/api/dashboard/company-stats")
    public ResponseEntity<List<Map<String, Object>>> getCompanyEligibilityStats() {
        return ResponseEntity.ok(placementService.getCompanyEligibilityStats());
    }

    // =========================================================================
    // 5. ERROR HANDLING
    // =========================================================================

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
