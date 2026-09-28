package com.bloodlink.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DonorRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "\\d{10}", message = "Phone must be exactly 10 digits")
    private String phone;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Blood group is required")
    private String bloodGroup;

    @PastOrPresent(message = "Last donation date cannot be in the future")
    private LocalDate lastDonationDate;   // optional on registration
}
