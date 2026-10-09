-- ==============================================================================
-- Fictional Demonstration Data for Campus Placement Portal
-- Suitable for College Viva & Presentation
-- ==============================================================================

USE placement_db;

-- Insert Skills
INSERT INTO skills (id, name) VALUES
(1, 'Java'),
(2, 'Python'),
(3, 'SQL'),
(4, 'C++'),
(5, 'React'),
(6, 'Spring Boot'),
(7, 'AWS'),
(8, 'Docker'),
(9, 'Machine Learning'),
(10, 'Data Structures')
ON DUPLICATE KEY UPDATE name=name;

-- Insert Students
INSERT INTO students (id, student_id, full_name, department, cgpa, active_backlogs, email, graduation_year, certifications_score) VALUES
(1, 'STU2027001', 'Aarav Sharma', 'CSE', 8.8, 0, 'aarav.sharma@campus.edu', 2027, 85.0),
(2, 'STU2027002', 'Priya Patel', 'IT', 8.5, 0, 'priya.patel@campus.edu', 2027, 90.0),
(3, 'STU2027003', 'Rohan Gupta', 'CSE', 9.2, 1, 'rohan.gupta@campus.edu', 2027, 75.0),
(4, 'STU2027004', 'Ananya Iyer', 'CSE', 7.2, 0, 'ananya.iyer@campus.edu', 2027, 60.0),
(5, 'STU2027005', 'Vikram Singh', 'ECE', 8.0, 0, 'vikram.singh@campus.edu', 2027, 70.0),
(6, 'STU2026006', 'Sneha Nair', 'CSE', 8.4, 0, 'sneha.nair@campus.edu', 2026, 80.0),
(7, 'STU2027007', 'Kabir Verma', 'IT', 8.1, 0, 'kabir.verma@campus.edu', 2027, 65.0),
(8, 'STU2027008', 'Meera Reddy', 'CSE', 8.8, 0, 'meera.reddy@campus.edu', 2027, 95.0),
(9, 'STU2026009', 'Devendra Joshi', 'MECH', 7.8, 0, 'devendra.j@campus.edu', 2026, 50.0),
(10, 'STU2026010', 'Ritu Sen', 'ECE', 7.6, 1, 'ritu.sen@campus.edu', 2026, 70.0)
ON DUPLICATE KEY UPDATE student_id=student_id;

-- Student Skills Mapping
INSERT INTO student_skills (student_id, skill_id) VALUES
(1, 1), (1, 3), (1, 6),
(2, 1), (2, 3), (2, 5),
(3, 1), (3, 2), (3, 3),
(4, 1), (4, 3),
(5, 1), (5, 4), (5, 2),
(6, 1), (6, 3), (6, 7),
(7, 1), (7, 8),
(8, 1), (8, 3), (8, 10),
(9, 4), (9, 2),
(10, 2), (10, 9)
ON DUPLICATE KEY UPDATE student_id=student_id;

-- Insert Companies
INSERT INTO companies (id, company_code, name, job_role, min_cgpa, max_backlogs, skill_matching_mode, eligible_departments, eligible_graduation_year, active) VALUES
(1, 'CMP-EX-001', 'Example Technologies', 'Software Development Engineer', 7.5, 0, 'ALL', 'CSE, IT', 2027, true),
(2, 'CMP-CS-002', 'CloudScale Systems', 'Cloud Solutions Associate', 7.0, 1, 'ANY', 'CSE, IT, ECE', 2026, true),
(3, 'CMP-AF-003', 'Apex FinTech', 'Backend Engineer', 8.0, 0, 'ALL', 'CSE, IT', 2026, true),
(4, 'CMP-IR-004', 'InnoCore Robotics', 'Embedded Software Engineer', 6.5, 2, 'ANY', 'ECE, MECH', 2026, true)
ON DUPLICATE KEY UPDATE name=name;

-- Company Required Skills Mapping
INSERT INTO company_required_skills (company_id, skill_id) VALUES
(1, 1), (1, 3),
(2, 1), (2, 2), (2, 7),
(3, 1), (3, 6), (3, 3),
(4, 4), (4, 2)
ON DUPLICATE KEY UPDATE company_id=company_id;
