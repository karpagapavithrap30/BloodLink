package com.bloodlink.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bloodlink.entity.BloodGroup;

public interface BloodGroupRepository extends JpaRepository<BloodGroup, Long> {

    Optional<BloodGroup> findByType(String type);

    boolean existsByType(String type);
}
