import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Linking,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  View,
  Platform,
} from 'react-native';
import TrackPlayer, {
  useActiveTrack,
  AndroidAutoContentStyle,
} from 'react-native-track-player';

import {
  PlayerControls,
  Progress,
  Spacer,
  TrackInfo,
  Visualizer,
  EQ,
} from './components';
import { QueueInitialTracksService, SetupService } from './services';
import DemoAndroidAutoHierarchy from './services/AndroidAutoHierarchy';

export default function App() {
  return <Inner />;
}

const Inner: React.FC = () => {
  const track = useActiveTrack();
  const isPlayerReady = useSetupPlayer();

  useEffect(() => {
    function deepLinkHandler(data: { url: string }) {
      console.log('deepLinkHandler', data.url);
    }

    // This event will be fired when the app is already open and the notification is clicked
    const subscription = Linking.addEventListener('url', deepLinkHandler);

    // When you launch the closed app from the notification or any other link
    Linking.getInitialURL().then((url) => console.log('getInitialURL', url));

    return () => {
      subscription.remove();
    };
  }, []);

  if (!isPlayerReady) {
    return (
      <SafeAreaView style={styles.screenContainer}>
        <ActivityIndicator />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.screenContainer}>
      <StatusBar barStyle={'light-content'} />
      <View style={styles.contentContainer}>
        <TrackInfo track={track} />
        <Progress live={track?.isLiveStream} />
        <PlayerControls />
        {/* uncomment this for EQ and loudness enhancer */}
        {/*<EQ /> */}
        <Visualizer />
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  screenContainer: {
    flex: 1,
    backgroundColor: '#212121',
    alignItems: 'center',
    justifyContent: 'center',
    // @ts-expect-error web platform compatibility
    minHeight: Platform.OS === 'web' ? '100vh' : '100%',
  },
  contentContainer: {
    flex: 1,
    alignItems: 'center',
  },
  topBarContainer: {
    width: '100%',
    flexDirection: 'row',
    justifyContent: 'flex-end',
  },
  sheetBackgroundContainer: {
    backgroundColor: '#181818',
  },
  sheetHandle: {
    backgroundColor: 'white',
  },
});

function useSetupPlayer() {
  const [playerReady, setPlayerReady] = useState<boolean>(false);

  useEffect(() => {
    let unmounted = false;
    (async () => {
      await SetupService();
      TrackPlayer.setBrowseTree(DemoAndroidAutoHierarchy);
      TrackPlayer.setBrowseTreeStyle(
        AndroidAutoContentStyle.CategoryList,
        AndroidAutoContentStyle.Grid
      );
      if (unmounted) return;
      setPlayerReady(true);
      const queue = await TrackPlayer.getQueue();
      if (unmounted) return;
      if (queue.length <= 0) {
        await QueueInitialTracksService();
      }
    })();
    return () => {
      unmounted = true;
    };
  }, []);
  return playerReady;
}
