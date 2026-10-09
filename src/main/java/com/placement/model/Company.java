package com.placement.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a visiting company with recruitment eligibility criteria.
 */
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String jobRole;

    @Column(nullable = false)
    private Double minCgpa;

    @Column(nullable = false)
    private Integer maxBacklogs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillMatchingMode skillMatchingMode = SkillMatchingMode.ALL;

    // Comma-separated list of departments: e.g. "CSE, IT" (null or empty means all departments)
    private String eligibleDepartments;

    // Eligible graduation year: e.g. 2027 (null means any batch)
    private Integer eligibleGraduationYear;

    private Boolean active = true;

    /**
     * Temporary holder for required skills when receiving JSON or forms from the frontend.
     */
    @Transient
    private String requiredSkillNames;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "company_required_skills",
        joinColumns = @JoinColumn(name = "company_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> requiredSkills = new HashSet<>();

    public Company() {
    }

    public Company(String name, String jobRole, Double minCgpa, Integer maxBacklogs,
                   SkillMatchingMode skillMatchingMode, String eligibleDepartments,
                   Integer eligibleGraduationYear) {
        this.name = name;
        this.jobRole = jobRole;
        this.minCgpa = minCgpa;
        this.maxBacklogs = maxBacklogs;
        this.skillMatchingMode = skillMatchingMode != null ? skillMatchingMode : SkillMatchingMode.ALL;
        this.eligibleDepartments = eligibleDepartments;
        this.eligibleGraduationYear = eligibleGraduationYear;
        this.active = true;
    }

    public void addRequiredSkill(Skill skill) {
        if (skill != null) {
            this.requiredSkills.add(skill);
        }
    }

    public String getRequiredSkillsSummary() {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return "None Required";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Skill s : requiredSkills) {
            if (i > 0) sb.append(", ");
            sb.append(s.getName());
            i++;
        }
        return sb.toString();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJobRole() {
        return jobRole;
    }

    public void setJobRole(String jobRole) {
        this.jobRole = jobRole;
    }

    public Double getMinCgpa() {
        return minCgpa;
    }

    public void setMinCgpa(Double minCgpa) {
        this.minCgpa = minCgpa;
    }

    public Integer getMaxBacklogs() {
        return maxBacklogs;
    }

    public void setMaxBacklogs(Integer maxBacklogs) {
        this.maxBacklogs = maxBacklogs;
    }

    public SkillMatchingMode getSkillMatchingMode() {
        return skillMatchingMode != null ? skillMatchingMode : SkillMatchingMode.ALL;
    }

    public void setSkillMatchingMode(SkillMatchingMode skillMatchingMode) {
        this.skillMatchingMode = skillMatchingMode != null ? skillMatchingMode : SkillMatchingMode.ALL;
    }

    public String getEligibleDepartments() {
        return eligibleDepartments;
    }

    public void setEligibleDepartments(String eligibleDepartments) {
        this.eligibleDepartments = eligibleDepartments;
    }

    public Integer getEligibleGraduationYear() {
        return eligibleGraduationYear;
    }

    public void setEligibleGraduationYear(Integer eligibleGraduationYear) {
        this.eligibleGraduationYear = eligibleGraduationYear;
    }

    public Boolean getActive() {
        return active != null ? active : true;
    }

    public void setActive(Boolean active) {
        this.active = active != null ? active : true;
    }

    public String getRequiredSkillNames() {
        if (requiredSkillNames != null) return requiredSkillNames;
        return getRequiredSkillsSummary();
    }

    public void setRequiredSkillNames(String requiredSkillNames) {
        this.requiredSkillNames = requiredSkillNames;
    }

    public Set<Skill> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(Set<Skill> requiredSkills) {
        this.requiredSkills = requiredSkills != null ? requiredSkills : new HashSet<>();
    }
}
