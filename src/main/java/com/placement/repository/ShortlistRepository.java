package com.placement.repository;

import com.placement.model.Shortlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShortlistRepository extends JpaRepository<Shortlist, Long> {

    List<Shortlist> findAllByOrderByGeneratedAtDesc();
}
