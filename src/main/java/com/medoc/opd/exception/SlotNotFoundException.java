package com.medoc.opd.exception;

/**
 * Exception thrown when a slot is not found.
 */
public class SlotNotFoundException extends RuntimeException {
    public SlotNotFoundException(String slotId) {
        super("Slot not found with ID: " + slotId);
    }
}
