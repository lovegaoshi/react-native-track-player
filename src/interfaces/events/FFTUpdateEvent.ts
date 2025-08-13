/**
 * android & ios only. returns a FFT doubleArray of the current playback, sample rate = 4096
 **/
export interface FFTUpdateEvent {
  // FFT doubleArray of the current playback buffer
  data: number[];
  // ios only.
  amplitude?: number;
}
