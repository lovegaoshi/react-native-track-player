import { AppRegistry } from 'react-native';
import TrackPlayer from 'react-native-track-player';

import App from './src/App';
import { PlaybackService } from './src/services';
import JSIModule from './JSIModule';
import { name as appName } from './app.json';

JSIModule;
AppRegistry.registerComponent(appName, () => App);
TrackPlayer.registerPlaybackService(() => PlaybackService);
