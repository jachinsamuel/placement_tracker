package com.placement.service;

import com.placement.model.Student;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Module 4: Candidate Ranking Engine.
 * 
 * Beginner Java Explanation:
 * This class calculates a final score (out of 100) for each qualified student.
 * It uses simple arithmetic:
 *   Score = (CGPA Part) + (Skills Part) + (Certifications Part)
 *
 * Then it sorts the students from highest score to lowest score.
 * If two students have the same score, the one with the higher CGPA gets the higher rank!
 */
@Service
public class RankingService {

    /**
     * Helper class to store a student along with their calculated ranking details.
     */
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

        public Student getStudent() {
            return student;
        }

        public double getSkillMatchPercentage() {
            return skillMatchPercentage;
        }

        public double getRankingScore() {
            return rankingScore;
        }

        public void setRankingScore(double rankingScore) {
            this.rankingScore = rankingScore;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }
    }

    /**
     * Calculates the composite ranking score (0 to 100) using simple math.
     *
     * Example with default weights:
     * - Student CGPA: 8.5 out of 10.0 -> (8.5 / 10.0) * 100 = 85.0 points
     *   CGPA weight 60%               -> 85.0 * 0.60 = 51.0
     * - Skills match: 100%
     *   Skills weight 30%             -> 100.0 * 0.30 = 30.0
     * - Certifications: 70 out of 100
     *   Certs weight 10%              -> 70.0 * 0.10 = 7.0
     * - Final Score = 51.0 + 30.0 + 7.0 = 88.0 out of 100!
     */
    public double calculateScore(Student student, double skillMatchPercentage,
                                 double cgpaWeight, double skillsWeight, double certsWeight) {

        // 1. Convert CGPA (0.0 to 10.0) into a percentage (0 to 100)
        double cgpaScore = 0.0;
        if (student.getCgpa() != null) {
            cgpaScore = (student.getCgpa() / 10.0) * 100.0;
            // Make sure it stays between 0 and 100
            if (cgpaScore > 100.0) {
                cgpaScore = 100.0;
            }
            if (cgpaScore < 0.0) {
                cgpaScore = 0.0;
            }
        }

        // 2. Skills Match Score (0 to 100)
        double skillsScore = skillMatchPercentage;
        if (skillsScore > 100.0) {
            skillsScore = 100.0;
        }
        if (skillsScore < 0.0) {
            skillsScore = 0.0;
        }

        // 3. Certifications Score (0 to 100)
        double certsScore = 0.0;
        if (student.getCertificationsScore() != null) {
            certsScore = student.getCertificationsScore();
            if (certsScore > 100.0) {
                certsScore = 100.0;
            }
            if (certsScore < 0.0) {
                certsScore = 0.0;
            }
        }

        // 4. Calculate weighted total
        double totalScore = (cgpaScore * (cgpaWeight / 100.0))
                          + (skillsScore * (skillsWeight / 100.0))
                          + (certsScore * (certsWeight / 100.0));

        // 5. Round to 2 decimal places using basic Java math: Math.round(value * 100.0) / 100.0
        return Math.round(totalScore * 100.0) / 100.0;
    }

    /**
     * Ranks a list of qualified candidates from 1st place to last place.
     */
    public List<RankedCandidate> rankCandidates(List<RankedCandidate> candidates,
                                                double cgpaWeight, double skillsWeight, double certsWeight) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: Calculate the score for each candidate in the list
        for (int i = 0; i < candidates.size(); i++) {
            RankedCandidate candidate = candidates.get(i);
            double score = calculateScore(candidate.getStudent(), candidate.getSkillMatchPercentage(),
                    cgpaWeight, skillsWeight, certsWeight);
            candidate.setRankingScore(score);
        }

        // Step 2: Sort the candidates using a simple Comparator
        candidates.sort(new Comparator<RankedCandidate>() {
            @Override
            public int compare(RankedCandidate c1, RankedCandidate c2) {
                // Rule 1: Highest ranking score comes first (descending)
                if (c2.getRankingScore() > c1.getRankingScore()) {
                    return 1;
                }
                if (c2.getRankingScore() < c1.getRankingScore()) {
                    return -1;
                }

                // Rule 2 (Tie-Breaker): If scores are tied, higher CGPA comes first
                double cgpa1 = (c1.getStudent().getCgpa() != null) ? c1.getStudent().getCgpa() : 0.0;
                double cgpa2 = (c2.getStudent().getCgpa() != null) ? c2.getStudent().getCgpa() : 0.0;
                if (cgpa2 > cgpa1) {
                    return 1;
                }
                if (cgpa2 < cgpa1) {
                    return -1;
                }

                // Rule 3: If still tied, sort alphabetically by Student ID
                String id1 = (c1.getStudent().getStudentId() != null) ? c1.getStudent().getStudentId() : "";
                String id2 = (c2.getStudent().getStudentId() != null) ? c2.getStudent().getStudentId() : "";
                return id1.compareTo(id2);
            }
        });

        // Step 3: Assign simple ranks (#1, #2, #3, ...)
        int currentRank = 1;
        for (int i = 0; i < candidates.size(); i++) {
            candidates.get(i).setRank(currentRank);
            currentRank++;
        }

        return candidates;
    }
}
