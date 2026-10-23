package com.placement.service;

import com.placement.model.Company;
import com.placement.model.Skill;
import com.placement.model.SkillMatchingMode;
import com.placement.model.Student;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Module 3: Eligibility Rule Engine.
 * 
 * Beginner Java Explanation:
 * This class checks if a student is eligible to apply for a company.
 * It checks 5 simple rules:
 *   1. Is CGPA >= Company's minimum CGPA?
 *   2. Are active backlogs <= Company's maximum allowed backlogs?
 *   3. Does the student belong to an eligible department (e.g. CSE, IT)?
 *   4. Is the student from the required graduation year?
 *   5. Does the student have the required technical skills (ALL or ANY)?
 *
 * If any rule fails, we record the exact reason in plain English so the student
 * knows why they were rejected.
 */
@Service
public class EligibilityService {

    /**
     * Simple container class holding the result of checking a student.
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

        // Helper to turn reasons list into a single semicolon-separated string
        public String getRejectionReasonsSummary() {
            if (rejectionReasons == null || rejectionReasons.isEmpty()) {
                return "";
            }
            return String.join("; ", rejectionReasons);
        }

        // Helper to turn matched skills into a nice comma-separated string
        public String getMatchedSkillsSummary() {
            if (matchedSkills == null || matchedSkills.isEmpty()) {
                return "None";
            }
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (Skill s : matchedSkills) {
                if (count > 0) {
                    sb.append(", ");
                }
                sb.append(s.getName());
                count++;
            }
            return sb.toString();
        }
    }

    /**
     * Evaluates a single student against a company's recruitment criteria.
     */
    public EvaluationResult evaluate(Student student, Company company) {
        List<String> reasons = new ArrayList<>();

        // Basic safety checks
        if (student == null) {
            reasons.add("Student record is missing");
            return new EvaluationResult(false, reasons, Collections.emptySet(), 0.0);
        }

        if (company == null) {
            reasons.add("Company criteria is missing");
            return new EvaluationResult(false, reasons, Collections.emptySet(), 0.0);
        }

        // =========================================================================
        // STEP 1: Check Minimum CGPA Cutoff
        // =========================================================================
        Double studentCgpa = student.getCgpa();
        if (studentCgpa == null) {
            reasons.add("Student CGPA is missing");
        } else if (studentCgpa < 0.0 || studentCgpa > 10.0) {
            reasons.add("Invalid CGPA (" + studentCgpa + "): must be between 0.0 and 10.0");
        } else if (studentCgpa < company.getMinCgpa()) {
            reasons.add("CGPA (" + studentCgpa + ") is below required minimum (" + company.getMinCgpa() + ")");
        }

        // =========================================================================
        // STEP 2: Check Active Backlogs
        // =========================================================================
        Integer studentBacklogs = student.getActiveBacklogs();
        if (studentBacklogs == null) {
            reasons.add("Student backlogs record is missing");
        } else if (studentBacklogs < 0) {
            reasons.add("Active backlogs cannot be negative (" + studentBacklogs + ")");
        } else if (studentBacklogs > company.getMaxBacklogs()) {
            reasons.add("Active backlogs (" + studentBacklogs + ") exceed permitted limit (" + company.getMaxBacklogs() + ")");
        }

        // =========================================================================
        // STEP 3: Check Department
        // =========================================================================
        String eligibleDepts = company.getEligibleDepartments();
        if (eligibleDepts != null && !eligibleDepts.trim().isEmpty()) {
            String studentDept = student.getDepartment();
            if (studentDept == null) {
                studentDept = "";
            }
            String cleanStudentDept = studentDept.trim();

            // Split comma-separated departments, e.g. "CSE, IT"
            String[] allowedArray = eligibleDepts.split(",");
            boolean departmentAllowed = false;
            for (int i = 0; i < allowedArray.length; i++) {
                String allowed = allowedArray[i].trim();
                if (allowed.equalsIgnoreCase(cleanStudentDept)) {
                    departmentAllowed = true;
                    break;
                }
            }

            if (!departmentAllowed) {
                reasons.add("Department '" + cleanStudentDept + "' is not in eligible departments: [" + eligibleDepts.trim() + "]");
            }
        }

        // =========================================================================
        // STEP 4: Check Graduation Year (Batch)
        // =========================================================================
        Integer requiredYear = company.getEligibleGraduationYear();
        if (requiredYear != null && requiredYear > 0) {
            Integer studentYear = student.getGraduationYear();
            if (studentYear == null || !studentYear.equals(requiredYear)) {
                reasons.add("Graduation year (" + studentYear + ") does not match required year (" + requiredYear + ")");
            }
        }

        // =========================================================================
        // STEP 5: Check Technical Skills (ALL mode vs ANY mode)
        // =========================================================================
        Set<Skill> companySkills = company.getRequiredSkills();
        if (companySkills == null) {
            companySkills = Collections.emptySet();
        }

        Set<Skill> studentSkills = student.getSkills();
        if (studentSkills == null) {
            studentSkills = Collections.emptySet();
        }

        Set<Skill> matchedSkills = new HashSet<>();
        List<String> missingSkills = new ArrayList<>();

        // Loop through each skill the company requires
        for (Skill required : companySkills) {
            boolean foundMatch = false;

            // Check if student has this skill
            for (Skill possessed : studentSkills) {
                if (possessed.getName().equalsIgnoreCase(required.getName())) {
                    foundMatch = true;
                    matchedSkills.add(required);
                    break;
                }
            }

            if (!foundMatch) {
                missingSkills.add(required.getName());
            }
        }

        // Calculate skill match percentage (e.g. 2 out of 4 skills = 50.0%)
        double skillMatchPercentage = 100.0;
        if (!companySkills.isEmpty()) {
            skillMatchPercentage = ((double) matchedSkills.size() / companySkills.size()) * 100.0;
            skillMatchPercentage = Math.round(skillMatchPercentage * 100.0) / 100.0;

            // Mode 1: ALL skills required
            if (company.getSkillMatchingMode() == SkillMatchingMode.ALL) {
                if (!missingSkills.isEmpty()) {
                    reasons.add("Missing required skills for ALL mode: " + String.join(", ", missingSkills));
                }
            } 
            // Mode 2: ANY skill matches
            else {
                if (matchedSkills.isEmpty()) {
                    List<String> requiredNames = new ArrayList<>();
                    for (Skill s : companySkills) {
                        requiredNames.add(s.getName());
                    }
                    reasons.add("Must possess at least one of required skills: " + String.join(", ", requiredNames));
                }
            }
        }

        // If there are zero rejection reasons, the student is ELIGIBLE!
        boolean isEligible = reasons.isEmpty();
        return new EvaluationResult(isEligible, reasons, matchedSkills, skillMatchPercentage);
    }
}
