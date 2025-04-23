import TrackPlayer, {
  AppKilledPlaybackBehavior,
  Capability,
  RepeatMode,
} from 'react-native-track-player';

export const DefaultRepeatMode = RepeatMode.Queue;
export const DefaultAudioServiceBehaviour =
  AppKilledPlaybackBehavior.StopPlaybackAndRemoveNotification;

const setupPlayer = async (
  options: Parameters<typeof TrackPlayer.setupPlayer>[0],
  background = false
) => {
  const setup = async () => {
    try {
      await TrackPlayer.setupPlayer(options, background);
    } catch (error) {
      return (error as Error & { code?: string }).code;
    }
  };
  while ((await setup()) === 'android_cannot_setup_player_in_background') {
    // A timeout will mostly only execute when the app is in the foreground,
    // and even if we were in the background still, it will reject the promise
    // and we'll try again:
    await new Promise<void>((resolve) => setTimeout(resolve, 1));
  }
};

export const SetupService = async (background = false) => {
  await setupPlayer(
    {
      autoHandleInterruptions: true,
      useFFTProcessor: 4096,
    },
    background
  );
  await TrackPlayer.updateOptions({
    android: {
      appKilledPlaybackBehavior: DefaultAudioServiceBehaviour,
      stopForegroundGracePeriod: 999999999,
    },
    // This flag is now deprecated. Please use the above to define playback mode.
    // stoppingAppPausesPlayback: true,
    capabilities: [
      Capability.Play,
      Capability.Pause,
      Capability.SkipToNext,
      Capability.SkipToPrevious,
      Capability.SeekTo,
      Capability.JumpBackward,
    ],
    compactCapabilities: [
      Capability.Play,
      Capability.Pause,
      // Capability.SkipToNext,
    ],
    notificationCapabilities: [
      Capability.Play,
      Capability.Pause,
      Capability.SeekTo,
      Capability.JumpBackward,
    ],
    progressUpdateEventInterval: 2,
    customActions: {
      customActionsList: [
        'customAction1',
        'customAction2',
        'customAction3',
        'customAction4',
      ],
      customAction1: 1,
      customAction2: 0,
      customAction3: 2,
      customAction4: 3,
    },
  });
  await TrackPlayer.setRepeatMode(DefaultRepeatMode);
};
