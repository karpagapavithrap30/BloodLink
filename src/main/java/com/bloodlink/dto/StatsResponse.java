package com.bloodlink.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class StatsResponse {
    private long totalDonors;
    private long availableDonors;
    private long inCooldown;
    private String mostCommonBloodGroup;
    private Map<String, Long> donorsPerBloodGroup;
}
