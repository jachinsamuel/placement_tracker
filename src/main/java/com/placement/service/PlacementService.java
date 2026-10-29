package com.placement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.placement.model.*;
import com.placement.repository.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Single Unified Service for the Campus Placement Portal.
 * 
 * Beginner Java Explanation:
 * Instead of splitting logic across 6 different complicated service files,
 * this single class holds all placement calculations, rules, and database operations:
 *   - Section 1: Student Management (Add, update, search, delete, CSV import)
 *   - Section 2: Company Management (Add, update, criteria, real-time count)
 *   - Section 3: Eligibility Rule Engine (CGPA, backlogs, department, batch, skills)
 *   - Section 4: Ranking Engine (Weighted score 0-100 and CGPA tie-breaker)
 *   - Section 5: Shortlist Generation & Reports (CSV and JSON download)
 *   - Section 6: Dashboard Statistics (Counts and chart data)
 */
@Service
public class PlacementService {

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final ShortlistRepository shortlistRepository;
    private final SkillRepository skillRepository;
    private final ObjectMapper objectMapper;

    public PlacementService(StudentRepository studentRepository,
                            CompanyRepository companyRepository,
                            ShortlistRepository shortlistRepository,
                            SkillRepository skillRepository) {
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.shortlistRepository = shortlistRepository;
        this.skillRepository = skillRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // =========================================================================
    // SECTION 1: STUDENT MANAGEMENT
    // =========================================================================

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    @Transactional
    public Student createStudent(Student student) {
        String stuId = (student.getStudentId() != null) ? student.getStudentId().trim() : "";
        if (stuId.isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be empty");
        }
        if (studentRepository.existsByStudentId(stuId)) {
            throw new IllegalArgumentException("Student ID '" + stuId + "' already exists");
        }

        String email = (student.getEmail() != null) ? student.getEmail().trim() : "";
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be empty");
        }
        if (studentRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email address '" + email + "' is already registered");
        }

        if (student.getSkillNames() != null) {
            student.setSkills(parseSkills(student.getSkillNames()));
        }

        return studentRepository.save(student);
    }

    @Transactional
    public Student updateStudent(Long id, Student incomingData) {
        Optional<Student> studentOpt = studentRepository.findById(id);
        if (!studentOpt.isPresent()) {
            throw new NoSuchElementException("Student not found with ID: " + id);
        }
        Student student = studentOpt.get();

        String newStudentId = incomingData.getStudentId().trim();
        String newEmail = incomingData.getEmail().trim();

        if (!student.getStudentId().equalsIgnoreCase(newStudentId) && studentRepository.existsByStudentId(newStudentId)) {
            throw new IllegalArgumentException("Student ID '" + newStudentId + "' is already in use");
        }

        if (!student.getEmail().equalsIgnoreCase(newEmail) && studentRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email '" + newEmail + "' is already in use");
        }

        student.setStudentId(newStudentId);
        student.setFullName(incomingData.getFullName().trim());
        student.setDepartment(incomingData.getDepartment().trim());
        student.setCgpa(incomingData.getCgpa());
        student.setActiveBacklogs(incomingData.getActiveBacklogs());
        student.setEmail(newEmail);
        student.setGraduationYear(incomingData.getGraduationYear());

        double certScore = (incomingData.getCertificationsScore() != null) ? incomingData.getCertificationsScore() : 0.0;
        student.setCertificationsScore(certScore);

        if (incomingData.getSkillNames() != null) {
            student.setSkills(parseSkills(incomingData.getSkillNames()));
        }

        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new NoSuchElementException("Student not found with ID: " + id);
        }
        studentRepository.deleteById(id);
    }

    public List<Student> searchStudents(String query) {
        if (query == null || query.trim().isEmpty()) {
            return studentRepository.findAll();
        }
        String clean = query.trim();
        return studentRepository.findByFullNameContainingIgnoreCaseOrStudentIdContainingIgnoreCase(clean, clean);
    }

    public List<Student> filterByDepartment(String department) {
        if (department == null || department.trim().isEmpty() || "ALL".equalsIgnoreCase(department.trim())) {
            return studentRepository.findAll();
        }
        return studentRepository.findByDepartmentIgnoreCase(department.trim());
    }

    @Transactional
    public int importStudentsFromCsv(InputStream inputStream) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            CSVFormat format = CSVFormat.Builder.create()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            CSVParser parser = format.parse(reader);

            for (CSVRecord record : parser) {
                String studentId = getCsvValue(record, "studentId", "id");
                String fullName = getCsvValue(record, "fullName", "name");
                String department = getCsvValue(record, "department", "dept");
                String cgpaStr = getCsvValue(record, "cgpa", "gpa");
                String backlogsStr = getCsvValue(record, "activeBacklogs", "backlogs");
                String email = getCsvValue(record, "email", "mail");
                String gradYearStr = getCsvValue(record, "graduationYear", "year");
                String skillsStr = getCsvValue(record, "skills");
                String certScoreStr = getCsvValue(record, "certificationsScore", "certScore");

                if (studentId.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
                    continue;
                }

                double cgpa = 0.0;
                try { cgpa = Double.parseDouble(cgpaStr); } catch (Exception ignored) {}

                int backlogs = 0;
                try { backlogs = Integer.parseInt(backlogsStr); } catch (Exception ignored) {}

                int gradYear = 2026;
                try { gradYear = Integer.parseInt(gradYearStr); } catch (Exception ignored) {}

                double certScore = 0.0;
                try { certScore = Double.parseDouble(certScoreStr); } catch (Exception ignored) {}

                Optional<Student> existingOpt = studentRepository.findByStudentId(studentId);
                Student student;
                if (existingOpt.isPresent()) {
                    student = existingOpt.get();
                    student.setFullName(fullName);
                    student.setDepartment(department);
                    student.setCgpa(cgpa);
                    student.setActiveBacklogs(backlogs);
                    student.setEmail(email);
                    student.setGraduationYear(gradYear);
                } else {
                    student = new Student(studentId, fullName, department, cgpa, backlogs, email, gradYear);
                }
                student.setCertificationsScore(certScore);

                if (!skillsStr.isEmpty()) {
                    student.setSkills(parseSkills(skillsStr));
                }

                studentRepository.save(student);
                count++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading CSV file: " + e.getMessage(), e);
        }
        return count;
    }

    private String getCsvValue(CSVRecord record, String... headers) {
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i];
            if (record.isMapped(h)) {
                String val = record.get(h);
                if (val != null) {
                    return val.trim();
                }
            }
        }
        return "";
    }

    // =========================================================================
    // SECTION 2: COMPANY MANAGEMENT
    // =========================================================================

    @Transactional
    public int importCompaniesFromCsv(InputStream inputStream) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            CSVFormat format = CSVFormat.Builder.create()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            CSVParser parser = format.parse(reader);

            for (CSVRecord record : parser) {
                String companyCode = getCsvValue(record, "companyCode", "code");
                String name = getCsvValue(record, "name", "companyName");
                String jobRole = getCsvValue(record, "jobRole", "role");
                String minCgpaStr = getCsvValue(record, "minCgpa", "cgpa");
                String maxBacklogsStr = getCsvValue(record, "maxBacklogs", "backlogs");
                String skillMatchingModeStr = getCsvValue(record, "skillMatchingMode", "skillMode", "mode");
                String requiredSkills = getCsvValue(record, "requiredSkills", "skills");
                String eligibleDepartments = getCsvValue(record, "eligibleDepartments", "departments", "dept");
                String eligibleGradYearStr = getCsvValue(record, "eligibleGraduationYear", "graduationYear", "batch", "year");

                if (name.isEmpty() || jobRole.isEmpty()) {
                    continue;
                }

                double minCgpa = 0.0;
                try { minCgpa = Double.parseDouble(minCgpaStr); } catch (Exception ignored) {}

                int maxBacklogs = 0;
                try { maxBacklogs = Integer.parseInt(maxBacklogsStr); } catch (Exception ignored) {}

                SkillMatchingMode mode = SkillMatchingMode.ALL;
                if ("ANY".equalsIgnoreCase(skillMatchingModeStr)) {
                    mode = SkillMatchingMode.ANY;
                }

                Integer gradYear = null;
                try {
                    if (!eligibleGradYearStr.isEmpty()) {
                        gradYear = Integer.parseInt(eligibleGradYearStr);
                    }
                } catch (Exception ignored) {}

                Optional<Company> existingComp = companyRepository.findByNameIgnoreCase(name);
                Company company;
                if (existingComp.isPresent()) {
                    company = existingComp.get();
                    company.setJobRole(jobRole);
                    company.setMinCgpa(minCgpa);
                    company.setMaxBacklogs(maxBacklogs);
                    company.setSkillMatchingMode(mode);
                    company.setEligibleDepartments(eligibleDepartments.isEmpty() ? null : eligibleDepartments);
                    company.setEligibleGraduationYear(gradYear);
                } else {
                    company = new Company(name, jobRole, minCgpa, maxBacklogs, mode,
                            eligibleDepartments.isEmpty() ? null : eligibleDepartments, gradYear);
                    if (!companyCode.isEmpty()) {
                        company.setCompanyCode(companyCode);
                    } else {
                        long randomNum = (System.currentTimeMillis() % 9000) + 1000 + count;
                        company.setCompanyCode("CMP-" + randomNum);
                    }
                }

                if (!requiredSkills.isEmpty()) {
                    company.setRequiredSkills(parseSkills(requiredSkills));
                }

                companyRepository.save(company);
                count++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading CSV file: " + e.getMessage(), e);
        }
        return count;
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
            long randomNum = (System.currentTimeMillis() % 9000) + 1000;
            company.setCompanyCode("CMP-" + randomNum);
        }

        if (company.getRequiredSkillNames() != null) {
            company.setRequiredSkills(parseSkills(company.getRequiredSkillNames()));
        }

        return companyRepository.save(company);
    }

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

    @Transactional
    public void deleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new NoSuchElementException("Company not found with ID: " + id);
        }
        companyRepository.deleteById(id);
    }

    @Transactional
    public Company toggleCompanyActive(Long id) {
        Optional<Company> companyOpt = companyRepository.findById(id);
        if (!companyOpt.isPresent()) {
            throw new NoSuchElementException("Company not found with ID: " + id);
        }
        Company company = companyOpt.get();
        boolean current = Boolean.TRUE.equals(company.getActive());
        company.setActive(!current);
        return companyRepository.save(company);
    }

    public int countEligibleCandidates(Long companyId) {
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (!companyOpt.isPresent()) {
            throw new NoSuchElementException("Company not found with ID: " + companyId);
        }
        Company company = companyOpt.get();

        List<Student> students = studentRepository.findAll();
        int eligibleCount = 0;
        for (int i = 0; i < students.size(); i++) {
            EvaluationResult res = evaluateEligibility(students.get(i), company);
            if (res.isEligible()) {
                eligibleCount++;
            }
        }
        return eligibleCount;
    }

    // =========================================================================
    // SECTION 3: ELIGIBILITY RULE ENGINE
    // =========================================================================

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

        public boolean isEligible() { return eligible; }
        public List<String> getRejectionReasons() { return rejectionReasons; }
        public Set<Skill> getMatchedSkills() { return matchedSkills; }
        public double getSkillMatchPercentage() { return skillMatchPercentage; }

        public String getRejectionReasonsSummary() {
            if (rejectionReasons == null || rejectionReasons.isEmpty()) return "";
            return String.join("; ", rejectionReasons);
        }

        public String getMatchedSkillsSummary() {
            if (matchedSkills == null || matchedSkills.isEmpty()) return "None";
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
     * Checks if a student satisfies all 5 company criteria.
     */
    public EvaluationResult evaluateEligibility(Student student, Company company) {
        List<String> reasons = new ArrayList<>();

        if (student == null) {
            reasons.add("Student record is missing");
            return new EvaluationResult(false, reasons, Collections.emptySet(), 0.0);
        }
        if (company == null) {
            reasons.add("Company criteria is missing");
            return new EvaluationResult(false, reasons, Collections.emptySet(), 0.0);
        }

        // Rule 1: Minimum CGPA
        Double cgpa = student.getCgpa();
        if (cgpa == null) {
            reasons.add("Student CGPA is missing");
        } else if (cgpa < 0.0 || cgpa > 10.0) {
            reasons.add("Invalid CGPA (" + cgpa + "): must be between 0.0 and 10.0");
        } else if (cgpa < company.getMinCgpa()) {
            reasons.add("CGPA (" + cgpa + ") is below required minimum (" + company.getMinCgpa() + ")");
        }

        // Rule 2: Active Backlogs
        Integer backlogs = student.getActiveBacklogs();
        if (backlogs == null) {
            reasons.add("Student backlogs record is missing");
        } else if (backlogs < 0) {
            reasons.add("Active backlogs cannot be negative (" + backlogs + ")");
        } else if (backlogs > company.getMaxBacklogs()) {
            reasons.add("Active backlogs (" + backlogs + ") exceed permitted limit (" + company.getMaxBacklogs() + ")");
        }

        // Rule 3: Department Check
        String eligibleDepts = company.getEligibleDepartments();
        if (eligibleDepts != null && !eligibleDepts.trim().isEmpty()) {
            String studentDept = (student.getDepartment() != null) ? student.getDepartment().trim() : "";
            String[] allowedArray = eligibleDepts.split(",");
            boolean allowed = false;
            for (int i = 0; i < allowedArray.length; i++) {
                if (allowedArray[i].trim().equalsIgnoreCase(studentDept)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                reasons.add("Department '" + studentDept + "' is not in eligible departments: [" + eligibleDepts.trim() + "]");
            }
        }

        // Rule 4: Graduation Batch Year
        Integer reqYear = company.getEligibleGraduationYear();
        if (reqYear != null && reqYear > 0) {
            Integer stuYear = student.getGraduationYear();
            if (stuYear == null || !stuYear.equals(reqYear)) {
                reasons.add("Graduation year (" + stuYear + ") does not match required year (" + reqYear + ")");
            }
        }

        // Rule 5: Technical Skills (ALL vs ANY mode)
        Set<Skill> compSkills = (company.getRequiredSkills() != null) ? company.getRequiredSkills() : Collections.emptySet();
        Set<Skill> stuSkills = (student.getSkills() != null) ? student.getSkills() : Collections.emptySet();

        Set<Skill> matched = new HashSet<>();
        List<String> missing = new ArrayList<>();

        for (Skill req : compSkills) {
            boolean has = false;
            for (Skill possessed : stuSkills) {
                if (possessed.getName().equalsIgnoreCase(req.getName())) {
                    has = true;
                    matched.add(req);
                    break;
                }
            }
            if (!has) {
                missing.add(req.getName());
            }
        }

        double matchPercent = 100.0;
        if (!compSkills.isEmpty()) {
            matchPercent = ((double) matched.size() / compSkills.size()) * 100.0;
            matchPercent = Math.round(matchPercent * 100.0) / 100.0;

            if (company.getSkillMatchingMode() == SkillMatchingMode.ALL) {
                if (!missing.isEmpty()) {
                    reasons.add("Missing required skills for ALL mode: " + String.join(", ", missing));
                }
            } else {
                if (matched.isEmpty()) {
                    List<String> names = new ArrayList<>();
                    for (Skill s : compSkills) names.add(s.getName());
                    reasons.add("Must possess at least one of required skills: " + String.join(", ", names));
                }
            }
        }

        boolean isEligible = reasons.isEmpty();
        return new EvaluationResult(isEligible, reasons, matched, matchPercent);
    }

    // =========================================================================
    // SECTION 4: CANDIDATE RANKING ENGINE
    // =========================================================================

    public static class RankedCandidate {
        private final Student student;
        private final double skillMatchPercentage;
        private double rankingScore;
        private int rank;

        public RankedCandidate(Student student, double skillMatchPercentage) {
            this.student = student;
            this.skillMatchPercentage = skillMatchPercentage;
            this.rankingScore = 0.0;
            this.rank = 0;
        }

        public Student getStudent() { return student; }
        public double getSkillMatchPercentage() { return skillMatchPercentage; }
        public double getRankingScore() { return rankingScore; }
        public void setRankingScore(double s) { this.rankingScore = s; }
        public int getRank() { return rank; }
        public void setRank(int r) { this.rank = r; }
    }

    public double calculateRankingScore(Student student, double skillMatchPercentage,
                                        double cgpaWeight, double skillsWeight, double certsWeight) {
        double cgpaScore = 0.0;
        if (student.getCgpa() != null) {
            cgpaScore = (student.getCgpa() / 10.0) * 100.0;
            if (cgpaScore > 100.0) cgpaScore = 100.0;
            if (cgpaScore < 0.0) cgpaScore = 0.0;
        }

        double skillsScore = skillMatchPercentage;
        if (skillsScore > 100.0) skillsScore = 100.0;
        if (skillsScore < 0.0) skillsScore = 0.0;

        double certsScore = 0.0;
        if (student.getCertificationsScore() != null) {
            certsScore = student.getCertificationsScore();
            if (certsScore > 100.0) certsScore = 100.0;
            if (certsScore < 0.0) certsScore = 0.0;
        }

        double total = (cgpaScore * (cgpaWeight / 100.0))
                     + (skillsScore * (skillsWeight / 100.0))
                     + (certsScore * (certsWeight / 100.0));

        return Math.round(total * 100.0) / 100.0;
    }

    public List<RankedCandidate> rankCandidates(List<RankedCandidate> candidates,
                                                double cgpaWeight, double skillsWeight, double certsWeight) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        for (int i = 0; i < candidates.size(); i++) {
            RankedCandidate c = candidates.get(i);
            double score = calculateRankingScore(c.getStudent(), c.getSkillMatchPercentage(),
                    cgpaWeight, skillsWeight, certsWeight);
            c.setRankingScore(score);
        }

        candidates.sort(new Comparator<RankedCandidate>() {
            @Override
            public int compare(RankedCandidate c1, RankedCandidate c2) {
                // Rank candidates purely by highest CGPA first
                double cgpa1 = (c1.getStudent().getCgpa() != null) ? c1.getStudent().getCgpa() : 0.0;
                double cgpa2 = (c2.getStudent().getCgpa() != null) ? c2.getStudent().getCgpa() : 0.0;
                if (cgpa2 > cgpa1) return 1;
                if (cgpa2 < cgpa1) return -1;

                // Tie-breaker: Student ID alphabetical
                String id1 = (c1.getStudent().getStudentId() != null) ? c1.getStudent().getStudentId() : "";
                String id2 = (c2.getStudent().getStudentId() != null) ? c2.getStudent().getStudentId() : "";
                return id1.compareTo(id2);
            }
        });

        int currentRank = 1;
        for (int i = 0; i < candidates.size(); i++) {
            candidates.get(i).setRank(currentRank);
            currentRank++;
        }

        return candidates;
    }

    // =========================================================================
    // SECTION 5: SHORTLIST GENERATION & FILE EXPORT
    // =========================================================================

    @Transactional
    public Shortlist generateShortlist(Long companyId, Double cgpaWeight, Double skillsWeight, Double certsWeight) {
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (!companyOpt.isPresent()) {
            throw new NoSuchElementException("Company not found with ID: " + companyId);
        }
        Company company = companyOpt.get();

        double wCgpa = (cgpaWeight != null) ? cgpaWeight : 60.0;
        double wSkills = (skillsWeight != null) ? skillsWeight : 30.0;
        double wCerts = (certsWeight != null) ? certsWeight : 10.0;

        if (Math.abs((wCgpa + wSkills + wCerts) - 100.0) > 0.001) {
            throw new IllegalArgumentException("Ranking weights must add up to exactly 100%");
        }

        List<Student> allStudents = studentRepository.findAll();

        Shortlist shortlist = new Shortlist();
        shortlist.setCompanyId(company.getId());
        shortlist.setCompanyName(company.getName());
        shortlist.setJobRole(company.getJobRole());
        shortlist.setGeneratedAt(LocalDateTime.now());
        shortlist.setCgpaWeight(wCgpa);
        shortlist.setSkillsWeight(wSkills);
        shortlist.setCertsWeight(wCerts);
        shortlist.setTotalEvaluated(allStudents.size());

        String criteriaSnapshot = "Min CGPA: " + company.getMinCgpa()
                + " | Max Backlogs: " + company.getMaxBacklogs()
                + " | Skills: [" + company.getRequiredSkillsSummary() + "] (" + company.getSkillMatchingMode() + ")"
                + (company.getEligibleDepartments() != null ? " | Depts: [" + company.getEligibleDepartments() + "]" : "")
                + (company.getEligibleGraduationYear() != null ? " | Batch: " + company.getEligibleGraduationYear() : "");
        shortlist.setCriteriaSummary(criteriaSnapshot);

        List<RankedCandidate> eligibleContainers = new ArrayList<>();
        List<ShortlistCandidate> ineligibleCandidates = new ArrayList<>();

        for (int i = 0; i < allStudents.size(); i++) {
            Student student = allStudents.get(i);
            EvaluationResult eval = evaluateEligibility(student, company);

            if (eval.isEligible()) {
                eligibleContainers.add(new RankedCandidate(student, eval.getSkillMatchPercentage()));
            } else {
                ShortlistCandidate inelig = new ShortlistCandidate();
                inelig.setStudentId(student.getStudentId());
                inelig.setStudentName(student.getFullName());
                inelig.setDepartment(student.getDepartment());
                inelig.setCgpa(student.getCgpa());
                inelig.setActiveBacklogs(student.getActiveBacklogs());
                inelig.setSkillsSummary(student.getSkillsSummary());
                inelig.setMatchedSkillsSummary(eval.getMatchedSkillsSummary());
                inelig.setSkillMatchPercentage(eval.getSkillMatchPercentage());
                inelig.setRankingScore(0.0);
                inelig.setRankNumber(null);
                inelig.setEligible(false);
                inelig.setRejectionReasons(eval.getRejectionReasonsSummary());
                ineligibleCandidates.add(inelig);
            }
        }

        List<RankedCandidate> ranked = rankCandidates(eligibleContainers, wCgpa, wSkills, wCerts);

        for (int i = 0; i < ranked.size(); i++) {
            RankedCandidate rc = ranked.get(i);
            Student s = rc.getStudent();

            ShortlistCandidate candidate = new ShortlistCandidate();
            candidate.setStudentId(s.getStudentId());
            candidate.setStudentName(s.getFullName());
            candidate.setDepartment(s.getDepartment());
            candidate.setCgpa(s.getCgpa());
            candidate.setActiveBacklogs(s.getActiveBacklogs());
            candidate.setSkillsSummary(s.getSkillsSummary());

            EvaluationResult eval = evaluateEligibility(s, company);
            candidate.setMatchedSkillsSummary(eval.getMatchedSkillsSummary());
            candidate.setSkillMatchPercentage(rc.getSkillMatchPercentage());
            candidate.setRankingScore(rc.getRankingScore());
            candidate.setRankNumber(rc.getRank());
            candidate.setEligible(true);
            candidate.setRejectionReasons("");

            shortlist.addCandidate(candidate);
        }

        for (int i = 0; i < ineligibleCandidates.size(); i++) {
            shortlist.addCandidate(ineligibleCandidates.get(i));
        }

        shortlist.setEligibleCount(ranked.size());
        shortlist.setIneligibleCount(ineligibleCandidates.size());

        return shortlistRepository.save(shortlist);
    }

    public List<Shortlist> getAllShortlists() {
        return shortlistRepository.findAllByOrderByGeneratedAtDesc();
    }

    public Shortlist getShortlistById(Long id) {
        Optional<Shortlist> opt = shortlistRepository.findById(id);
        if (!opt.isPresent()) {
            throw new NoSuchElementException("Shortlist not found with ID: " + id);
        }
        return opt.get();
    }

    public void exportToCsv(Long shortlistId, Writer writer) {
        Shortlist shortlist = getShortlistById(shortlistId);
        try {
            CSVFormat csvFormat = CSVFormat.Builder.create()
                    .setHeader("Rank", "Student ID", "Full Name", "Department", "CGPA",
                            "Active Backlogs", "All Skills", "Matched Skills", "Skill Match %",
                            "Ranking Score", "Eligibility Status", "Rejection Reasons")
                    .build();

            try (CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
                List<ShortlistCandidate> candidates = shortlist.getCandidates();
                for (int i = 0; i < candidates.size(); i++) {
                    ShortlistCandidate c = candidates.get(i);
                    printer.printRecord(
                            c.getRankNumber() != null ? c.getRankNumber() : "-",
                            c.getStudentId(),
                            c.getStudentName(),
                            c.getDepartment(),
                            c.getCgpa(),
                            c.getActiveBacklogs(),
                            c.getSkillsSummary(),
                            c.getMatchedSkillsSummary(),
                            String.format("%.1f%%", c.getSkillMatchPercentage()),
                            Boolean.TRUE.equals(c.getEligible()) ? c.getRankingScore() : "-",
                            Boolean.TRUE.equals(c.getEligible()) ? "ELIGIBLE" : "NOT ELIGIBLE",
                            c.getRejectionReasons() != null ? c.getRejectionReasons() : ""
                    );
                }
                printer.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error exporting CSV: " + e.getMessage(), e);
        }
    }

    public String exportToJson(Long shortlistId) {
        Shortlist shortlist = getShortlistById(shortlistId);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(shortlist);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing JSON: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // SECTION 6: DASHBOARD STATS
    // =========================================================================

    public Map<String, Object> getOverallStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalStudents = studentRepository.count();
        long totalCompanies = companyRepository.count();
        List<Shortlist> allShortlists = shortlistRepository.findAllByOrderByGeneratedAtDesc();
        long totalShortlists = allShortlists.size();

        int latestEligibleCount = 0;
        String latestShortlistCompany = "None";
        String latestShortlistTime = "-";

        if (!allShortlists.isEmpty()) {
            Shortlist latest = allShortlists.get(0);
            latestEligibleCount = latest.getEligibleCount();
            latestShortlistCompany = latest.getCompanyName();
            if (latest.getGeneratedAt() != null) {
                latestShortlistTime = latest.getGeneratedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
            }
        }

        stats.put("totalStudents", totalStudents);
        stats.put("totalCompanies", totalCompanies);
        stats.put("totalShortlists", totalShortlists);
        stats.put("latestEligibleCount", latestEligibleCount);
        stats.put("latestShortlistCompany", latestShortlistCompany);
        stats.put("latestShortlistTime", latestShortlistTime);

        return stats;
    }

    public List<Map<String, Object>> getDepartmentDistribution() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Object[]> rows = studentRepository.countStudentsByDepartment();

        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            String dept = (String) row[0];
            Long count = (Long) row[1];

            Map<String, Object> item = new HashMap<>();
            item.put("department", (dept != null) ? dept : "Unknown");
            item.put("count", count);
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getCompanyEligibilityStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Company> activeCompanies = companyRepository.findByActiveTrue();

        for (int i = 0; i < activeCompanies.size(); i++) {
            Company company = activeCompanies.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("id", company.getId());
            item.put("name", company.getName());
            item.put("jobRole", company.getJobRole());
            item.put("minCgpa", company.getMinCgpa());
            item.put("eligibleCount", countEligibleCandidates(company.getId()));
            result.add(item);
        }
        return result;
    }

    // =========================================================================
    // HELPER: PARSE SKILLS
    // =========================================================================

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
