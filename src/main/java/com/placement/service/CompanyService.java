package com.placement.service;

import com.placement.model.Company;
import com.placement.model.Skill;
import com.placement.model.Student;
import com.placement.repository.CompanyRepository;
import com.placement.repository.SkillRepository;
import com.placement.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Module 2: Company Management Service.
 * 
 * Beginner Java Explanation:
 * This class handles company records and eligibility criteria:
 *   - Add visiting companies with job role and minimum cutoffs.
 *   - Edit criteria (CGPA cutoff, max backlogs, required skills, allowed departments).
 *   - Toggle company active/inactive status.
 *   - Count how many registered students qualify in real time.
 */
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final SkillRepository skillRepository;
    private final StudentRepository studentRepository;
    private final EligibilityService eligibilityService;

    public CompanyService(CompanyRepository companyRepository,
                          SkillRepository skillRepository,
                          StudentRepository studentRepository,
                          EligibilityService eligibilityService) {
        this.companyRepository = companyRepository;
        this.skillRepository = skillRepository;
        this.studentRepository = studentRepository;
        this.eligibilityService = eligibilityService;
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public List<Company> getActiveCompanies() {
        return companyRepository.findByActiveTrue();
    }

    public Optional<Company> getCompanyById(Long id) {
        return companyRepository.findById(id);
    }

    /**
     * Registers a new visiting company.
     */
    @Transactional
    public Company createCompany(Company company) {
        // If no code is entered, generate a simple code like CMP-101
        if (company.getCompanyCode() == null || company.getCompanyCode().trim().isEmpty()) {
            long randomNum = (System.currentTimeMillis() % 9000) + 1000;
            company.setCompanyCode("CMP-" + randomNum);
        }

        // Parse required skills entered in form (e.g. "Java, SQL")
        if (company.getRequiredSkillNames() != null) {
            company.setRequiredSkills(parseSkills(company.getRequiredSkillNames()));
        }

        return companyRepository.save(company);
    }

    /**
     * Updates an existing company's recruitment criteria.
     */
    @Transactional
    public Company updateCompany(Long id, Company incoming) {
        Optional<Company> companyOpt = companyRepository.findById(id);
        if (!companyOpt.isPresent()) {
            throw new NoSuchElementException("Company not found with ID: " + id);
        }
        Company company = companyOpt.get();

        company.setName(incoming.getName().trim());
        company.setJobRole(incoming.getJobRole().trim());
        company.setMinCgpa(incoming.getMinCgpa());
        company.setMaxBacklogs(incoming.getMaxBacklogs());
        company.setSkillMatchingMode(incoming.getSkillMatchingMode());
        company.setEligibleDepartments(incoming.getEligibleDepartments());
        company.setEligibleGraduationYear(incoming.getEligibleGraduationYear());

        boolean isActive = (incoming.getActive() != null) ? incoming.getActive() : true;
        company.setActive(isActive);

        if (incoming.getRequiredSkillNames() != null) {
            company.setRequiredSkills(parseSkills(incoming.getRequiredSkillNames()));
        }

        return companyRepository.save(company);
    }

    /**
     * Deletes a company by ID.
     */
    @Transactional
    public void deleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new NoSuchElementException("Company not found with ID: " + id);
        }
        companyRepository.deleteById(id);
    }

    /**
     * Toggles whether a company drive is currently active or closed.
     */
    @Transactional
    public Company toggleCompanyActive(Long id) {
        Optional<Company> companyOpt = companyRepository.findById(id);
        if (!companyOpt.isPresent()) {
            throw new NoSuchElementException("Company not found with ID: " + id);
        }
        Company company = companyOpt.get();

        boolean currentStatus = Boolean.TRUE.equals(company.getActive());
        company.setActive(!currentStatus);
        return companyRepository.save(company);
    }

    /**
     * Counts how many currently registered students meet all criteria for this company.
     */
    public int countEligibleCandidates(Long companyId) {
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (!companyOpt.isPresent()) {
            throw new NoSuchElementException("Company not found with ID: " + companyId);
        }
        Company company = companyOpt.get();

        List<Student> students = studentRepository.findAll();
        int eligibleCount = 0;

        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            EligibilityService.EvaluationResult res = eligibilityService.evaluate(s, company);
            if (res.isEligible()) {
                eligibleCount++;
            }
        }

        return eligibleCount;
    }

    /**
     * Splits comma-separated skills and retrieves or creates Skill entities.
     */
    public Set<Skill> parseSkills(String skillsString) {
        Set<Skill> result = new HashSet<>();
        if (skillsString == null || skillsString.trim().isEmpty()) {
            return result;
        }

        String[] parts = skillsString.split("[,;]");
        for (int i = 0; i < parts.length; i++) {
            String name = parts[i].trim();
            if (!name.isEmpty()) {
                Optional<Skill> existing = skillRepository.findByNameIgnoreCase(name);
                Skill skill;
                if (existing.isPresent()) {
                    skill = existing.get();
                } else {
                    skill = skillRepository.save(new Skill(name));
                }
                result.add(skill);
            }
        }
        return result;
    }
}
