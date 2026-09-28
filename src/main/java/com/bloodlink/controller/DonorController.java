package com.bloodlink.controller;

import com.bloodlink.dto.*;
import com.bloodlink.service.DonorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donors")
@RequiredArgsConstructor
@Tag(name = "Donors", description = "Blood donor registry operations")
public class DonorController {

    private final DonorService donorService;

    // ── POST /api/donors → 201 ───────────────────────────────────────────────
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new blood donor")
    public ResponseEntity<DonorResponse> registerDonor(
            @Valid @RequestBody DonorRequest request) {
        DonorResponse saved = donorService.registerDonor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── GET /api/donors?page=0&size=20 ──────────────────────────────────────
    @GetMapping
    @Operation(summary = "List all donors (paginated)")
    public ResponseEntity<Page<DonorResponse>> getAllDonors(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DonorResponse> result = donorService.getAllDonors(
                PageRequest.of(page, size, Sort.by("id").ascending()));
        return ResponseEntity.ok(result);
    }

    // ── GET /api/donors/search?bloodGroup=A+&city=Chennai ───────────────────
    @GetMapping("/search")
    @Operation(summary = "Search available donors by blood group and city")
    public ResponseEntity<List<DonorResponse>> searchDonors(
            @RequestParam String bloodGroup,
            @RequestParam String city) {
        return ResponseEntity.ok(donorService.searchDonors(bloodGroup, city));
    }

    // ── POST /api/donors/{id}/donations ─────────────────────────────────────
    @PostMapping("/{id}/donations")
    @Operation(summary = "Record a donation for a donor")
    public ResponseEntity<DonationRecordResponse> recordDonation(
            @PathVariable Long id,
            @Valid @RequestBody DonationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donorService.recordDonation(id, request));
    }

    // ── PUT /api/donors/{id}/unavailable ────────────────────────────────────
    @PutMapping("/{id}/unavailable")
    @Operation(summary = "Manually mark a donor as unavailable")
    public ResponseEntity<DonorResponse> markUnavailable(@PathVariable Long id) {
        return ResponseEntity.ok(donorService.markUnavailable(id));
    }

    // ── GET /api/donors/stats ────────────────────────────────────────────────
    @GetMapping("/stats")
    @Operation(summary = "Get registry statistics")
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(donorService.getStats());
    }
}
