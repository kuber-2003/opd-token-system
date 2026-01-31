package com.medoc.opd.controller;

import com.medoc.opd.model.TimeSlot;
import com.medoc.opd.service.OPDTokenEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
@Tag(name = "Slot Management", description = "APIs for managing doctor time slots")
public class SlotController {

    private final OPDTokenEngine engine;

    @PostMapping
    @Operation(summary = "Create a new time slot", description = "Creates a new time slot for a doctor with specified capacity")
    public ResponseEntity<TimeSlot> createSlot(@Valid @RequestBody CreateSlotRequest request) {
        TimeSlot slot = engine.createSlot(
                request.getDoctorId(),
                request.getDoctorName(),
                request.getDepartment(),
                request.getStartTime(),
                request.getEndTime(),
                request.getMaxCapacity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(slot);
    }

    @GetMapping("/{slotId}")
    @Operation(summary = "Get slot by ID", description = "Retrieve details of a specific time slot")
    public ResponseEntity<TimeSlot> getSlot(@PathVariable String slotId) {
        return ResponseEntity.ok(engine.getSlot(slotId));
    }

    @GetMapping
    @Operation(summary = "Get all slots", description = "Retrieve all time slots in the system")
    public ResponseEntity<List<TimeSlot>> getAllSlots() {
        return ResponseEntity.ok(engine.getAllSlots());
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get doctor's slots", description = "Retrieve all time slots for a specific doctor")
    public ResponseEntity<List<TimeSlot>> getDoctorSlots(@PathVariable String doctorId) {
        return ResponseEntity.ok(engine.getDoctorSlots(doctorId));
    }

    @PutMapping("/{slotId}/capacity")
    @Operation(summary = "Adjust slot capacity", description = "Dynamically adjust the capacity of a time slot")
    public ResponseEntity<TimeSlot> adjustCapacity(
            @PathVariable String slotId,
            @Valid @RequestBody AdjustCapacityRequest request) {
        return ResponseEntity.ok(engine.adjustSlotCapacity(slotId, request.getNewCapacity()));
    }

    // Request DTOs
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSlotRequest {
        @NotBlank(message = "Doctor ID is required")
        private String doctorId;

        @NotBlank(message = "Doctor name is required")
        private String doctorName;

        private String department;

        @NotNull(message = "Start time is required")
        private LocalDateTime startTime;

        @NotNull(message = "End time is required")
        private LocalDateTime endTime;

        @Min(value = 1, message = "Max capacity must be at least 1")
        private int maxCapacity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdjustCapacityRequest {
        @Min(value = 0, message = "Capacity cannot be negative")
        private int newCapacity;
    }
}
