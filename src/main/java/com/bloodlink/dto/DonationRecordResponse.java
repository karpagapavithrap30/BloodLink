package com.bloodlink.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DonationRecordResponse {
    private Long id;
    private Long donorId;
    private String donorName;
    private LocalDate donationDate;
    private String hospital;
}
