package com.doublesymmetry.trackplayer.sabr.parser.parts;

public class RefreshPlayerResponseSabrPart implements SabrPart {
    public final Reason reason;
    public final String reloadPlaybackToken;

    public RefreshPlayerResponseSabrPart(Reason reason, String reloadPlaybackToken) {
        this.reason = reason;
        this.reloadPlaybackToken = reloadPlaybackToken;
    }

    public enum Reason {
        UNKNOWN,
        SABR_URL_EXPIRY,
        SABR_RELOAD_PLAYER_RESPONSE
    }
}
