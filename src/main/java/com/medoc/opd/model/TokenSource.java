package com.medoc.opd.model;

/**
 * Defines the source of a token and its base priority.
 * Priority determines allocation order when slots are contested.
 */
public enum TokenSource {
    EMERGENCY(1000, "Emergency admission - highest priority"),
    PAID_PRIORITY(500, "Premium/paid priority service"),
    ONLINE_BOOKING(300, "Pre-scheduled online appointment"),
    FOLLOW_UP(200, "Return visit for existing patient"),
    WALK_IN(100, "Same-day walk-in patient");

    private final int basePriority;
    private final String description;

    TokenSource(int basePriority, String description) {
        this.basePriority = basePriority;
        this.description = description;
    }

    public int getBasePriority() {
        return basePriority;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Calculate dynamic priority based on wait time.
     * Patients gain +0.5 priority points per minute waiting after check-in.
     */
    public double calculateDynamicPriority(long waitTimeMinutes) {
        return basePriority + (waitTimeMinutes * 0.5);
    }
}
