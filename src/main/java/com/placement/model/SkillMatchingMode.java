package com.placement.model;

/**
 * Enum indicating skill matching mode for company eligibility criteria.
 * ANY: Candidate must possess at least one of the company's required skills.
 * ALL: Candidate must possess every single required skill.
 */
public enum SkillMatchingMode {
    ANY,
    ALL
}
