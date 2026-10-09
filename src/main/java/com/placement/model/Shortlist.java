package com.placement.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the result of a generated shortlist session for historical review.
 */
@Entity
@Table(name = "shortlists")
public class Shortlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String jobRole;

    @Column(length = 500)
    private String criteriaSummary;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    private Double cgpaWeight = 60.0;
    private Double skillsWeight = 30.0;
    private Double certsWeight = 10.0;

    private Integer totalEvaluated = 0;
    private Integer eligibleCount = 0;
    private Integer ineligibleCount = 0;

    @OneToMany(mappedBy = "shortlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShortlistCandidate> candidates = new ArrayList<>();

    public Shortlist() {
        this.generatedAt = LocalDateTime.now();
    }

    public void addCandidate(ShortlistCandidate candidate) {
        this.candidates.add(candidate);
        candidate.setShortlist(this);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getJobRole() {
        return jobRole;
    }

    public void setJobRole(String jobRole) {
        this.jobRole = jobRole;
    }

    public String getCriteriaSummary() {
        return criteriaSummary;
    }

    public void setCriteriaSummary(String criteriaSummary) {
        this.criteriaSummary = criteriaSummary;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Double getCgpaWeight() {
        return cgpaWeight;
    }

    public void setCgpaWeight(Double cgpaWeight) {
        this.cgpaWeight = cgpaWeight;
    }

    public Double getSkillsWeight() {
        return skillsWeight;
    }

    public void setSkillsWeight(Double skillsWeight) {
        this.skillsWeight = skillsWeight;
    }

    public Double getCertsWeight() {
        return certsWeight;
    }

    public void setCertsWeight(Double certsWeight) {
        this.certsWeight = certsWeight;
    }

    public Integer getTotalEvaluated() {
        return totalEvaluated;
    }

    public void setTotalEvaluated(Integer totalEvaluated) {
        this.totalEvaluated = totalEvaluated;
    }

    public Integer getEligibleCount() {
        return eligibleCount;
    }

    public void setEligibleCount(Integer eligibleCount) {
        this.eligibleCount = eligibleCount;
    }

    public Integer getIneligibleCount() {
        return ineligibleCount;
    }

    public void setIneligibleCount(Integer ineligibleCount) {
        this.ineligibleCount = ineligibleCount;
    }

    public List<ShortlistCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<ShortlistCandidate> candidates) {
        this.candidates = candidates;
    }
}
