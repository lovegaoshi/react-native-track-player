import React from 'react';
import { View, Text, Platform } from 'react-native';
import { useTrackPlayerEvents, Event } from 'react-native-track-player';

export const Visualizer = () => {
  const [fftArray, setFFTArray] = React.useState<number[]>([]);
  useTrackPlayerEvents(Platform.OS === 'android' ? [Event.fftUpdate] : [], async (event) => {
    setFFTArray(event.data);
  });

  return (
    <View style={{ backgroundColor: 'white' }}>
      <Text>{'FFT visualizer'}</Text>
      {fftArray.map((i) => (
        <Text>{i}</Text>
      ))}
    </View>
  );
};
