package com.medoc.opd.exception;

/**
 * Exception thrown when an invalid state transition is attempted on a token.
 */
public class InvalidTokenStateException extends RuntimeException {
    public InvalidTokenStateException(String message) {
        super(message);
    }
}
