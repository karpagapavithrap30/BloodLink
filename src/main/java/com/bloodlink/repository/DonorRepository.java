package com.bloodlink.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bloodlink.entity.Donor;

public interface DonorRepository extends JpaRepository<Donor, Long> {

    /**
     * Search available donors by blood-group type and city,
     * whose last donation was before the cooldown cutoff
     * (or who have never donated at all).
     */
    @Query("""
            SELECT d FROM Donor d
            JOIN d.bloodGroup bg
            WHERE LOWER(bg.type) = LOWER(:bloodGroupType)
              AND LOWER(d.city)  = LOWER(:city)
              AND d.available    = true
              AND (d.lastDonationDate IS NULL
                   OR d.lastDonationDate <= :cooldownCutoff)
            """)
    List<Donor> searchAvailableDonors(
            @Param("bloodGroupType") String bloodGroupType,
            @Param("city")          String city,
            @Param("cooldownCutoff") LocalDate cooldownCutoff
    );

    /**
     * Count of donors per blood group (for the bar chart / stats).
     * Returns Object[] rows: [bloodGroupType, count].
     */
    @Query("""
            SELECT bg.type, COUNT(d)
            FROM Donor d
            JOIN d.bloodGroup bg
            GROUP BY bg.type
            ORDER BY bg.type
            """)
    List<Object[]> countDonorsPerBloodGroup();

    /**
     * Bulk update: re-enable donors whose cooldown period has passed.
     * Called nightly by the scheduler.
     */
    @Modifying
    @Query("""
            UPDATE Donor d
            SET d.available = true
            WHERE d.available = false
              AND d.lastDonationDate IS NOT NULL
              AND d.lastDonationDate <= :cooldownCutoff
            """)
    int reEnableDonorsAfterCooldown(@Param("cooldownCutoff") LocalDate cooldownCutoff);

    /** Count of all available donors. */
    long countByAvailableTrue();
}
