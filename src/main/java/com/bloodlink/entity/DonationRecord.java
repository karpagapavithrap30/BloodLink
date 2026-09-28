package com.bloodlink.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "donation_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DonationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

    @Column(nullable = false)
    private LocalDate donationDate;

    @Column(nullable = false)
    private String hospital;
}
