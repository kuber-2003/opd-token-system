package com.medoc.opd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a time slot for a specific doctor.
 * Each slot has a fixed capacity and time window.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {
    
    @Builder.Default
    private String slotId = UUID.randomUUID().toString();
    
    private String doctorId;
    private String doctorName;
    private String department;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private int maxCapacity;
    
    @Builder.Default
    private int currentOccupancy = 0;
    
    @Builder.Default
    private boolean isActive = true;
    
    private String notes;

    /**
     * Check if slot has available capacity.
     */
    public boolean hasCapacity() {
        return currentOccupancy < maxCapacity;
    }

    /**
     * Get remaining capacity in this slot.
     */
    public int getRemainingCapacity() {
        return Math.max(0, maxCapacity - currentOccupancy);
    }

    /**
     * Check if this slot is currently ongoing.
     */
    public boolean isOngoing(LocalDateTime currentTime) {
        return !currentTime.isBefore(startTime) && currentTime.isBefore(endTime);
    }

    /**
     * Check if this slot is in the future.
     */
    public boolean isFuture(LocalDateTime currentTime) {
        return currentTime.isBefore(startTime);
    }

    /**
     * Increment occupancy (thread-safe for demo purposes).
     */
    public synchronized void incrementOccupancy() {
        this.currentOccupancy++;
    }

    /**
     * Decrement occupancy (thread-safe for demo purposes).
     */
    public synchronized void decrementOccupancy() {
        if (this.currentOccupancy > 0) {
            this.currentOccupancy--;
        }
    }

    /**
     * Calculate utilization percentage.
     */
    public double getUtilizationPercentage() {
        if (maxCapacity == 0) return 0.0;
        return (currentOccupancy * 100.0) / maxCapacity;
    }
}
