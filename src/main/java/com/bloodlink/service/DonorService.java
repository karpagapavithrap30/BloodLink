package com.bloodlink.service;

import com.bloodlink.dto.*;
import com.bloodlink.entity.BloodGroup;
import com.bloodlink.entity.DonationRecord;
import com.bloodlink.entity.Donor;
import com.bloodlink.exception.BusinessRuleException;
import com.bloodlink.exception.ResourceNotFoundException;
import com.bloodlink.repository.BloodGroupRepository;
import com.bloodlink.repository.DonationRecordRepository;
import com.bloodlink.repository.DonorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonorService {

    private final DonorRepository       donorRepository;
    private final BloodGroupRepository  bloodGroupRepository;
    private final DonationRecordRepository donationRecordRepository;

    @Value("${bloodlink.cooldown-days:90}")
    private int cooldownDays;

    // ── Helper: entity → DTO ─────────────────────────────────────────────────
    private DonorResponse toResponse(Donor d) {
        return DonorResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .phone(d.getPhone())
                .city(d.getCity())
                .bloodGroup(d.getBloodGroup().getType())
                .lastDonationDate(d.getLastDonationDate())
                .available(d.getAvailable())
                .build();
    }

    private LocalDate cooldownCutoff() {
        return LocalDate.now().minusDays(cooldownDays);
    }

    // ── Register Donor ───────────────────────────────────────────────────────
    @Transactional
    public DonorResponse registerDonor(DonorRequest req) {
        BloodGroup bg = bloodGroupRepository.findByType(req.getBloodGroup())
                .orElseThrow(() -> new BusinessRuleException(
                        "Unknown blood group: " + req.getBloodGroup()
                        + ". Valid values are A+, A-, B+, B-, AB+, AB-, O+, O-"));

        // Determine availability based on last donation date supplied at registration
        boolean available = true;
        if (req.getLastDonationDate() != null) {
            available = !req.getLastDonationDate().isAfter(cooldownCutoff());
        }

        Donor donor = Donor.builder()
                .name(req.getName())
                .phone(req.getPhone())
                .city(req.getCity())
                .bloodGroup(bg)
                .lastDonationDate(req.getLastDonationDate())
                .available(available)
                .build();

        return toResponse(donorRepository.save(donor));
    }

    // ── Get All Donors (paginated) ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<DonorResponse> getAllDonors(Pageable pageable) {
        return donorRepository.findAll(pageable).map(this::toResponse);
    }

    // ── Search Donors ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DonorResponse> searchDonors(String bloodGroup, String city) {
        // Validate blood group exists
        bloodGroupRepository.findByType(bloodGroup)
                .orElseThrow(() -> new BusinessRuleException(
                        "Unknown blood group: " + bloodGroup));

        List<Donor> donors = donorRepository.searchAvailableDonors(
                bloodGroup, city, cooldownCutoff());

        return donors.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Record Donation ──────────────────────────────────────────────────────
    @Transactional
    public DonationRecordResponse recordDonation(Long donorId, DonationRequest req) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Donor not found with id: " + donorId));

        // Cooldown check
        if (donor.getLastDonationDate() != null) {
            LocalDate eligibleFrom = donor.getLastDonationDate().plusDays(cooldownDays);
            if (donor.getLastDonationDate().isAfter(cooldownCutoff())) {
                throw new BusinessRuleException(
                        "Donor " + donor.getName() + " is in cooldown. "
                        + "Eligible to donate again from: " + eligibleFrom);
            }
        }

        donor.setLastDonationDate(req.getDonationDate());
        donor.setAvailable(false);
        donorRepository.save(donor);

        DonationRecord record = DonationRecord.builder()
                .donor(donor)
                .donationDate(req.getDonationDate())
                .hospital(req.getHospital())
                .build();

        DonationRecord saved = donationRecordRepository.save(record);

        return DonationRecordResponse.builder()
                .id(saved.getId())
                .donorId(donor.getId())
                .donorName(donor.getName())
                .donationDate(saved.getDonationDate())
                .hospital(saved.getHospital())
                .build();
    }

    // ── Mark Unavailable (manual pause) ─────────────────────────────────────
    @Transactional
    public DonorResponse markUnavailable(Long donorId) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Donor not found with id: " + donorId));
        donor.setAvailable(false);
        return toResponse(donorRepository.save(donor));
    }

    // ── Stats ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public StatsResponse getStats() {
        long total     = donorRepository.count();
        long available = donorRepository.countByAvailableTrue();
        long cooldown  = total - available;

        List<Object[]> rows = donorRepository.countDonorsPerBloodGroup();
        Map<String, Long> perGroup = new LinkedHashMap<>();
        String mostCommon = null;
        long   maxCount   = -1;

        for (Object[] row : rows) {
            String type  = (String) row[0];
            long   count = (Long)   row[1];
            perGroup.put(type, count);
            if (count > maxCount) {
                maxCount   = count;
                mostCommon = type;
            }
        }

        return StatsResponse.builder()
                .totalDonors(total)
                .availableDonors(available)
                .inCooldown(cooldown)
                .mostCommonBloodGroup(mostCommon != null ? mostCommon : "N/A")
                .donorsPerBloodGroup(perGroup)
                .build();
    }

    // ── Scheduled: nightly re-enable ─────────────────────────────────────────
    @Scheduled(cron = "0 0 0 * * *")   // midnight every day
    @Transactional
    public void reEnableDonorsAfterCooldown() {
        int updated = donorRepository.reEnableDonorsAfterCooldown(cooldownCutoff());
        log.info("[Scheduler] Re-enabled {} donor(s) after cooldown period.", updated);
    }
}
