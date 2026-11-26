package com.doublesymmetry.trackplayer.sabr;

import androidx.annotation.Nullable;

import com.doublesymmetry.trackplayer.sabr.manifest.SabrManifest;

import androidx.media3.exoplayer.source.chunk.Chunk;
import androidx.media3.exoplayer.upstream.Allocator;

/**
 * Minimal replacement for the DASH emsg handler used by SmartTube's SABR port.
 *
 * <p>SABR responses carry their control signals (redirect, backoff, reload player response, etc.)
 * in UMP parts and are handled by {@code SabrProcessor}. The DASH-style emsg refresh path is not
 * used, so this class only preserves the API expected by the copied media period/chunk source.
 */
public final class PlayerEmsgHandler {
    /** Callbacks for player emsg events encountered during DASH live stream. */
    public interface PlayerEmsgCallback {
        void onDashManifestRefreshRequested();

        void onDashManifestPublishTimeExpired(long expiredManifestPublishTimeUs);
    }

    private SabrManifest manifest;

    public PlayerEmsgHandler(
            SabrManifest manifest, PlayerEmsgCallback playerEmsgCallback, Allocator allocator) {
        this.manifest = manifest;
    }

    public void updateManifest(SabrManifest newManifest) {
        manifest = newManifest;
    }

    /* package */ boolean maybeRefreshManifestBeforeLoadingNextChunk(long presentationPositionUs) {
        return false;
    }

    /* package */ boolean maybeRefreshManifestOnLoadingError(Chunk chunk) {
        return false;
    }

    /* package */ void onChunkLoadCompleted(Chunk chunk) {
        // No emsg refresh is supported for SABR manifests.
    }

    public PlayerTrackEmsgHandler newPlayerTrackEmsgHandler() {
        return new PlayerTrackEmsgHandler();
    }

    public void release() {
        // No resources to release.
    }

    public static boolean isPlayerEmsgEvent(String schemeIdUri, String value) {
        return "urn:mpeg:sabr:event:2025".equals(schemeIdUri)
                && ("1".equals(value) || "2".equals(value) || "3".equals(value));
    }

    /** Handles emsg messages for a specific track for the player. */
    public final class PlayerTrackEmsgHandler {
        private PlayerTrackEmsgHandler() {
        }

        public boolean maybeRefreshManifestBeforeLoadingNextChunk(long presentationPositionUs) {
            return false;
        }

        public void onChunkLoadCompleted(Chunk chunk) {
            // No emsg refresh is supported for SABR manifests.
        }

        public boolean maybeRefreshManifestOnLoadingError(Chunk chunk) {
            return false;
        }

        public void release() {
            // No resources to release.
        }
    }
}
