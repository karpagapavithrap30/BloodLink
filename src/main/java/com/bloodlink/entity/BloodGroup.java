package com.bloodlink.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "blood_groups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BloodGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 5)
    private String type;  // e.g. "A+", "O-"
}
