export enum TrackType {
  Default = 'default',
  Dash = 'dash',
  HLS = 'hls',
  SmoothStreaming = 'smoothstreaming',
  /** Android only. Uses the signed server ABR streaming URL as `url`. */
  Sabr = 'sabr',
}
