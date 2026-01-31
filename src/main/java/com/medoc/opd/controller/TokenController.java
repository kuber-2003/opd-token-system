package com.medoc.opd.controller;

import com.medoc.opd.model.Token;
import com.medoc.opd.model.TokenSource;
import com.medoc.opd.service.OPDTokenEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
@Tag(name = "Token Management", description = "APIs for managing patient tokens")
public class TokenController {

    private final OPDTokenEngine engine;

    @PostMapping
    @Operation(summary = "Allocate a token", description = "Allocate a new token to a patient for a doctor's slot")
    public ResponseEntity<Token> allocateToken(@Valid @RequestBody AllocateTokenRequest request) {
        Token token = engine.allocateToken(
                request.getPatientId(),
                request.getPatientName(),
                request.getDoctorId(),
                request.getSource(),
                request.getPreferredTime(),
                request.getNotes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @PostMapping("/emergency")
    @Operation(summary = "Allocate emergency token", description = "Allocate highest priority emergency token, can exceed slot capacity")
    public ResponseEntity<Token> allocateEmergencyToken(@Valid @RequestBody EmergencyTokenRequest request) {
        Token token = engine.allocateEmergencyToken(
                request.getPatientId(),
                request.getPatientName(),
                request.getDoctorId(),
                request.getNotes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @GetMapping("/{tokenId}")
    @Operation(summary = "Get token by ID", description = "Retrieve details of a specific token")
    public ResponseEntity<Token> getToken(@PathVariable String tokenId) {
        return ResponseEntity.ok(engine.getToken(tokenId));
    }

    @GetMapping
    @Operation(summary = "Get all tokens", description = "Retrieve all tokens in the system")
    public ResponseEntity<List<Token>> getAllTokens() {
        return ResponseEntity.ok(engine.getAllTokens());
    }

    @PostMapping("/{tokenId}/check-in")
    @Operation(summary = "Check-in patient", description = "Mark patient as checked in and waiting")
    public ResponseEntity<Token> checkIn(@PathVariable String tokenId) {
        return ResponseEntity.ok(engine.checkIn(tokenId));
    }

    @PostMapping("/{tokenId}/start-consultation")
    @Operation(summary = "Start consultation", description = "Mark consultation as started")
    public ResponseEntity<Token> startConsultation(@PathVariable String tokenId) {
        return ResponseEntity.ok(engine.startConsultation(tokenId));
    }

    @PostMapping("/{tokenId}/complete-consultation")
    @Operation(summary = "Complete consultation", description = "Mark consultation as completed")
    public ResponseEntity<Token> completeConsultation(@PathVariable String tokenId) {
        return ResponseEntity.ok(engine.completeConsultation(tokenId));
    }

    @DeleteMapping("/{tokenId}")
    @Operation(summary = "Cancel token", description = "Cancel a token and free up slot capacity")
    public ResponseEntity<Token> cancelToken(@PathVariable String tokenId) {
        return ResponseEntity.ok(engine.cancelToken(tokenId));
    }

    @PostMapping("/{tokenId}/no-show")
    @Operation(summary = "Mark as no-show", description = "Mark patient as no-show and free up capacity")
    public ResponseEntity<Token> markNoShow(@PathVariable String tokenId) {
        return ResponseEntity.ok(engine.markNoShow(tokenId));
    }

    @GetMapping("/queue/{doctorId}")
    @Operation(summary = "Get doctor's queue", description = "Get current queue for a doctor sorted by priority")
    public ResponseEntity<List<Token>> getDoctorQueue(@PathVariable String doctorId) {
        return ResponseEntity.ok(engine.getDoctorQueue(doctorId));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get statistics", description = "Get token statistics for a doctor or overall system")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam(required = false) String doctorId) {
        return ResponseEntity.ok(engine.getStatistics(doctorId));
    }

    // Request DTOs
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocateTokenRequest {
        @NotBlank(message = "Patient ID is required")
        private String patientId;

        @NotBlank(message = "Patient name is required")
        private String patientName;

        @NotBlank(message = "Doctor ID is required")
        private String doctorId;

        @NotNull(message = "Token source is required")
        private TokenSource source;

        @NotNull(message = "Preferred time is required")
        private LocalDateTime preferredTime;

        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmergencyTokenRequest {
        @NotBlank(message = "Patient ID is required")
        private String patientId;

        @NotBlank(message = "Patient name is required")
        private String patientName;

        @NotBlank(message = "Doctor ID is required")
        private String doctorId;

        private String notes;
    }
}
