package com.placement.repository;

import com.placement.model.ShortlistCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShortlistCandidateRepository extends JpaRepository<ShortlistCandidate, Long> {

    List<ShortlistCandidate> findByShortlistId(Long shortlistId);

    List<ShortlistCandidate> findByShortlistIdAndEligibleTrueOrderByRankNumberAsc(Long shortlistId);

    List<ShortlistCandidate> findByShortlistIdAndEligibleFalse(Long shortlistId);
}
