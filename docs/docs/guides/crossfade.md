---
sidebar_position: 97
---

# Crossfade

Crossfading is merging the current playback and the next playback at the same time, with the current fading out and the next fading in. This by definition requires 2 playback sources and players. It introduces a whole spaghetti of player control problems and is only custom implemented on Android and iOS.

## Concept

on Android this is built by 2 exoplayers rotating on crossfading. I copied the ForwardingPlayer in media3 out to accept 2 exoplayers. On crossfading, the "main" player responsible for the forwarded controls and metadata displays will rotate and emit metadata.

on iOS, the logic is similar, with the exception event emitting is done via setting delegates into the AudioPlayer class and on rotation, the back AVPlayerWrapper will release its delegate, while the main AVPlayerWrapper gains the delegate. SwitchExoPlayer also manually emits nowPlaying metadata and onTrackChanged.

## Usage

RNTP keeps both players queue the same.

To enable crossfading, setupPlayer with crossfade: true.

You should first prepare the crossfading player well ahead of crossfading via `TP.crossFadePrepare(previous=true/false)`. This primes the rotating player's current media index to the current player's, then skip to next or previous, then prepare for playback.

Then to perform crossfading, invoke TP.crossfade.

see the example app's 2nd row of controls. rotate button is `TP.crossFadePrepare`, next button is `TP.crossFade`.
