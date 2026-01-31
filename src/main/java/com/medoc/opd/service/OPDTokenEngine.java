package com.medoc.opd.service;

import com.medoc.opd.exception.*;
import com.medoc.opd.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Core OPD Token Allocation Engine.
 * Handles token allocation, reallocation, and slot management with elastic
 * capacity.
 */
@Slf4j
@Service
public class OPDTokenEngine {

    // In-memory storage (use database in production)
    private final Map<String, TimeSlot> slots = new ConcurrentHashMap<>();
    private final Map<String, Token> tokens = new ConcurrentHashMap<>();
    private final AtomicInteger tokenCounter = new AtomicInteger(1);

    @Value("${opd.simulation.mode:false}")
    private boolean simulationMode = false;

    /**
     * Enable or disable simulation mode.
     * In simulation mode, time-based slot filtering is relaxed.
     */
    public void setSimulationMode(boolean simulationMode) {
        this.simulationMode = simulationMode;
    }

    /**
     * Create a new time slot for a doctor.
     */
    public TimeSlot createSlot(String doctorId, String doctorName, String department,
            LocalDateTime startTime, LocalDateTime endTime,
            int maxCapacity) {
        TimeSlot slot = TimeSlot.builder()
                .doctorId(doctorId)
                .doctorName(doctorName)
                .department(department)
                .startTime(startTime)
                .endTime(endTime)
                .maxCapacity(maxCapacity)
                .build();

        slots.put(slot.getSlotId(), slot);
        log.info("Created slot {} for Dr. {} ({}-{})", slot.getSlotId(), doctorName, startTime, endTime);
        return slot;
    }

    /**
     * Allocate a token to a patient.
     * Finds the best available slot based on preferred time and capacity.
     */
    public Token allocateToken(String patientId, String patientName, String doctorId,
            TokenSource source, LocalDateTime preferredTime, String notes) {

        // Find best available slot
        TimeSlot bestSlot = findBestSlot(doctorId, preferredTime, source);

        if (bestSlot == null) {
            throw new SlotCapacityExceededException(
                    "No available slots found for doctor " + doctorId + " near preferred time " + preferredTime);
        }

        // Create and allocate token
        Token token = Token.builder()
                .patientId(patientId)
                .patientName(patientName)
                .slotId(bestSlot.getSlotId())
                .doctorId(doctorId)
                .source(source)
                .tokenNumber(tokenCounter.getAndIncrement())
                .notes(notes)
                .build();

        tokens.put(token.getTokenId(), token);
        bestSlot.incrementOccupancy();

        log.info("Allocated token {} to patient {} in slot {} (source: {})",
                token.getTokenNumber(), patientName, bestSlot.getSlotId(), source);

        return token;
    }

    /**
     * Allocate emergency token - highest priority, can exceed capacity.
     */
    public Token allocateEmergencyToken(String patientId, String patientName,
            String doctorId, String notes) {

        // For emergencies, find current or next immediate slot
        TimeSlot emergencySlot = findEmergencySlot(doctorId);

        if (emergencySlot == null) {
            throw new SlotNotFoundException("No active or upcoming slots for doctor " + doctorId);
        }

        // Create emergency token
        Token emergencyToken = Token.builder()
                .patientId(patientId)
                .patientName(patientName)
                .slotId(emergencySlot.getSlotId())
                .doctorId(doctorId)
                .source(TokenSource.EMERGENCY)
                .tokenNumber(tokenCounter.getAndIncrement())
                .notes("EMERGENCY: " + notes)
                .build();

        tokens.put(emergencyToken.getTokenId(), emergencyToken);
        emergencySlot.incrementOccupancy();

        log.warn("EMERGENCY token {} allocated to {} in slot {} (capacity may be exceeded)",
                emergencyToken.getTokenNumber(), patientName, emergencySlot.getSlotId());

        // If slot is now overcapacity, trigger reallocation
        if (emergencySlot.getCurrentOccupancy() > emergencySlot.getMaxCapacity()) {
            reallocateOverflowTokens(emergencySlot);
        }

        return emergencyToken;
    }

    /**
     * Find the best slot for allocation based on time proximity and capacity.
     */
    private TimeSlot findBestSlot(String doctorId, LocalDateTime preferredTime, TokenSource source) {
        LocalDateTime now = LocalDateTime.now();

        return slots.values().stream()
                .filter(slot -> slot.getDoctorId().equals(doctorId))
                .filter(slot -> slot.isActive())
                .filter(slot -> simulationMode || slot.isFuture(now) || slot.isOngoing(now))
                // For non-emergency, only consider slots with capacity
                .filter(slot -> source == TokenSource.EMERGENCY || slot.hasCapacity())
                .min(Comparator.comparingDouble(slot -> calculateSlotScore(slot, preferredTime)))
                .orElse(null);
    }

    /**
     * Find emergency slot (current or next immediate slot).
     */
    private TimeSlot findEmergencySlot(String doctorId) {
        LocalDateTime now = LocalDateTime.now();

        return slots.values().stream()
                .filter(slot -> slot.getDoctorId().equals(doctorId))
                .filter(slot -> slot.isActive())
                .filter(slot -> simulationMode || slot.isFuture(now) || slot.isOngoing(now))
                .min(Comparator.comparing(TimeSlot::getStartTime))
                .orElse(null);
    }

    /**
     * Calculate slot score for prioritization (lower is better).
     * Factors: time proximity, capacity utilization.
     */
    private double calculateSlotScore(TimeSlot slot, LocalDateTime preferredTime) {
        // Time proximity score (in minutes)
        long timeProximity = Math.abs(
                java.time.Duration.between(preferredTime, slot.getStartTime()).toMinutes());

        // Capacity penalty (prefer less crowded slots)
        double capacityPenalty = (slot.getCurrentOccupancy() * 100.0) / slot.getMaxCapacity();

        // Combined score
        return timeProximity + capacityPenalty;
    }

    /**
     * Reallocate overflow tokens when slot exceeds capacity.
     * Moves lowest priority non-emergency tokens to next available slots.
     */
    private void reallocateOverflowTokens(TimeSlot overflowSlot) {
        int overflow = overflowSlot.getCurrentOccupancy() - overflowSlot.getMaxCapacity();

        if (overflow <= 0) {
            return; // No overflow
        }

        log.warn("Slot {} has overflow of {}. Initiating reallocation.", overflowSlot.getSlotId(), overflow);

        // Find tokens in this slot, sorted by priority (lowest first)
        List<Token> tokensInSlot = tokens.values().stream()
                .filter(t -> t.getSlotId().equals(overflowSlot.getSlotId()))
                .filter(Token::canBeReallocated)
                .sorted(Comparator.comparingDouble(Token::getDynamicPriority))
                .limit(overflow)
                .collect(Collectors.toList());

        // Reallocate each token
        for (Token token : tokensInSlot) {
            TimeSlot newSlot = findNextAvailableSlot(overflowSlot.getDoctorId(), overflowSlot.getEndTime());

            if (newSlot != null) {
                // Move token to new slot
                overflowSlot.decrementOccupancy();
                newSlot.incrementOccupancy();
                token.reallocate(newSlot.getSlotId());

                log.info("Reallocated token {} from slot {} to slot {}",
                        token.getTokenNumber(), overflowSlot.getSlotId(), newSlot.getSlotId());
            } else {
                log.error("Could not find alternative slot for token {}. Patient must be notified.",
                        token.getTokenNumber());
            }
        }
    }

    /**
     * Find next available slot after given time.
     */
    private TimeSlot findNextAvailableSlot(String doctorId, LocalDateTime afterTime) {
        return slots.values().stream()
                .filter(slot -> slot.getDoctorId().equals(doctorId))
                .filter(slot -> slot.isActive())
                .filter(slot -> slot.getStartTime().isAfter(afterTime))
                .filter(TimeSlot::hasCapacity)
                .min(Comparator.comparing(TimeSlot::getStartTime))
                .orElse(null);
    }

    /**
     * Cancel a token and free up slot capacity.
     */
    public Token cancelToken(String tokenId) {
        Token token = getToken(tokenId);

        if (token.getStatus().isTerminal()) {
            throw new InvalidTokenStateException(
                    "Cannot cancel token in " + token.getStatus() + " state");
        }

        TimeSlot slot = getSlot(token.getSlotId());
        slot.decrementOccupancy();
        token.cancel();

        log.info("Cancelled token {} for patient {}", token.getTokenNumber(), token.getPatientName());
        return token;
    }

    /**
     * Mark token as no-show.
     */
    public Token markNoShow(String tokenId) {
        Token token = getToken(tokenId);

        if (token.getStatus() != TokenStatus.ALLOCATED && token.getStatus() != TokenStatus.CHECKED_IN) {
            throw new InvalidTokenStateException(
                    "Cannot mark as no-show from " + token.getStatus() + " state");
        }

        TimeSlot slot = getSlot(token.getSlotId());
        slot.decrementOccupancy();
        token.markNoShow();

        log.info("Marked token {} as no-show", token.getTokenNumber());
        return token;
    }

    /**
     * Check-in a patient.
     */
    public Token checkIn(String tokenId) {
        Token token = getToken(tokenId);

        if (token.getStatus() != TokenStatus.ALLOCATED && token.getStatus() != TokenStatus.REALLOCATED) {
            throw new InvalidTokenStateException(
                    "Cannot check-in token in " + token.getStatus() + " state");
        }

        token.checkIn();
        log.info("Checked in token {} for patient {}", token.getTokenNumber(), token.getPatientName());
        return token;
    }

    /**
     * Start consultation.
     */
    public Token startConsultation(String tokenId) {
        Token token = getToken(tokenId);

        if (token.getStatus() != TokenStatus.CHECKED_IN) {
            throw new InvalidTokenStateException(
                    "Cannot start consultation from " + token.getStatus() + " state");
        }

        token.startConsultation();
        log.info("Started consultation for token {}", token.getTokenNumber());
        return token;
    }

    /**
     * Complete consultation.
     */
    public Token completeConsultation(String tokenId) {
        Token token = getToken(tokenId);

        if (token.getStatus() != TokenStatus.IN_CONSULTATION) {
            throw new InvalidTokenStateException(
                    "Cannot complete consultation from " + token.getStatus() + " state");
        }

        token.complete();
        log.info("Completed consultation for token {}", token.getTokenNumber());
        return token;
    }

    /**
     * Adjust slot capacity dynamically.
     */
    public TimeSlot adjustSlotCapacity(String slotId, int newCapacity) {
        TimeSlot slot = getSlot(slotId);
        int oldCapacity = slot.getMaxCapacity();
        slot.setMaxCapacity(newCapacity);

        log.info("Adjusted capacity for slot {} from {} to {}", slotId, oldCapacity, newCapacity);

        // If capacity reduced and now overcapacity, reallocate
        if (newCapacity < slot.getCurrentOccupancy()) {
            reallocateOverflowTokens(slot);
        }

        return slot;
    }

    /**
     * Get current queue for a doctor (sorted by priority).
     */
    public List<Token> getDoctorQueue(String doctorId) {
        LocalDateTime now = LocalDateTime.now();

        return tokens.values().stream()
                .filter(t -> t.getDoctorId().equals(doctorId))
                .filter(t -> t.getStatus().isActive())
                .filter(t -> {
                    TimeSlot slot = slots.get(t.getSlotId());
                    return slot != null && (slot.isOngoing(now) || slot.isFuture(now));
                })
                .sorted(Comparator.comparingDouble(Token::getDynamicPriority).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get all slots for a doctor.
     */
    public List<TimeSlot> getDoctorSlots(String doctorId) {
        return slots.values().stream()
                .filter(slot -> slot.getDoctorId().equals(doctorId))
                .sorted(Comparator.comparing(TimeSlot::getStartTime))
                .collect(Collectors.toList());
    }

    /**
     * Get statistics for a doctor or overall.
     */
    public Map<String, Object> getStatistics(String doctorId) {
        List<Token> relevantTokens = doctorId != null
                ? tokens.values().stream().filter(t -> t.getDoctorId().equals(doctorId)).collect(Collectors.toList())
                : new ArrayList<>(tokens.values());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTokens", relevantTokens.size());
        stats.put("activeTokens", relevantTokens.stream().filter(t -> t.getStatus().isActive()).count());
        stats.put("completedTokens",
                relevantTokens.stream().filter(t -> t.getStatus() == TokenStatus.COMPLETED).count());
        stats.put("cancelledTokens",
                relevantTokens.stream().filter(t -> t.getStatus() == TokenStatus.CANCELLED).count());
        stats.put("noShowTokens", relevantTokens.stream().filter(t -> t.getStatus() == TokenStatus.NO_SHOW).count());
        stats.put("emergencyTokens",
                relevantTokens.stream().filter(t -> t.getSource() == TokenSource.EMERGENCY).count());

        if (doctorId != null) {
            List<TimeSlot> doctorSlots = getDoctorSlots(doctorId);
            stats.put("totalSlots", doctorSlots.size());
            stats.put("averageUtilization", doctorSlots.stream()
                    .mapToDouble(TimeSlot::getUtilizationPercentage)
                    .average()
                    .orElse(0.0));
        }

        return stats;
    }

    // Helper methods
    public TimeSlot getSlot(String slotId) {
        TimeSlot slot = slots.get(slotId);
        if (slot == null) {
            throw new SlotNotFoundException(slotId);
        }
        return slot;
    }

    public Token getToken(String tokenId) {
        Token token = tokens.get(tokenId);
        if (token == null) {
            throw new TokenNotFoundException(tokenId);
        }
        return token;
    }

    public List<TimeSlot> getAllSlots() {
        return new ArrayList<>(slots.values());
    }

    public List<Token> getAllTokens() {
        return new ArrayList<>(tokens.values());
    }
}
