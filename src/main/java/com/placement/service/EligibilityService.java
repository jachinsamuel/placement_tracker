package com.placement.service;

import com.placement.model.Company;
import com.placement.model.Skill;
import com.placement.model.SkillMatchingMode;
import com.placement.model.Student;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Module 3: Eligibility Rule Engine.
 * Evaluates candidates against company criteria and provides clear rejection reasons.
 */
@Service
public class EligibilityService {

    /**
     * Data holder representing the result of evaluating a student against company criteria.
     */
    public static class EvaluationResult {
        private final boolean eligible;
        private final List<String> rejectionReasons;
        private final Set<Skill> matchedSkills;
        private final double skillMatchPercentage;

        public EvaluationResult(boolean eligible, List<String> rejectionReasons,
                                Set<Skill> matchedSkills, double skillMatchPercentage) {
            this.eligible = eligible;
            this.rejectionReasons = rejectionReasons;
            this.matchedSkills = matchedSkills;
            this.skillMatchPercentage = skillMatchPercentage;
        }

        public boolean isEligible() {
            return eligible;
        }

        public List<String> getRejectionReasons() {
            return rejectionReasons;
        }

        public Set<Skill> getMatchedSkills() {
            return matchedSkills;
        }

        public double getSkillMatchPercentage() {
            return skillMatchPercentage;
        }

        public String getRejectionReasonsSummary() {
            if (rejectionReasons == null || rejectionReasons.isEmpty()) {
                return "";
            }
            return String.join("; ", rejectionReasons);
        }

        public String getMatchedSkillsSummary() {
            if (matchedSkills == null || matchedSkills.isEmpty()) {
                return "None";
            }
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (Skill s : matchedSkills) {
                if (count > 0) sb.append(", ");
                sb.append(s.getName());
                count++;
            }
            return sb.toString();
        }
    }

    /**
     * Evaluates a single student against the rules of a company.
     * All mandatory conditions must be satisfied for the student to be eligible.
     */
    public EvaluationResult evaluate(Student student, Company company) {
        List<String> reasons = new ArrayList<>();

        if (student == null) {
            reasons.add("Student record is missing");
            return new EvaluationResult(false, reasons, Collections.emptySet(), 0.0);
        }

        if (company == null) {
            reasons.add("Company criteria is missing");
            return new EvaluationResult(false, reasons, Collections.emptySet(), 0.0);
        }

        // 1. Minimum CGPA Check
        if (student.getCgpa() == null || student.getCgpa() < 0.0 || student.getCgpa() > 10.0 || student.getCgpa() < company.getMinCgpa()) {
            double actualCgpa = student.getCgpa() != null ? student.getCgpa() : 0.0;
            if (student.getCgpa() != null && (student.getCgpa() < 0.0 || student.getCgpa() > 10.0)) {
                reasons.add("Invalid CGPA (" + actualCgpa + "): must be between 0.0 and 10.0");
            } else {
                reasons.add("CGPA (" + actualCgpa + ") is below required minimum (" + company.getMinCgpa() + ")");
            }
        }

        // 2. Maximum Active Backlogs Check
        if (student.getActiveBacklogs() == null || student.getActiveBacklogs() < 0 || student.getActiveBacklogs() > company.getMaxBacklogs()) {
            int actualBacklogs = student.getActiveBacklogs() != null ? student.getActiveBacklogs() : 0;
            if (student.getActiveBacklogs() != null && student.getActiveBacklogs() < 0) {
                reasons.add("Active backlogs cannot be negative (" + actualBacklogs + ")");
            } else {
                reasons.add("Active backlogs (" + actualBacklogs + ") exceed permitted limit (" + company.getMaxBacklogs() + ")");
            }
        }

        // 3. Department Eligibility Check
        if (company.getEligibleDepartments() != null && !company.getEligibleDepartments().trim().isEmpty()) {
            String[] allowedDepts = company.getEligibleDepartments().split(",");
            boolean deptMatches = false;
            String studentDept = student.getDepartment() != null ? student.getDepartment().trim() : "";

            for (String allowed : allowedDepts) {
                if (allowed.trim().equalsIgnoreCase(studentDept)) {
                    deptMatches = true;
                    break;
                }
            }

            if (!deptMatches) {
                reasons.add("Department '" + studentDept + "' is not in eligible departments: ["
                        + company.getEligibleDepartments().trim() + "]");
            }
        }

        // 4. Graduation Year Check
        if (company.getEligibleGraduationYear() != null && company.getEligibleGraduationYear() > 0) {
            if (student.getGraduationYear() == null
                    || !student.getGraduationYear().equals(company.getEligibleGraduationYear())) {
                reasons.add("Graduation year (" + student.getGraduationYear()
                        + ") does not match required year (" + company.getEligibleGraduationYear() + ")");
            }
        }

        // 5. Technical Skills Check & Matching Mode (ANY vs ALL)
        Set<Skill> companySkills = company.getRequiredSkills() != null ? company.getRequiredSkills() : Collections.emptySet();
        Set<Skill> studentSkills = student.getSkills() != null ? student.getSkills() : Collections.emptySet();

        Set<Skill> matchedSkills = new HashSet<>();
        List<String> missingSkills = new ArrayList<>();

        for (Skill required : companySkills) {
            boolean hasSkill = false;
            for (Skill possessed : studentSkills) {
                if (possessed.getName().equalsIgnoreCase(required.getName())) {
                    hasSkill = true;
                    matchedSkills.add(required);
                    break;
                }
            }
            if (!hasSkill) {
                missingSkills.add(required.getName());
            }
        }

        double skillMatchPercentage = 100.0;
        if (!companySkills.isEmpty()) {
            skillMatchPercentage = ((double) matchedSkills.size() / companySkills.size()) * 100.0;

            if (company.getSkillMatchingMode() == SkillMatchingMode.ALL) {
                if (!missingSkills.isEmpty()) {
                    reasons.add("Missing required skills for ALL mode: " + String.join(", ", missingSkills));
                }
            } else { // ANY mode
                if (matchedSkills.isEmpty()) {
                    List<String> requiredNames = new ArrayList<>();
                    for (Skill s : companySkills) {
                        requiredNames.add(s.getName());
                    }
                    reasons.add("Must possess at least one of required skills: " + String.join(", ", requiredNames));
                }
            }
        }

        boolean eligible = reasons.isEmpty();
        return new EvaluationResult(eligible, reasons, matchedSkills, skillMatchPercentage);
    }
}
