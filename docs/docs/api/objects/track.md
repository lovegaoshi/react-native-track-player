---
sidebar_position: 1
---

# Track

Tracks in the player queue are plain javascript objects as described below.

Only the `url`, `title` and `artist` properties are required for basic playback

| Param          | Type                        | Description  |
| -------------- | --------------------------- | ------------ |
| id             | `string`                    | The track id                |
| url            | `string` or [Resource Object](../objects/resource.md) | The media URL |
| type           | `string`                    | Stream type. One of `dash`, `hls`, `smoothstreaming`, `sabr` or `default` |
| userAgent      | `string`                    | The user agent HTTP header  |
| contentType    | `string`                    | Mime type of the media file |
| duration       | `number`                    | The duration in seconds     |
| title          | `string`                    | The track title             |
| artist         | `string`                    | The track artist            |
| album          | `string`                    | The track album             |
| description    | `string`                    | The track description       |
| genre          | `string`                    | The track genre             |
| date           | `string`                    | The track release date in [RFC 3339](https://www.ietf.org/rfc/rfc3339.txt) |
| rating         | Depends on the [rating type](../constants/rating.md)  | The track rating value |
| artwork        | `string` or [Resource Object](../objects/resource.md) | The artwork url |
| pitchAlgorithm | [Pitch Algorithm](../constants/pitch-algorithm.md) | The pitch algorithm |
| headers        | `object`                    | An object containing all the headers to use in the HTTP request |
| isLiveStream   | `boolean`                   | Used by iOS to present live stream option in control center |
| sabr           | `SabrTrackOptions`          | (Android only) Required SABR request data when `type` is `sabr` |

### SABR (Android only)

`type: 'sabr'` plays YouTube's SABR endpoint through Media3. The track `url` must be the full signed
`serverAbrStreamingUrl`. A URL plus a poToken is not enough: the request body also needs the
Base64 URL-safe `videoPlaybackUstreamerConfig`, the video id, a client identity that matches the
poToken, and the adaptive formats available for the video.

| `sabr` field | Type | Description |
| ------------ | ---- | ----------- |
| poToken | `string` | Base64 URL-safe poToken. The signed URL should already contain the matching `pot` parameter; this value is decoded into the protobuf request body. |
| videoPlaybackUstreamerConfig | `string` | Base64 URL-safe config from the player response. |
| videoId | `string` | YouTube video id. |
| client | `object` | `name`, `version`, `osName` and `osVersion`. `name` is a `StreamerContext.ClientName` such as `ANDROID`, `WEB`, `MWEB`, `TVHTML5`, `TVLITE` or `TVANDROID`. |
| formats | `array` | Adaptive formats derived from the player response. At least one audio or video format is required. |
| subtitles | `array` | Optional caption formats. |
| visitorCookie | `string` | Optional cookie forwarded with media requests. |
| durationMs | `number` | Duration in milliseconds. Falls back to the track's `duration` property in seconds. |

Each entry in `formats` (and `subtitles`) accepts `itag`, `mimeType`, `codecs`, `bitrate`, `width`,
`height`, `fps`, `language`, `qualityLabel`, `drc`, `lmt`, `xtags` and `audioTrackId`.

```ts
import TrackPlayer, { TrackType } from 'react-native-track-player';

await TrackPlayer.add({
  id: 'video-id',
  url: serverAbrStreamingUrl,
  type: TrackType.Sabr,
  title: 'Example',
  artist: 'Example artist',
  duration: 212,
  sabr: {
    poToken,
    videoPlaybackUstreamerConfig,
    videoId: 'video-id',
    client: {
      name: 'ANDROID',
      version: '20.10.38',
      osName: 'Android',
      osVersion: '14',
    },
    formats: [
      {
        itag: 141,
        mimeType: 'audio/mp4',
        codecs: 'mp4a.40.2',
        bitrate: 128000,
        language: 'en',
      },
      {
        itag: 18,
        mimeType: 'video/mp4',
        codecs: 'avc1.42001E, mp4a.40.2',
        width: 640,
        height: 360,
        fps: 30,
        bitrate: 800000,
      },
    ],
  },
});
```

SABR control signals such as redirect, backoff and player-response refresh are handled by the
ported SABR stream processor, but the application is responsible for supplying a fresh signed URL
and streaming config when those expire.
