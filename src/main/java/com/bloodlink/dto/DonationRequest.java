package com.bloodlink.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DonationRequest {

    @NotNull(message = "Donor ID is required")
    private Long donorId;

    @NotBlank(message = "Hospital name is required")
    private String hospital;

    @NotNull(message = "Donation date is required")
    @PastOrPresent(message = "Donation date cannot be in the future")
    private LocalDate donationDate;
}
