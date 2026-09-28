package com.bloodlink.service;

import com.bloodlink.dto.DonationRequest;
import com.bloodlink.dto.DonorRequest;
import com.bloodlink.dto.DonorResponse;
import com.bloodlink.entity.BloodGroup;
import com.bloodlink.entity.Donor;
import com.bloodlink.exception.BusinessRuleException;
import com.bloodlink.exception.ResourceNotFoundException;
import com.bloodlink.repository.BloodGroupRepository;
import com.bloodlink.repository.DonationRecordRepository;
import com.bloodlink.repository.DonorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonorServiceTest {

    @Mock DonorRepository          donorRepository;
    @Mock BloodGroupRepository     bloodGroupRepository;
    @Mock DonationRecordRepository donationRecordRepository;

    @InjectMocks DonorService donorService;

    private BloodGroup bgAPos;
    private Donor donor;

    @BeforeEach
    void setUp() {
        // Inject cooldown-days = 90 via reflection (replaces @Value)
        ReflectionTestUtils.setField(donorService, "cooldownDays", 90);

        bgAPos = BloodGroup.builder().id(1L).type("A+").build();

        donor = Donor.builder()
                .id(1L)
                .name("Test User")
                .phone("9876543210")
                .city("Chennai")
                .bloodGroup(bgAPos)
                .available(true)
                .build();
    }

    // ── Registration ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Register donor with no last-donation → available=true")
    void registerDonor_noLastDonation_isAvailable() {
        DonorRequest req = new DonorRequest();
        req.setName("Test User"); req.setPhone("9876543210");
        req.setCity("Chennai");   req.setBloodGroup("A+");

        when(bloodGroupRepository.findByType("A+")).thenReturn(Optional.of(bgAPos));
        when(donorRepository.save(any())).thenAnswer(inv -> {
            Donor d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        DonorResponse resp = donorService.registerDonor(req);

        assertThat(resp.getAvailable()).isTrue();
        assertThat(resp.getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Register donor donated 30 days ago → available=false (in cooldown)")
    void registerDonor_recentDonation_isInCooldown() {
        DonorRequest req = new DonorRequest();
        req.setName("Test User"); req.setPhone("9876543210");
        req.setCity("Chennai");   req.setBloodGroup("A+");
        req.setLastDonationDate(LocalDate.now().minusDays(30));

        when(bloodGroupRepository.findByType("A+")).thenReturn(Optional.of(bgAPos));
        when(donorRepository.save(any())).thenAnswer(inv -> {
            Donor d = inv.getArgument(0); d.setId(1L); return d;
        });

        DonorResponse resp = donorService.registerDonor(req);
        assertThat(resp.getAvailable()).isFalse();
    }

    @Test
    @DisplayName("Register donor donated 100 days ago → available=true")
    void registerDonor_100DaysAgo_isAvailable() {
        DonorRequest req = new DonorRequest();
        req.setName("Test User"); req.setPhone("9876543210");
        req.setCity("Chennai");   req.setBloodGroup("A+");
        req.setLastDonationDate(LocalDate.now().minusDays(100));

        when(bloodGroupRepository.findByType("A+")).thenReturn(Optional.of(bgAPos));
        when(donorRepository.save(any())).thenAnswer(inv -> {
            Donor d = inv.getArgument(0); d.setId(1L); return d;
        });

        DonorResponse resp = donorService.registerDonor(req);
        assertThat(resp.getAvailable()).isTrue();
    }

    @Test
    @DisplayName("Register with unknown blood group → BusinessRuleException")
    void registerDonor_unknownBloodGroup_throwsBusinessRule() {
        DonorRequest req = new DonorRequest();
        req.setName("X"); req.setPhone("1234567890");
        req.setCity("Y"); req.setBloodGroup("X+");

        when(bloodGroupRepository.findByType("X+")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> donorService.registerDonor(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Unknown blood group");
    }

    // ── Cooldown rule on donation ─────────────────────────────────────────────

    @Test
    @DisplayName("Recording donation while in cooldown → BusinessRuleException with eligibility date")
    void recordDonation_inCooldown_throwsBusinessRule() {
        donor.setLastDonationDate(LocalDate.now().minusDays(30));
        donor.setAvailable(false);

        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor));

        DonationRequest req = new DonationRequest();
        req.setDonorId(1L);
        req.setHospital("Test Hospital");
        req.setDonationDate(LocalDate.now());

        assertThatThrownBy(() -> donorService.recordDonation(1L, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cooldown")
                .hasMessageContaining("Eligible to donate again from:");
    }

    @Test
    @DisplayName("Recording donation 91 days after last → success, sets available=false")
    void recordDonation_afterCooldown_succeeds() {
        donor.setLastDonationDate(LocalDate.now().minusDays(91));
        donor.setAvailable(true);

        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor));
        when(donorRepository.save(any())).thenReturn(donor);
        when(donationRecordRepository.save(any())).thenAnswer(inv -> {
            var r = inv.<com.bloodlink.entity.DonationRecord>getArgument(0);
            r = com.bloodlink.entity.DonationRecord.builder()
                    .id(1L).donor(donor)
                    .donationDate(LocalDate.now())
                    .hospital("Test Hospital").build();
            return r;
        });

        DonationRequest req = new DonationRequest();
        req.setDonorId(1L);
        req.setHospital("Test Hospital");
        req.setDonationDate(LocalDate.now());

        var resp = donorService.recordDonation(1L, req);
        assertThat(resp.getHospital()).isEqualTo("Test Hospital");
        // Donor should now be unavailable
        assertThat(donor.getAvailable()).isFalse();
    }

    @Test
    @DisplayName("Recording donation for non-existent donor → ResourceNotFoundException")
    void recordDonation_donorNotFound_throwsNotFound() {
        when(donorRepository.findById(999L)).thenReturn(Optional.empty());

        DonationRequest req = new DonationRequest();
        req.setDonorId(999L); req.setHospital("H"); req.setDonationDate(LocalDate.now());

        assertThatThrownBy(() -> donorService.recordDonation(999L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Search ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Search excludes donors in cooldown via repository cutoff")
    void searchDonors_excludesCooldownDonors() {
        when(bloodGroupRepository.findByType("A+")).thenReturn(Optional.of(bgAPos));
        when(donorRepository.searchAvailableDonors(eq("A+"), eq("Chennai"), any()))
                .thenReturn(List.of(donor));

        var results = donorService.searchDonors("A+", "Chennai");
        assertThat(results).hasSize(1);
        // verify cutoff was calculated correctly: today - 90 days
        ArgumentCaptor<LocalDate> cutoffCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(donorRepository).searchAvailableDonors(eq("A+"), eq("Chennai"), cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isEqualTo(LocalDate.now().minusDays(90));
    }

    // ── Mark Unavailable ─────────────────────────────────────────────────────

    @Test
    @DisplayName("markUnavailable for missing donor → ResourceNotFoundException")
    void markUnavailable_notFound() {
        when(donorRepository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> donorService.markUnavailable(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
