[![Android compile check](https://github.com/lovegaoshi/react-native-track-player/actions/workflows/compile-check.yml/badge.svg)](https://github.com/lovegaoshi/react-native-track-player/actions/workflows/compile-check.yml)


Please note this is a personal fork of [RNTP v4](https://github.com/doublesymmetry/react-native-track-player) for [APM](https://github.com/lovegaoshi/azusa-player-mobile)'s development. Its forked out of upstream because RNTP v5 ditched apache 2.0 licensing, while the entire react native community [1](https://github.com/evergrace-co/react-native-audio-pro),[2](https://docs.expo.dev/versions/latest/sdk/audio/) does NOT have anything better for audio playback. Many of the features APM uses, such as crossfade and Android Auto capabilities, were NOT merged upstream, unlikely to be merged in the foreseeable future, and won't be seen elsewhere either.

Functionalities used by APM [passes the play store review](https://play.google.com/store/apps/details?id=com.noxplay.noxplayer&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1).

This fork and APM proudly remains open-source and APM is published under GPLv3. as an RNTP fork this inherited a lot of tech debt and so does APM depending on it. For RNTP alternatives please look at 

https://github.com/riteshshukla04/react-native-nitro-player 

https://www.npmjs.com/package/expo-audio 

https://github.com/bbplayer-app/BBPlayer/tree/dev/packages/orpheus

https://github.com/bilisound/bilisound/tree/main/packages/player

https://github.com/Illusion137/RNTPvE
