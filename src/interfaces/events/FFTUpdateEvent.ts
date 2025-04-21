export interface FFTUpdateEvent {
  /**
   * android only. returns a FFT doubleArray of the current playback, sample rate = 4096
   **/
  data: number[];
}
