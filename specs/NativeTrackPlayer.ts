import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  setupPlayer(options?: PlayerOptions, background?: boolean): Promise<void>;
}

const module = TurboModuleRegistry.getEnforcing<Spec>('TrackPlayer');
export default module;

// https://github.com/facebook/react-native/issues/38769
// here we go...

export enum AndroidAudioContentType {
  /**
   * Content type value to use when the content type is music.
   *
   * See https://developer.android.com/reference/android/media/AudioAttributes#CONTENT_TYPE_MUSIC
   */
  Music = 'music',
  /**
   * Content type value to use when the content type is speech.
   *
   * See https://developer.android.com/reference/android/media/AudioAttributes#CONTENT_TYPE_SPEECH
   */
  Speech = 'speech',
  /**
   * Content type value to use when the content type is a sound used to
   * accompany a user action, such as a beep or sound effect expressing a key
   * click, or event, such as the type of a sound for a bonus being received in
   * a game. These sounds are mostly synthesized or short Foley sounds.
   *
   * See https://developer.android.com/reference/android/media/AudioAttributes#CONTENT_TYPE_SONIFICATION
   */
  Sonification = 'sonification',
  /**
   * Content type value to use when the content type is a soundtrack, typically
   * accompanying a movie or TV program.
   */
  Movie = 'movie',
  /**
   * Content type value to use when the content type is unknown, or other than
   * the ones defined.
   *
   * See https://developer.android.com/reference/android/media/AudioAttributes#CONTENT_TYPE_UNKNOWN
   */
  Unknown = 'unknown',
}

export enum IOSCategory {
  /**
   * The category for playing recorded music or other sounds that are central
   * to the successful use of your app.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/category/1616509-playback
   **/
  Playback = 'playback',
  /**
   * The category for recording (input) and playback (output) of audio, such as
   * for a Voice over Internet Protocol (VoIP) app.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/category/1616568-playandrecord
   **/
  PlayAndRecord = 'playAndRecord',
  /**
   * The category for routing distinct streams of audio data to different
   * output devices at the same time.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/category/1616484-multiroute
   **/
  MultiRoute = 'multiRoute',
  /**
   * The category for an app in which sound playback is nonprimary — that is,
   * your app also works with the sound turned off.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/category/1616560-ambient
   **/
  Ambient = 'ambient',
  /**
   * The default audio session category.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/category/1616488-soloambient
   **/
  SoloAmbient = 'soloAmbient',
  /**
   * The category for recording audio while also silencing playback audio.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/category/1616451-record
   **/
  Record = 'record',
}

export enum IOSCategoryMode {
  /**
   * The default audio session mode.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/mode/1616579-default
   **/
  Default = 'default',
  /**
   * A mode that the GameKit framework sets on behalf of an application that
   * uses GameKit’s voice chat service.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/mode/1616511-gamechat
   **/
  GameChat = 'gameChat',
  /**
   * A mode that indicates that your app is performing measurement of audio
   * input or output.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/mode/1616608-measurement
   **/
  Measurement = 'measurement',
  /** A mode that indicates that your app is playing back movie content.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/mode/1616623-movieplayback
   **/
  MoviePlayback = 'moviePlayback',
  /** A mode used for continuous spoken audio to pause the audio when another
   * app plays a short audio prompt. See https://developer.apple.com/documentation/avfaudio/avaudiosession/mode/1616510-spokenaudio */
  SpokenAudio = 'spokenAudio',
  /**
   * A mode that indicates that your app is engaging in online video conferencing.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/mode/1616590-videochat
   **/
  VideoChat = 'videoChat',
  /**
   * A mode that indicates that your app is recording a movie.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/mode/1616535-videorecording
   **/
  VideoRecording = 'videoRecording',
  /**
   * A mode that indicates that your app is performing two-way voice communication,
   * such as using Voice over Internet Protocol (VoIP).
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/mode/1616455-voicechat
   **/
  VoiceChat = 'voiceChat',
  /**
   * A mode that indicates that your app plays audio using text-to-speech.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/mode/2962803-voiceprompt
   **/
  VoicePrompt = 'voicePrompt',
}

export enum IOSCategoryOptions {
  /**
   * An option that indicates whether audio from this session mixes with audio
   * from active sessions in other audio apps.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/categoryoptions/1616611-mixwithothers
   **/
  MixWithOthers = 'mixWithOthers',
  /**
   * An option that reduces the volume of other audio sessions while audio from
   * this session plays.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/categoryoptions/1616618-duckothers
   **/
  DuckOthers = 'duckOthers',
  /**
   * An option that determines whether to pause spoken audio content from other
   * sessions when your app plays its audio.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/categoryoptions/1616534-interruptspokenaudioandmixwithot
   **/
  InterruptSpokenAudioAndMixWithOthers = 'interruptSpokenAudioAndMixWithOthers',
  /**
   * An option that determines whether Bluetooth hands-free devices appear as
   * available input routes.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/categoryoptions/1616518-allowbluetooth
   **/
  AllowBluetooth = 'allowBluetooth',
  /**
   * An option that determines whether you can stream audio from this session
   * to Bluetooth devices that support the Advanced Audio Distribution Profile (A2DP).
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/categoryoptions/1771735-allowbluetootha2dp
   **/
  AllowBluetoothA2DP = 'allowBluetoothA2DP',
  /**
   * An option that determines whether you can stream audio from this session
   * to AirPlay devices.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/categoryoptions/1771736-allowairplay
   **/
  AllowAirPlay = 'allowAirPlay',
  /**
   * An option that determines whether audio from the session defaults to the
   * built-in speaker instead of the receiver.
   * See https://developer.apple.com/documentation/avfaudio/avaudiosession/categoryoptions/1616462-defaulttospeaker
   **/
  DefaultToSpeaker = 'defaultToSpeaker',
}

export interface PlayerOptions {
  /**
   * Minimum duration of media that the player will attempt to buffer in seconds.
   *
   * Supported on Android & iOS.
   *
   * @throws Will throw on Android if min buffer is higher than max buffer.
   * @default 50
   */
  minBuffer?: number;
  /**
   * Maximum duration of media that the player will attempt to buffer in seconds.
   * Max buffer may not be lower than min buffer.
   *
   * Supported on Android only.
   *
   * @throws Will throw if max buffer is lower than min buffer.
   * @default 50
   */
  maxBuffer?: number;
  /**
   * Duration in seconds that should be kept in the buffer behind the current
   * playhead time.
   *
   * Supported on Android only.
   *
   * @default 0
   */
  backBuffer?: number;
  /**
   * Duration of media in seconds that must be buffered for playback to start or
   * resume following a user action such as a seek.
   *
   * Supported on Android only.
   *
   * @default 2.5
   */
  playBuffer?: number;
  /**
   * Maximum cache size in kilobytes.
   *
   * Supported on Android only.
   *
   * @default 0
   */
  maxCacheSize?: number;
  /**
   * [AVAudioSession.Category](https://developer.apple.com/documentation/avfoundation/avaudiosession/1616615-category)
   * for iOS. Sets on `play()`.
   */
  iosCategory?: IOSCategory;
  /**
   * (iOS only) The audio session mode, together with the audio session category,
   * indicates to the system how you intend to use audio in your app. You can use
   * a mode to configure the audio system for specific use cases such as video
   * recording, voice or video chat, or audio analysis.
   * Sets on `play()`.
   *
   * See https://developer.apple.com/documentation/avfoundation/avaudiosession/1616508-mode
   */
  iosCategoryMode?: IOSCategoryMode;
  /**
   * [AVAudioSession.CategoryOptions](https://developer.apple.com/documentation/avfoundation/avaudiosession/1616503-categoryoptions) for iOS.
   * Sets on `play()`.
   */
  iosCategoryOptions?: IOSCategoryOptions[];
  /**
   * (Android only) The audio content type indicates to the android system how
   * you intend to use audio in your app.
   *
   * With `autoHandleInterruptions: true` and
   * `androidAudioContentType: AndroidAudioContentType.Speech`, the audio will be
   * paused during short interruptions, such as when a message arrives.
   * Otherwise the playback volume is reduced while the notification is playing.
   *
   * @default AndroidAudioContentType.Music
   */
  androidAudioContentType?: AndroidAudioContentType;
  /**
   * auto pause playback when playback device changes from headset to speaker.
   * @default true
   */
  androidHandleAudioBecomingNoisy?: boolean;
  /**
   * always show next and previous as android player command. this overrides
   * exoplayer disabling the next button on playmode != all and at queue's end.
   * @default true
   */
  androidAlwaysShowNext?: boolean;
  /**
   * enables exoplayer's skipSilence parser
   * @default false
   */
  androidSkipSilence?: boolean;
  /**
   * set android exoplayer wake mode. 1 is WAKE_MODE_LOCAL, 2 is WAKE_MODE_NETWORK,
   * and others is WAKE_MODE_NONE.
   * @default 0
   */
  androidWakeMode?: number;
  /**
   * Indicates whether the player should automatically delay playback in order to minimize stalling.
   * Defaults to `true`.
   * @deprecated This option has been nominated for removal in a future version
   * of RNTP. If you have this set to `true`, you can safely remove this from
   * the options. If you are setting this to `false` and have a reason for that,
   * please post a comment in the following discussion: https://github.com/doublesymmetry/react-native-track-player/pull/1695
   * and describe why you are doing so.
   */
  waitForBuffer?: boolean;
  /**
   * Indicates whether the player should automatically update now playing metadata data in control center / notification.
   * Defaults to `true`.
   */
  autoUpdateMetadata?: boolean;
  /**
   * Indicates whether the player should automatically handle audio interruptions.
   * Defaults to `false`.
   */
  autoHandleInterruptions?: boolean;
  /**
   * enables crossfade. android only.
   * Defaults to `false`.
   */
  crossfade?: boolean;
  /**
   * applies an FFT processor with the given sampling size. android only.
   * Defaults to 0 (disables it).
   */
  useFFTProcessor?: number;
}
