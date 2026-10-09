package com.placement.config;

import com.placement.model.Company;
import com.placement.model.Skill;
import com.placement.model.SkillMatchingMode;
import com.placement.model.Student;
import com.placement.repository.CompanyRepository;
import com.placement.repository.SkillRepository;
import com.placement.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Initializes realistic sample student and visiting company data
 * for college project presentation and viva demonstration.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final SkillRepository skillRepository;

    public DataInitializer(StudentRepository studentRepository,
                           CompanyRepository companyRepository,
                           SkillRepository skillRepository) {
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.skillRepository = skillRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (studentRepository.count() == 0 && companyRepository.count() == 0) {
            initSampleData();
        }
    }

    private void initSampleData() {
        System.out.println(">>> [DataInitializer] Seeding placement demo data...");

        // 1. Create Technical Skills
        Map<String, Skill> skills = new HashMap<>();
        String[] skillNames = {
            "Java", "Python", "SQL", "C++", "React", "Spring Boot",
            "AWS", "Docker", "Machine Learning", "Data Structures"
        };
        for (String name : skillNames) {
            skills.put(name, skillRepository.save(new Skill(name)));
        }

        // 2. Create Sample Students
        List<Student> students = new ArrayList<>();

        // Student 1: Aarav Sharma (CSE, 8.8 CGPA, 0 backlogs, 2027)
        Student s1 = new Student("STU2027001", "Aarav Sharma", "CSE", 8.8, 0, "aarav.sharma@campus.edu", 2027);
        s1.setCertificationsScore(85.0);
        s1.addSkill(skills.get("Java"));
        s1.addSkill(skills.get("SQL"));
        s1.addSkill(skills.get("Spring Boot"));
        students.add(s1);

        // Student 2: Priya Patel (IT, 8.5 CGPA, 0 backlogs, 2027)
        Student s2 = new Student("STU2027002", "Priya Patel", "IT", 8.5, 0, "priya.patel@campus.edu", 2027);
        s2.setCertificationsScore(90.0);
        s2.addSkill(skills.get("Java"));
        s2.addSkill(skills.get("SQL"));
        s2.addSkill(skills.get("React"));
        students.add(s2);

        // Student 3: Rohan Gupta (High CGPA 9.2, but has 1 backlog)
        Student s3 = new Student("STU2027003", "Rohan Gupta", "CSE", 9.2, 1, "rohan.gupta@campus.edu", 2027);
        s3.setCertificationsScore(75.0);
        s3.addSkill(skills.get("Java"));
        s3.addSkill(skills.get("Python"));
        s3.addSkill(skills.get("SQL"));
        students.add(s3);

        // Student 4: Ananya Iyer (CSE, 7.2 CGPA -> below 7.5 CGPA)
        Student s4 = new Student("STU2027004", "Ananya Iyer", "CSE", 7.2, 0, "ananya.iyer@campus.edu", 2027);
        s4.setCertificationsScore(60.0);
        s4.addSkill(skills.get("Java"));
        s4.addSkill(skills.get("SQL"));
        students.add(s4);

        // Student 5: Vikram Singh (ECE, 8.0 CGPA, 0 backlogs, 2027)
        Student s5 = new Student("STU2027005", "Vikram Singh", "ECE", 8.0, 0, "vikram.singh@campus.edu", 2027);
        s5.setCertificationsScore(70.0);
        s5.addSkill(skills.get("Java"));
        s5.addSkill(skills.get("C++"));
        s5.addSkill(skills.get("Python"));
        students.add(s5);

        // Student 6: Sneha Nair (CSE, 8.4 CGPA, 0 backlogs, 2026 Batch)
        Student s6 = new Student("STU2026006", "Sneha Nair", "CSE", 8.4, 0, "sneha.nair@campus.edu", 2026);
        s6.setCertificationsScore(80.0);
        s6.addSkill(skills.get("Java"));
        s6.addSkill(skills.get("SQL"));
        s6.addSkill(skills.get("AWS"));
        students.add(s6);

        // Student 7: Kabir Verma (IT, 8.1 CGPA, 0 backlogs, 2027 -> Has Java, missing SQL)
        Student s7 = new Student("STU2027007", "Kabir Verma", "IT", 8.1, 0, "kabir.verma@campus.edu", 2027);
        s7.setCertificationsScore(65.0);
        s7.addSkill(skills.get("Java"));
        s7.addSkill(skills.get("Docker"));
        students.add(s7);

        // Student 8: Meera Reddy (CSE, 8.8 CGPA, 0 backlogs, 2027 -> Equal CGPA with Aarav)
        Student s8 = new Student("STU2027008", "Meera Reddy", "CSE", 8.8, 0, "meera.reddy@campus.edu", 2027);
        s8.setCertificationsScore(95.0);
        s8.addSkill(skills.get("Java"));
        s8.addSkill(skills.get("SQL"));
        s8.addSkill(skills.get("Data Structures"));
        students.add(s8);

        // Student 9: Devendra Joshi (MECH, 7.8 CGPA, 0 backlogs, 2026)
        Student s9 = new Student("STU2026009", "Devendra Joshi", "MECH", 7.8, 0, "devendra.j@campus.edu", 2026);
        s9.setCertificationsScore(50.0);
        s9.addSkill(skills.get("C++"));
        s9.addSkill(skills.get("Python"));
        students.add(s9);

        // Student 10: Ritu Sen (ECE, 7.6 CGPA, 1 backlog, 2026)
        Student s10 = new Student("STU2026010", "Ritu Sen", "ECE", 7.6, 1, "ritu.sen@campus.edu", 2026);
        s10.setCertificationsScore(70.0);
        s10.addSkill(skills.get("Python"));
        s10.addSkill(skills.get("Machine Learning"));
        students.add(s10);

        studentRepository.saveAll(students);

        // 3. Create Sample Companies with Configured Criteria
        List<Company> companies = new ArrayList<>();

        Company c1 = new Company(
                "Example Technologies",
                "Software Development Engineer",
                7.5,
                0,
                SkillMatchingMode.ALL,
                "CSE, IT",
                2027
        );
        c1.setCompanyCode("CMP-EX-001");
        c1.addRequiredSkill(skills.get("Java"));
        c1.addRequiredSkill(skills.get("SQL"));
        companies.add(c1);

        Company c2 = new Company(
                "CloudScale Systems",
                "Cloud Solutions Associate",
                7.0,
                1,
                SkillMatchingMode.ANY,
                "CSE, IT, ECE",
                2026
        );
        c2.setCompanyCode("CMP-CS-002");
        c2.addRequiredSkill(skills.get("Java"));
        c2.addRequiredSkill(skills.get("Python"));
        c2.addRequiredSkill(skills.get("AWS"));
        companies.add(c2);

        Company c3 = new Company(
                "Apex FinTech",
                "Backend Engineer",
                8.0,
                0,
                SkillMatchingMode.ALL,
                "CSE, IT",
                2026
        );
        c3.setCompanyCode("CMP-AF-003");
        c3.addRequiredSkill(skills.get("Java"));
        c3.addRequiredSkill(skills.get("Spring Boot"));
        c3.addRequiredSkill(skills.get("SQL"));
        companies.add(c3);

        Company c4 = new Company(
                "InnoCore Robotics",
                "Embedded Software Engineer",
                6.5,
                2,
                SkillMatchingMode.ANY,
                "ECE, MECH",
                2026
        );
        c4.setCompanyCode("CMP-IR-004");
        c4.addRequiredSkill(skills.get("C++"));
        c4.addRequiredSkill(skills.get("Python"));
        companies.add(c4);

        companyRepository.saveAll(companies);

        System.out.println(">>> [DataInitializer] Seeded 10 students, 4 companies, and 10 skills.");
    }
}
