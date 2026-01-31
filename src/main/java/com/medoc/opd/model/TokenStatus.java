package com.medoc.opd.model;

/**
 * Represents the lifecycle status of a token.
 */
public enum TokenStatus {
    ALLOCATED("Token allocated, patient not yet arrived"),
    CHECKED_IN("Patient checked in, waiting for consultation"),
    IN_CONSULTATION("Patient currently being consulted"),
    COMPLETED("Consultation completed"),
    CANCELLED("Token cancelled by patient or system"),
    NO_SHOW("Patient did not arrive for allocated slot"),
    REALLOCATED("Token moved to different slot due to capacity constraints");

    private final String description;

    TokenStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if token is in an active state (can still be consulted).
     */
    public boolean isActive() {
        return this == ALLOCATED || this == CHECKED_IN || this == REALLOCATED;
    }

    /**
     * Check if token is in a terminal state (cannot be modified).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == NO_SHOW;
    }
}
