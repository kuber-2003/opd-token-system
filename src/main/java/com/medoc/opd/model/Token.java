package com.medoc.opd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a patient token for OPD consultation.
 * Each token is associated with a specific slot and patient.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    
    @Builder.Default
    private String tokenId = UUID.randomUUID().toString();
    
    private String patientId;
    private String patientName;
    
    private String slotId;
    private String doctorId;
    
    private TokenSource source;
    
    @Builder.Default
    private TokenStatus status = TokenStatus.ALLOCATED;
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime checkedInAt;
    private LocalDateTime consultationStartedAt;
    private LocalDateTime consultationCompletedAt;
    
    private int tokenNumber;
    
    private String notes;
    
    @Builder.Default
    private int reallocatedCount = 0;

    /**
     * Calculate base priority from token source.
     */
    public double getBasePriority() {
        return source.getBasePriority();
    }

    /**
     * Calculate dynamic priority including wait time bonus.
     */
    public double getDynamicPriority() {
        if (checkedInAt == null) {
            return getBasePriority();
        }
        
        long waitTimeMinutes = java.time.Duration.between(
            checkedInAt, 
            LocalDateTime.now()
        ).toMinutes();
        
        return source.calculateDynamicPriority(waitTimeMinutes);
    }

    /**
     * Mark token as checked in.
     */
    public void checkIn() {
        this.status = TokenStatus.CHECKED_IN;
        this.checkedInAt = LocalDateTime.now();
    }

    /**
     * Start consultation.
     */
    public void startConsultation() {
        this.status = TokenStatus.IN_CONSULTATION;
        this.consultationStartedAt = LocalDateTime.now();
    }

    /**
     * Complete consultation.
     */
    public void complete() {
        this.status = TokenStatus.COMPLETED;
        this.consultationCompletedAt = LocalDateTime.now();
    }

    /**
     * Cancel token.
     */
    public void cancel() {
        this.status = TokenStatus.CANCELLED;
    }

    /**
     * Mark as no-show.
     */
    public void markNoShow() {
        this.status = TokenStatus.NO_SHOW;
    }

    /**
     * Mark as reallocated and increment count.
     */
    public void reallocate(String newSlotId) {
        this.slotId = newSlotId;
        this.status = TokenStatus.REALLOCATED;
        this.reallocatedCount++;
    }

    /**
     * Check if token can be reallocated.
     */
    public boolean canBeReallocated() {
        return status.isActive() && source != TokenSource.EMERGENCY;
    }

    /**
     * Get wait time in minutes.
     */
    public long getWaitTimeMinutes() {
        if (checkedInAt == null) return 0;
        
        LocalDateTime endTime = consultationStartedAt != null 
            ? consultationStartedAt 
            : LocalDateTime.now();
            
        return java.time.Duration.between(checkedInAt, endTime).toMinutes();
    }
}
