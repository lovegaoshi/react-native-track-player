package com.doublesymmetry.trackplayer.sabr.parser.exceptions;

/**
 * The server told us to back off, honor it instead of re-requesting
 * immediately - otherwise we burn exactly the waiting period the server asked for.
 */
public class SabrBackoffError extends SabrPropagationError {
    public final int backoffMs;

    public SabrBackoffError(int backoffMs) {
        super(String.format("Server asked to back off for %s ms", backoffMs));
        this.backoffMs = backoffMs;
    }
}