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
 * Manages visiting companies, eligibility requirements, and real-time eligible preview.
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

    @Transactional
    public Company createCompany(Company company) {
        if (company.getCompanyCode() == null || company.getCompanyCode().trim().isEmpty()) {
            company.setCompanyCode("CMP-" + (System.currentTimeMillis() % 10000));
        }

        if (company.getRequiredSkillNames() != null) {
            company.setRequiredSkills(parseSkills(company.getRequiredSkillNames()));
        }

        return companyRepository.save(company);
    }

    @Transactional
    public Company updateCompany(Long id, Company incoming) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Company not found with ID: " + id));

        company.setName(incoming.getName().trim());
        company.setJobRole(incoming.getJobRole().trim());
        company.setMinCgpa(incoming.getMinCgpa());
        company.setMaxBacklogs(incoming.getMaxBacklogs());
        company.setSkillMatchingMode(incoming.getSkillMatchingMode());
        company.setEligibleDepartments(incoming.getEligibleDepartments());
        company.setEligibleGraduationYear(incoming.getEligibleGraduationYear());
        company.setActive(incoming.getActive() != null ? incoming.getActive() : true);

        if (incoming.getRequiredSkillNames() != null) {
            company.setRequiredSkills(parseSkills(incoming.getRequiredSkillNames()));
        }

        return companyRepository.save(company);
    }

    @Transactional
    public void deleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new NoSuchElementException("Company not found with ID: " + id);
        }
        companyRepository.deleteById(id);
    }

    @Transactional
    public Company toggleCompanyActive(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Company not found with ID: " + id));
        company.setActive(!Boolean.TRUE.equals(company.getActive()));
        return companyRepository.save(company);
    }

    public int countEligibleCandidates(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NoSuchElementException("Company not found with ID: " + companyId));

        List<Student> students = studentRepository.findAll();
        int eligible = 0;
        for (Student s : students) {
            EligibilityService.EvaluationResult res = eligibilityService.evaluate(s, company);
            if (res.isEligible()) {
                eligible++;
            }
        }
        return eligible;
    }

    public Set<Skill> parseSkills(String skillsString) {
        Set<Skill> result = new HashSet<>();
        if (skillsString == null || skillsString.trim().isEmpty()) {
            return result;
        }

        String[] parts = skillsString.split("[,;]");
        for (String part : parts) {
            String name = part.trim();
            if (!name.isEmpty()) {
                Skill skill = skillRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> skillRepository.save(new Skill(name)));
                result.add(skill);
            }
        }
        return result;
    }
}
