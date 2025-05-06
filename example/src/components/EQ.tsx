import React, { useEffect } from 'react';
import { View, TextInput, Button, ScrollView } from 'react-native';
import TrackPlayer from 'react-native-track-player';

export const EQ = () => {
  const [eq, setEq] = React.useState<string[]>([]);

  useEffect(() => {
    const init = async () => {
      setEq(await TrackPlayer.getEqualizerPresets());
    };
    init();
  }, []);

  return (
    <View style={{ backgroundColor: 'white' }}>
      <View>
        <TextInput
          placeholder={'volumeGain'}
          onSubmitEditing={(e) =>
            TrackPlayer.setLoudnessEnhance(Number(e.nativeEvent.text) || 0)
          }
        />
      </View>
      <ScrollView horizontal>
        {eq.map((v, i) => (
          <Button
            key={v}
            title={v}
            onPress={() => TrackPlayer.setEqualizerPreset(i)}
          ></Button>
        ))}
      </ScrollView>
    </View>
  );
};
