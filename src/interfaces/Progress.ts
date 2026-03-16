export interface Progress {
  /**
   * The playback position of the current track in seconds.
   * See https://doublesymmetry.github.io/react-native-track-player/docs/api/functions/player#getposition
   **/
  position: number;
  /** The duration of the current track in seconds.
   * See https://doublesymmetry.github.io/react-native-track-player/docs/api/functions/player#getduration
   **/
  duration: number;
  /**
   * The buffered position of the current track in seconds.
   **/
  buffered: number;
}
