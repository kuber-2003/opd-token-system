package com.medoc.opd.exception;

/**
 * Exception thrown when slot capacity is exceeded.
 */
public class SlotCapacityExceededException extends RuntimeException {
    public SlotCapacityExceededException(String slotId) {
        super("Slot capacity exceeded for slot ID: " + slotId);
    }
    
    public SlotCapacityExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
