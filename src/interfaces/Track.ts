import type { PitchAlgorithm, TrackType } from '../constants';
import { ResourceObject } from './ResourceObject';
import type { TrackMetadataBase } from './TrackMetadataBase';

/** YouTube client identity used in the SABR request body. Must match the poToken's client. */
export interface SabrClientInfo {
  /** A StreamerContext.ClientName value, for example `ANDROID`, `WEB` or `TVHTML5`. */
  name: string;
  version: string;
  osName: string;
  osVersion: string;
}

/** One SABR representation, normally derived from a YouTube player response. */
export interface SabrFormat {
  itag: number;
  /** Container MIME type, for example `audio/mp4` or `video/webm`. */
  mimeType: string;
  codecs?: string;
  bitrate?: number;
  width?: number;
  height?: number;
  fps?: number;
  language?: string;
  qualityLabel?: string;
  drc?: boolean;
  lmt?: number;
  xtags?: string;
  audioTrackId?: string;
}

/**
 * Android-only SABR configuration. A URL and poToken alone are insufficient: YouTube's SABR
 * endpoint also needs the streaming config, client identity, video id and format metadata.
 */
export interface SabrTrackOptions {
  /** Base64 URL-safe poToken. */
  poToken: string;
  /** Base64 URL-safe `videoPlaybackUstreamerConfig` from the player response. */
  videoPlaybackUstreamerConfig: string;
  videoId: string;
  client: SabrClientInfo;
  formats: SabrFormat[];
  subtitles?: SabrFormat[];
  visitorCookie?: string;
  /** Duration in milliseconds. Falls back to the track's `duration` (seconds). */
  durationMs?: number;
}

export interface Track extends TrackMetadataBase {
  url: string;
  type?: TrackType;
  /** The user agent HTTP header */
  userAgent?: string;
  /** Mime type of the media file */
  contentType?: string;
  /** (Android only) Use bounded HTTP range requests instead of open ended ones. */
  setRange?: boolean;
  /** (Android only) Required when `type` is `sabr`. */
  sabr?: SabrTrackOptions;
  /** (iOS only) The pitch algorithm to apply to the sound. */
  pitchAlgorithm?: PitchAlgorithm;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  headers?: { [key: string]: any };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  [key: string]: any;
}

export type AddTrack = Track & {
  url: string | ResourceObject;
  artwork?: string | ResourceObject;
};
