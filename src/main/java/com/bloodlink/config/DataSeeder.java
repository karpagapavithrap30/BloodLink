package com.bloodlink.config;

import com.bloodlink.entity.BloodGroup;
import com.bloodlink.repository.BloodGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final BloodGroupRepository bloodGroupRepository;

    private static final List<String> BLOOD_TYPES = List.of(
            "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    );

    @Override
    public void run(String... args) {
        BLOOD_TYPES.forEach(type -> {
            if (!bloodGroupRepository.existsByType(type)) {
                bloodGroupRepository.save(BloodGroup.builder().type(type).build());
                log.info("Seeded blood group: {}", type);
            }
        });
        log.info("Blood group seeding complete. Total: {}", bloodGroupRepository.count());
    }
}
