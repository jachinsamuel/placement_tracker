package com.placement.service;

import com.placement.model.Student;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Module 4: Candidate Ranking Engine.
 * Calculates transparent, explainable ranking scores (out of 100) and sorts candidates.
 */
@Service
public class RankingService {

    /**
     * Helper container holding a candidate student along with their computed ranking metrics.
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
     * Calculates the ranking score for a candidate using normalized components and configured weights.
     *
     * @param student              The student being ranked
     * @param skillMatchPercentage Normalized skill match score (0 to 100)
     * @param cgpaWeight           Weight for CGPA (default 60%)
     * @param skillsWeight         Weight for Skills (default 30%)
     * @param certsWeight          Weight for Certifications/Achievements (default 10%)
     * @return Final composite score out of 100 (rounded to 2 decimal places)
     */
    public double calculateScore(Student student, double skillMatchPercentage,
                                 double cgpaWeight, double skillsWeight, double certsWeight) {
        // 1. Normalize CGPA to a score out of 100 (CGPA is out of 10.0)
        double cgpaNormalized = 0.0;
        if (student.getCgpa() != null) {
            cgpaNormalized = Math.min(100.0, Math.max(0.0, (student.getCgpa() / 10.0) * 100.0));
        }

        // 2. Normalized Skills Component (0 to 100)
        double skillsNormalized = Math.min(100.0, Math.max(0.0, skillMatchPercentage));

        // 3. Normalized Certifications & Achievements Component (0 to 100)
        double certsNormalized = 0.0;
        if (student.getCertificationsScore() != null) {
            certsNormalized = Math.min(100.0, Math.max(0.0, student.getCertificationsScore()));
        }

        // 4. Weighted Calculation
        double weightedScore = (cgpaNormalized * (cgpaWeight / 100.0))
                             + (skillsNormalized * (skillsWeight / 100.0))
                             + (certsNormalized * (certsWeight / 100.0));

        // Round to 2 decimal places for clean display and consistency
        return BigDecimal.valueOf(weightedScore)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Ranks a list of eligible candidates.
     * Primary Sort: Highest ranking score to lowest.
     * Secondary Sort (Tie-breaker): Highest CGPA.
     *
     * @param candidates   List of eligible candidate containers
     * @param cgpaWeight   Configured CGPA weight
     * @param skillsWeight Configured Skills weight
     * @param certsWeight  Configured Certifications weight
     * @return Sorted list with assigned ranks (1, 2, 3...)
     */
    public List<RankedCandidate> rankCandidates(List<RankedCandidate> candidates,
                                               double cgpaWeight, double skillsWeight, double certsWeight) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: Calculate score for each candidate
        for (RankedCandidate rc : candidates) {
            double score = calculateScore(rc.getStudent(), rc.getSkillMatchPercentage(),
                    cgpaWeight, skillsWeight, certsWeight);
            rc.setRankingScore(score);
        }

        // Step 2: Sort using Comparator (Primary: Score DESC, Secondary: CGPA DESC)
        candidates.sort(new Comparator<RankedCandidate>() {
            @Override
            public int compare(RankedCandidate c1, RankedCandidate c2) {
                // Primary: Score descending
                int scoreComparison = Double.compare(c2.getRankingScore(), c1.getRankingScore());
                if (scoreComparison != 0) {
                    return scoreComparison;
                }

                // Secondary (Tie-breaker): CGPA descending
                double cgpa1 = c1.getStudent().getCgpa() != null ? c1.getStudent().getCgpa() : 0.0;
                double cgpa2 = c2.getStudent().getCgpa() != null ? c2.getStudent().getCgpa() : 0.0;
                int cgpaComparison = Double.compare(cgpa2, cgpa1);
                if (cgpaComparison != 0) {
                    return cgpaComparison;
                }

                // Tertiary: Student ID ascending for stable order
                String id1 = c1.getStudent().getStudentId() != null ? c1.getStudent().getStudentId() : "";
                String id2 = c2.getStudent().getStudentId() != null ? c2.getStudent().getStudentId() : "";
                return id1.compareTo(id2);
            }
        });

        // Step 3: Assign sequential ranks (1, 2, 3...)
        int currentRank = 1;
        for (RankedCandidate rc : candidates) {
            rc.setRank(currentRank++);
        }

        return candidates;
    }
}
