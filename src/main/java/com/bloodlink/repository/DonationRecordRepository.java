package com.bloodlink.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bloodlink.entity.DonationRecord;

public interface DonationRecordRepository extends JpaRepository<DonationRecord, Long> {

    List<DonationRecord> findByDonorIdOrderByDonationDateDesc(Long donorId);
}
