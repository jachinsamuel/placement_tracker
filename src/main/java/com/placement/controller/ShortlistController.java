package com.placement.controller;

import com.placement.model.Shortlist;
import com.placement.service.ShortlistService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for generating shortlists and exporting CSV/JSON reports.
 * 
 * Beginner Java Explanation:
 * Handles HTTP requests for:
 *   - POST /api/shortlists/generate/{companyId} -> Runs the eligibility & ranking engine
 *   - GET  /api/shortlists                      -> View shortlist history
 *   - GET  /api/shortlists/{id}/export/csv      -> Download results as CSV
 *   - GET  /api/shortlists/{id}/export/json     -> Download results as JSON
 */
@RestController
@RequestMapping("/api/shortlists")
public class ShortlistController {

    private final ShortlistService shortlistService;

    public ShortlistController(ShortlistService shortlistService) {
        this.shortlistService = shortlistService;
    }

    @PostMapping("/generate/{companyId}")
    public Shortlist generateShortlist(
            @PathVariable Long companyId,
            @RequestBody(required = false) Map<String, Double> weights) {

        // Default weights: 60% CGPA, 30% Skills, 10% Certifications
        Double cgpaWeight = 60.0;
        Double skillsWeight = 30.0;
        Double certsWeight = 10.0;

        if (weights != null) {
            if (weights.containsKey("cgpaWeight")) {
                cgpaWeight = weights.get("cgpaWeight");
            }
            if (weights.containsKey("skillsWeight")) {
                skillsWeight = weights.get("skillsWeight");
            }
            if (weights.containsKey("certsWeight")) {
                certsWeight = weights.get("certsWeight");
            }
        }

        return shortlistService.generateShortlist(companyId, cgpaWeight, skillsWeight, certsWeight);
    }

    @GetMapping
    public List<Shortlist> getAllShortlists() {
        return shortlistService.getAllShortlists();
    }

    @GetMapping("/{id}")
    public Shortlist getShortlistById(@PathVariable Long id) {
        return shortlistService.getShortlistById(id);
    }

    @GetMapping("/{id}/export/csv")
    public void exportCsv(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Shortlist shortlist = shortlistService.getShortlistById(id);
        String filename = shortlist.getCompanyName().replaceAll("[^a-zA-Z0-9]", "_") + "_shortlist.csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        StringWriter writer = new StringWriter();
        shortlistService.exportToCsv(id, writer);
        response.getWriter().write(writer.toString());
        response.getWriter().flush();
    }

    @GetMapping("/{id}/export/json")
    public ResponseEntity<String> exportJson(@PathVariable Long id) {
        Shortlist shortlist = shortlistService.getShortlistById(id);
        String filename = shortlist.getCompanyName().replaceAll("[^a-zA-Z0-9]", "_") + "_shortlist.json";

        String json = shortlistService.exportToJson(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(json);
    }
}
