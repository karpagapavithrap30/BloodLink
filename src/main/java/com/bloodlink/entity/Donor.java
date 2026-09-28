package com.bloodlink.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "donors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String phone;

    @Column(nullable = false)
    private String city;

    private LocalDate lastDonationDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "blood_group_id", nullable = false)
    private BloodGroup bloodGroup;

    @OneToMany(mappedBy = "donor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DonationRecord> donationRecords = new ArrayList<>();
}
