package com.placement.cli;

import com.placement.model.*;
import com.placement.service.PlacementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Interactive Command Line Interface (CLI) Console Runner.
 * Allows placement officers or college examiners to manage students,
 * companies, and generate ranked shortlists directly inside the terminal.
 */
@Component
public class ConsoleMenuRunner implements CommandLineRunner {

    private final PlacementService placementService;

    @Value("${placement.cli.enabled:true}")
    private boolean cliEnabled;

    public ConsoleMenuRunner(PlacementService placementService) {
        this.placementService = placementService;
    }

    @Override
    public void run(String... args) {
        if (!cliEnabled) {
            return;
        }

        Thread consoleThread = new Thread(this::startInteractiveLoop, "Interactive-Console-Thread");
        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    private void startInteractiveLoop() {
        try {
            Thread.sleep(1200);
        } catch (InterruptedException ignored) {}

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("\n");
        System.out.println("============================================================================");
        System.out.println("   CAMPUS PLACEMENT SHORTLISTING SYSTEM - INTERACTIVE TERMINAL CONSOLE");
        System.out.println("   (Web Portal running simultaneously at http://localhost:8080)");
        System.out.println("============================================================================");

        while (running) {
            printMenu();
            System.out.print("Enter choice [1-8]: ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleAddStudent(scanner);
                    break;
                case "2":
                    handleViewAllStudents();
                    break;
                case "3":
                    handleAddCompany(scanner);
                    break;
                case "4":
                    handleViewAllCompanies();
                    break;
                case "5":
                    handleGenerateShortlist(scanner);
                    break;
                case "6":
                    handleViewShortlistHistory();
                    break;
                case "7":
                    handleViewDashboard();
                    break;
                case "8":
                case "q":
                case "exit":
                case "quit":
                    System.out.println("\n[!] Shutting down Campus Placement Portal...");
                    System.out.println("[*] Goodbye!\n");
                    System.exit(0);
                    break;
                default:
                    System.out.println("\n[X] Invalid option. Please enter a number between 1 and 8 (or 'q' to exit).\n");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--------------------------------------------------------------");
        System.out.println(" [1] Add New Student");
        System.out.println(" [2] View All Registered Students");
        System.out.println(" [3] Add New Visiting Company");
        System.out.println(" [4] View All Visiting Companies");
        System.out.println(" [5] Run Shortlisting & Ranking Engine");
        System.out.println(" [6] View Historical Shortlists");
        System.out.println(" [7] View Placement Analytics Summary");
        System.out.println(" [8] Exit & Shutdown Application");
        System.out.println("--------------------------------------------------------------");
    }

    // =========================================================================
    // 1. Add Student
    // =========================================================================
    private void handleAddStudent(Scanner scanner) {
        System.out.println("\n--- [ADD NEW STUDENT] ---");

        System.out.print("Student ID (e.g. URK25CS1001): ");
        String id = readNonEmpty(scanner, "Student ID cannot be empty");

        System.out.print("Full Name: ");
        String name = readNonEmpty(scanner, "Name cannot be empty");

        System.out.print("Department (CSE/IT/ECE/MECH/CIVIL/EEE) [Default: CSE]: ");
        String dept = scanner.nextLine().trim();
        if (dept.isEmpty()) dept = "CSE";

        double cgpa = readDouble(scanner, "CGPA (0.0 - 10.0): ", 0.0, 10.0);
        int backlogs = readInt(scanner, "Active Backlogs (0 or more): ", 0, 100);

        System.out.print("Email Address (e.g. student@karunya.edu.in): ");
        String email = readNonEmpty(scanner, "Email cannot be empty");

        int gradYear = readInt(scanner, "Graduation Year (e.g. 2027): ", 2000, 2035);

        System.out.print("Technical Skills (comma-separated, e.g. Java, Python, SQL): ");
        String skills = scanner.nextLine().trim();

        double certs = readDouble(scanner, "Certifications/Achievements Score (0 - 100) [Default: 0]: ", 0.0, 100.0);

        try {
            Student student = new Student(id, name, dept, cgpa, backlogs, email, gradYear);
            student.setSkillNames(skills);
            student.setCertificationsScore(certs);

            Student saved = placementService.createStudent(student);
            System.out.println("\n[+] SUCCESS: Student '" + saved.getFullName() + "' (" + saved.getStudentId() + ") registered successfully!");
        } catch (Exception ex) {
            System.out.println("\n[X] ERROR: " + ex.getMessage());
        }
    }

    // =========================================================================
    // 2. View All Students
    // =========================================================================
    private void handleViewAllStudents() {
        System.out.println("\n--- [REGISTERED STUDENTS DIRECTORY] ---");
        List<Student> students = placementService.getAllStudents();

        if (students.isEmpty()) {
            System.out.println("No students registered yet. Use Option [1] to add a student or import via Web UI.");
            return;
        }

        System.out.printf("%-12s | %-20s | %-6s | %-5s | %-8s | %-6s | %s\n",
                "Student ID", "Full Name", "Dept", "CGPA", "Backlogs", "Batch", "Technical Skills");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (Student s : students) {
            List<String> skillList = new ArrayList<>();
            if (s.getSkills() != null) {
                for (Skill sk : s.getSkills()) {
                    skillList.add(sk.getName());
                }
            }
            String skillStr = skillList.isEmpty() ? "None" : String.join(", ", skillList);

            System.out.printf("%-12s | %-20s | %-6s | %-5.2f | %-8d | %-6d | %s\n",
                    s.getStudentId(),
                    truncate(s.getFullName(), 20),
                    s.getDepartment(),
                    s.getCgpa(),
                    s.getActiveBacklogs(),
                    s.getGraduationYear(),
                    skillStr);
        }
        System.out.println("Total Registered Students: " + students.size());
    }

    // =========================================================================
    // 3. Add Company
    // =========================================================================
    private void handleAddCompany(Scanner scanner) {
        System.out.println("\n--- [REGISTER VISITING COMPANY] ---");

        System.out.print("Company Code (e.g. TCS-2027) [Press enter for auto-code]: ");
        String code = scanner.nextLine().trim();

        System.out.print("Company Name: ");
        String name = readNonEmpty(scanner, "Company name cannot be empty");

        System.out.print("Job Role (e.g. Software Engineer): ");
        String role = readNonEmpty(scanner, "Job role cannot be empty");

        double minCgpa = readDouble(scanner, "Minimum CGPA Cutoff (0.0 - 10.0): ", 0.0, 10.0);
        int maxBacklogs = readInt(scanner, "Maximum Permitted Active Backlogs: ", 0, 100);

        System.out.print("Skill Matching Mode [1 = ALL skills required, 2 = ANY skill matches] (Default: 1): ");
        String modeInput = scanner.nextLine().trim();
        SkillMatchingMode mode = "2".equals(modeInput) ? SkillMatchingMode.ANY : SkillMatchingMode.ALL;

        System.out.print("Required Technical Skills (comma-separated, e.g. Java, SQL): ");
        String requiredSkills = scanner.nextLine().trim();

        System.out.print("Eligible Departments (e.g. CSE, IT) [Press enter for All]: ");
        String depts = scanner.nextLine().trim();

        System.out.print("Eligible Batch / Year (e.g. 2027) [Press enter for Any]: ");
        String yearInput = scanner.nextLine().trim();
        Integer gradYear = null;
        if (!yearInput.isEmpty()) {
            try {
                gradYear = Integer.parseInt(yearInput);
            } catch (NumberFormatException ignored) {}
        }

        try {
            Company company = new Company();
            company.setCompanyCode(code.isEmpty() ? null : code);
            company.setName(name);
            company.setJobRole(role);
            company.setMinCgpa(minCgpa);
            company.setMaxBacklogs(maxBacklogs);
            company.setSkillMatchingMode(mode);
            company.setRequiredSkillNames(requiredSkills);
            company.setEligibleDepartments(depts.isEmpty() ? null : depts);
            company.setEligibleGraduationYear(gradYear);
            company.setActive(true);

            Company saved = placementService.createCompany(company);
            System.out.println("\n[+] SUCCESS: Company '" + saved.getName() + "' for role '" + saved.getJobRole() + "' registered successfully!");
        } catch (Exception ex) {
            System.out.println("\n[X] ERROR: " + ex.getMessage());
        }
    }

    // =========================================================================
    // 4. View All Companies
    // =========================================================================
    private void handleViewAllCompanies() {
        System.out.println("\n--- [VISITING COMPANIES DIRECTORY] ---");
        List<Company> companies = placementService.getAllCompanies();

        if (companies.isEmpty()) {
            System.out.println("No companies registered yet. Use Option [3] to register a company.");
            return;
        }

        System.out.printf("%-4s | %-12s | %-18s | %-18s | %-8s | %-8s | %-6s | %s\n",
                "No.", "Code", "Company Name", "Job Role", "Min CGPA", "Max Logs", "Mode", "Required Skills");
        System.out.println("--------------------------------------------------------------------------------------------------------");

        int idx = 1;
        for (Company c : companies) {
            List<String> skillList = new ArrayList<>();
            if (c.getRequiredSkills() != null) {
                for (Skill sk : c.getRequiredSkills()) {
                    skillList.add(sk.getName());
                }
            }
            String skillStr = skillList.isEmpty() ? "None" : String.join(", ", skillList);

            System.out.printf("%-4d | %-12s | %-18s | %-18s | %-8.2f | %-8d | %-6s | %s\n",
                    idx++,
                    c.getCompanyCode(),
                    truncate(c.getName(), 18),
                    truncate(c.getJobRole(), 18),
                    c.getMinCgpa(),
                    c.getMaxBacklogs(),
                    c.getSkillMatchingMode(),
                    skillStr);
        }
        System.out.println("Total Companies: " + companies.size());
    }

    // =========================================================================
    // 5. Generate Shortlist
    // =========================================================================
    private void handleGenerateShortlist(Scanner scanner) {
        System.out.println("\n--- [RUN SHORTLISTING & RANKING ENGINE] ---");
        List<Company> companies = placementService.getAllCompanies();

        if (companies.isEmpty()) {
            System.out.println("[X] No companies available. Please register a company first using Option [3].");
            return;
        }

        System.out.println("Select a Company to evaluate:");
        for (int i = 0; i < companies.size(); i++) {
            Company c = companies.get(i);
            System.out.printf(" [%d] %s (%s) - Min CGPA: %.1f, Max Backlogs: %d\n",
                    (i + 1), c.getName(), c.getJobRole(), c.getMinCgpa(), c.getMaxBacklogs());
        }

        int companyChoice = readInt(scanner, "Choose company number [1-" + companies.size() + "]: ", 1, companies.size());
        Company selectedCompany = companies.get(companyChoice - 1);

        System.out.println("\nConfigure Ranking Weights (Must total 100%):");
        System.out.println("Default weights: 60% CGPA, 30% Skills, 10% Certifications");
        System.out.print("Press [Enter] to use default weights, or 'C' to customize: ");
        String customChoice = scanner.nextLine().trim();

        double cgpaW = 60.0;
        double skillsW = 30.0;
        double certsW = 10.0;

        if ("C".equalsIgnoreCase(customChoice) || "custom".equalsIgnoreCase(customChoice)) {
            while (true) {
                cgpaW = readDouble(scanner, "CGPA Weight (%): ", 0.0, 100.0);
                skillsW = readDouble(scanner, "Technical Skills Weight (%): ", 0.0, 100.0);
                certsW = readDouble(scanner, "Certifications/Achievements Weight (%): ", 0.0, 100.0);

                if (Math.abs((cgpaW + skillsW + certsW) - 100.0) < 0.001) {
                    break;
                }
                System.out.println("[X] Weights must sum to exactly 100%! Current sum: " + (cgpaW + skillsW + certsW) + "%. Please try again.");
            }
        }

        System.out.println("\n[*] Evaluating all registered students against " + selectedCompany.getName() + " criteria...");

        try {
            Shortlist shortlist = placementService.generateShortlist(selectedCompany.getId(), cgpaW, skillsW, certsW);

            System.out.println("\n==============================================================");
            System.out.println("                   SHORTLIST RESULTS SUMMARY                  ");
            System.out.println("==============================================================");
            System.out.println("Company: " + shortlist.getCompanyName() + " (" + shortlist.getJobRole() + ")");
            System.out.println("Total Evaluated: " + shortlist.getTotalEvaluated());
            System.out.println("Eligible Candidates:   " + shortlist.getEligibleCount());
            System.out.println("Ineligible Candidates: " + shortlist.getIneligibleCount());
            System.out.println("Ranking Weights: CGPA " + cgpaW + "%, Skills " + skillsW + "%, Certs " + certsW + "%");
            System.out.println("--------------------------------------------------------------");

            List<ShortlistCandidate> candidates = shortlist.getCandidates();
            if (candidates == null) candidates = Collections.emptyList();

            // 1. Eligible Candidates
            System.out.println("\n[+] RANKED ELIGIBLE CANDIDATES:");
            List<ShortlistCandidate> eligible = new ArrayList<>();
            for (ShortlistCandidate sc : candidates) {
                if (Boolean.TRUE.equals(sc.getEligible())) eligible.add(sc);
            }

            if (eligible.isEmpty()) {
                System.out.println("  No candidates met all eligibility conditions for this drive.");
            } else {
                System.out.printf("  %-5s | %-12s | %-20s | %-6s | %-5s | %-12s | %s\n",
                        "Rank", "Student ID", "Full Name", "Dept", "CGPA", "Score (100)", "Matched Skills");
                System.out.println("  -------------------------------------------------------------------------------------------");
                for (ShortlistCandidate ec : eligible) {
                    String medal = "";
                    if (ec.getRankNumber() != null) {
                        if (ec.getRankNumber() == 1) medal = " [1st]";
                        else if (ec.getRankNumber() == 2) medal = " [2nd]";
                        else if (ec.getRankNumber() == 3) medal = " [3rd]";
                    }
                    System.out.printf("  %-5s | %-12s | %-20s | %-6s | %-5.2f | %-12.2f | %s\n",
                            "#" + ec.getRankNumber() + medal,
                            ec.getStudentId(),
                            truncate(ec.getStudentName(), 20),
                            ec.getDepartment(),
                            ec.getCgpa(),
                            ec.getRankingScore(),
                            ec.getMatchedSkillsSummary() == null ? "-" : ec.getMatchedSkillsSummary());
                }
            }

            // 2. Ineligible Candidates
            System.out.println("\n[-] INELIGIBLE CANDIDATES (REJECTION REASONS):");
            List<ShortlistCandidate> ineligible = new ArrayList<>();
            for (ShortlistCandidate sc : candidates) {
                if (!Boolean.TRUE.equals(sc.getEligible())) ineligible.add(sc);
            }

            if (ineligible.isEmpty()) {
                System.out.println("  All candidates qualified!");
            } else {
                System.out.printf("  %-12s | %-20s | %-6s | %-5s | %s\n",
                        "Student ID", "Full Name", "Dept", "CGPA", "Rejection Reasons");
                System.out.println("  -------------------------------------------------------------------------------------------");
                for (ShortlistCandidate ic : ineligible) {
                    System.out.printf("  %-12s | %-20s | %-6s | %-5.2f | %s\n",
                            ic.getStudentId(),
                            truncate(ic.getStudentName(), 20),
                            ic.getDepartment(),
                            ic.getCgpa(),
                            ic.getRejectionReasons());
                }
            }

            System.out.println("\n[*] Shortlist session #" + shortlist.getId() + " saved to database history.");
            System.out.println("[*] You can also view or download CSV/JSON via Web Portal at http://localhost:8080");

        } catch (Exception ex) {
            System.out.println("\n[X] ERROR generating shortlist: " + ex.getMessage());
        }
    }

    // =========================================================================
    // 6. View Shortlist History
    // =========================================================================
    private void handleViewShortlistHistory() {
        System.out.println("\n--- [HISTORICAL SHORTLIST SESSIONS] ---");
        List<Shortlist> history = placementService.getAllShortlists();

        if (history.isEmpty()) {
            System.out.println("No shortlists generated yet. Use Option [5] to run shortlisting.");
            return;
        }

        System.out.printf("%-4s | %-16s | %-18s | %-18s | %-10s | %-8s | %s\n",
                "ID", "Date & Time", "Company", "Job Role", "Evaluated", "Eligible", "Weights (C/S/A)");
        System.out.println("------------------------------------------------------------------------------------------------");

        for (Shortlist s : history) {
            String dateStr = s.getGeneratedAt() != null ? s.getGeneratedAt().toString().replace('T', ' ').substring(0, 16) : "-";
            String weights = String.format("%.0f/%.0f/%.0f", s.getCgpaWeight(), s.getSkillsWeight(), s.getCertsWeight());
            System.out.printf("%-4d | %-16s | %-18s | %-18s | %-10d | %-8d | %s\n",
                    s.getId(),
                    dateStr,
                    truncate(s.getCompanyName(), 18),
                    truncate(s.getJobRole(), 18),
                    s.getTotalEvaluated(),
                    s.getEligibleCount(),
                    weights);
        }
    }

    // =========================================================================
    // 7. View Dashboard Summary
    // =========================================================================
    private void handleViewDashboard() {
        System.out.println("\n==============================================================");
        System.out.println("                PLACEMENT HUB ANALYTICS SUMMARY               ");
        System.out.println("==============================================================");
        Map<String, Object> stats = placementService.getOverallStats();

        System.out.println("Total Registered Students:     " + stats.get("totalStudents"));
        System.out.println("Total Visiting Companies:       " + stats.get("totalCompanies"));
        System.out.println("Total Shortlist Drives Run:    " + stats.get("totalShortlists"));
        System.out.println("Latest Drive Company:          " + stats.get("latestShortlistCompany"));
        System.out.println("Latest Drive Eligible Pool:    " + stats.get("latestEligibleCount"));
        System.out.println("Web Portal:                    http://localhost:8080");
        System.out.println("==============================================================");
    }

    // =========================================================================
    // Helper Input Methods
    // =========================================================================
    private String readNonEmpty(Scanner scanner, String errorMsg) {
        while (true) {
            String val = scanner.nextLine().trim();
            if (!val.isEmpty()) return val;
            System.out.print("[!] " + errorMsg + ". Please re-enter: ");
        }
    }

    private double readDouble(Scanner scanner, String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                double val = Double.parseDouble(input);
                if (val >= min && val <= max) return val;
                System.out.println("[!] Value must be between " + min + " and " + max + ".");
            } catch (NumberFormatException ex) {
                System.out.println("[!] Please enter a valid number.");
            }
        }
    }

    private int readInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) return val;
                System.out.println("[!] Value must be between " + min + " and " + max + ".");
            } catch (NumberFormatException ex) {
                System.out.println("[!] Please enter a valid integer.");
            }
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "-";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 2) + "..";
    }
}
