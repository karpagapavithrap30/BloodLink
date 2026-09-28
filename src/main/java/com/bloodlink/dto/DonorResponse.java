package com.bloodlink.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DonorResponse {
    private Long id;
    private String name;
    private String phone;
    private String city;
    private String bloodGroup;
    private LocalDate lastDonationDate;
    private Boolean available;
}
