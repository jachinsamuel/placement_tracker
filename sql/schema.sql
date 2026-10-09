-- ==============================================================================
-- Campus Placement Drive Candidate Tracking & Shortlisting Portal
-- MySQL Database Schema Definition
-- ==============================================================================

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS placement_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE placement_db;

-- Table: Skills
CREATE TABLE IF NOT EXISTS skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- Table: Students
CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(150) NOT NULL,
    department VARCHAR(50) NOT NULL,
    cgpa DOUBLE NOT NULL,
    active_backlogs INT NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    graduation_year INT NOT NULL,
    certifications_score DOUBLE NOT NULL DEFAULT 0.0
) ENGINE=InnoDB;

-- Table: Student Skills (Join Table)
CREATE TABLE IF NOT EXISTS student_skills (
    student_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (student_id, skill_id),
    CONSTRAINT fk_student_skills_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_student_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: Companies
CREATE TABLE IF NOT EXISTS companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_code VARCHAR(50),
    name VARCHAR(150) NOT NULL,
    job_role VARCHAR(100) NOT NULL,
    min_cgpa DOUBLE NOT NULL,
    max_backlogs INT NOT NULL,
    skill_matching_mode VARCHAR(10) NOT NULL DEFAULT 'ALL',
    eligible_departments VARCHAR(255),
    eligible_graduation_year INT,
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- Table: Company Required Skills (Join Table)
CREATE TABLE IF NOT EXISTS company_required_skills (
    company_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (company_id, skill_id),
    CONSTRAINT fk_company_skills_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_company_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Table: Shortlists (History Records)
CREATE TABLE IF NOT EXISTS shortlists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT,
    company_name VARCHAR(150) NOT NULL,
    job_role VARCHAR(100) NOT NULL,
    criteria_summary VARCHAR(500),
    generated_at DATETIME NOT NULL,
    cgpa_weight DOUBLE NOT NULL DEFAULT 60.0,
    skills_weight DOUBLE NOT NULL DEFAULT 30.0,
    certs_weight DOUBLE NOT NULL DEFAULT 10.0,
    total_evaluated INT NOT NULL DEFAULT 0,
    eligible_count INT NOT NULL DEFAULT 0,
    ineligible_count INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- Table: Shortlist Candidates
CREATE TABLE IF NOT EXISTS shortlist_candidates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shortlist_id BIGINT NOT NULL,
    student_id VARCHAR(50) NOT NULL,
    student_name VARCHAR(150) NOT NULL,
    department VARCHAR(50) NOT NULL,
    cgpa DOUBLE NOT NULL,
    active_backlogs INT NOT NULL,
    skills_summary VARCHAR(500),
    matched_skills_summary VARCHAR(500),
    skill_match_percentage DOUBLE DEFAULT 0.0,
    ranking_score DOUBLE NOT NULL DEFAULT 0.0,
    rank_number INT,
    eligible BOOLEAN NOT NULL,
    rejection_reasons VARCHAR(1000),
    CONSTRAINT fk_candidate_shortlist FOREIGN KEY (shortlist_id) REFERENCES shortlists(id) ON DELETE CASCADE
) ENGINE=InnoDB;
