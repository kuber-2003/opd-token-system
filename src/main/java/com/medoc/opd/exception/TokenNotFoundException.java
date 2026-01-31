package com.medoc.opd.exception;

/**
 * Exception thrown when a token is not found.
 */
public class TokenNotFoundException extends RuntimeException {
    public TokenNotFoundException(String tokenId) {
        super("Token not found with ID: " + tokenId);
    }
}
