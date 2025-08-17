package com.doublesymmetry.trackplayer.module

import android.annotation.SuppressLint
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.RatingCompat
import androidx.media3.common.MediaItem
import androidx.media.utils.MediaConstants
import com.lovegaoshi.kotlinaudio.models.Capability
import com.lovegaoshi.kotlinaudio.models.RepeatMode
import com.doublesymmetry.trackplayer.model.State
import com.doublesymmetry.trackplayer.model.Track
import com.doublesymmetry.trackplayer.module.MusicEvents.Companion.EVENT_INTENT
import com.doublesymmetry.trackplayer.service.MusicService
import com.doublesymmetry.trackplayer.utils.AppForegroundTracker
import com.doublesymmetry.trackplayer.utils.RejectionException
import com.doublesymmetry.trackplayer.NativeTrackPlayerSpec
import com.facebook.react.bridge.*
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.doublesymmetry.trackplayer.utils.buildMediaItem
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import androidx.core.net.toUri
import com.facebook.react.module.annotations.ReactModule


/**
 * @author Milen Pivchev @mpivchev
 */
@ReactModule(name = MusicModule.NAME)
class MusicModule(reactContext: ReactApplicationContext) : NativeTrackPlayerSpec(reactContext),
    ServiceConnection {
    private lateinit var browser: MediaBrowser
    private var playerOptions: Bundle? = null
    private var isServiceBound = false
    private var playerSetUpPromise: Promise? = null
    private val scope = MainScope()
    private lateinit var musicService: MusicService
    private val context = reactContext

    override fun getName() = NAME

    companion object {
        const val NAME = "TrackPlayer"
    }

    override fun addListener(eventType: String) {
        // No implementation needed for TurboModule
        // This implements the abstract method required by NativeTrackPlayerSpec
    }

    override fun removeListeners(count: Double) {
        // No implementation needed for TurboModule
        // This implements the abstract method required by NativeTrackPlayerSpec
    }

    override fun initialize() {
        AppForegroundTracker.start()
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        launchInScope {
            // If a binder already exists, don't get a new one
            if (!::musicService.isInitialized) {
                val binder: MusicService.MusicBinder = service as MusicService.MusicBinder
                musicService = binder.service
                musicService.setupPlayer(playerOptions)
                playerSetUpPromise?.resolve(null)
            }

            isServiceBound = true
        }
    }

    /**
     * Called when a connection to the Service has been lost.
     */
    override fun onServiceDisconnected(name: ComponentName) {
        launchInScope {
            isServiceBound = false
        }
    }

    /**
     * Checks wither service is bound, or rejects. Returns whether promise was rejected.
     */
    private fun verifyServiceBoundOrReject(promise: Promise): Boolean {
        if (!isServiceBound) {
            promise.reject(
                "player_not_initialized",
                "The player is not initialized. Call setupPlayer first."
            )
            return true
        }

        return false
    }

    private fun bundleToTrack(bundle: Bundle): Track {
        return Track(context, bundle, 0)
    }

    private fun hashmapToMediaItem(hashmap: HashMap<String, String>): MediaItem {
        val mediaUri = hashmap["mediaUri"]
        val iconUri = hashmap["iconUri"]

        val extras = Bundle()
        hashmap["groupTitle"]?.let {
            extras.putString(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE, it)
        }
        hashmap["contentStyle"]?.toInt()?.let {
            extras.putInt(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_SINGLE_ITEM, it)
        }
        hashmap["childrenPlayableContentStyle"]?.toInt()?.let {
            extras.putInt(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, it)
        }
        hashmap["childrenBrowsableContentStyle"]?.toInt()?.let {
            extras.putInt(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, it)
        }

        // playbackProgress should contain a string representation of a number between 0 and 1 if present
        hashmap["playbackProgress"]?.toDouble()?.let {
            if (it > 0.98) {
                extras.putInt(
                    MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS,
                    MediaConstants.DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED)
            } else if (it == 0.0) {
                extras.putInt(
                    MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS,
                    MediaConstants.DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED)
            } else {
                extras.putInt(
                    MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS,
                    MediaConstants.DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED)
                extras.putDouble(
                    MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_PERCENTAGE, it)
            }
        }
        return buildMediaItem(
            isPlayable = hashmap["playable"]?.toInt() != 1,
            title = hashmap["title"],
            mediaId = hashmap["mediaId"] ?: "no-media-id",
            imageUri = iconUri?.toUri(),
            artist = hashmap["subtitle"],
            subtitle = hashmap["subtitle"],
            sourceUri = mediaUri?.toUri(),
            extras = extras
        )
    }

    private fun readableArrayToMediaItems(data: ArrayList<HashMap<String, String>>): MutableList<MediaItem> {
        return data.map {
            hashmapToMediaItem(it)
        }.toMutableList()
    }

    private fun rejectWithException(callback: Promise, exception: Exception) {
        when (exception) {
            is RejectionException -> {
                callback.reject(exception.code, exception)
            }
            else -> {
                callback.reject("runtime_exception", exception)
            }
        }
    }

    private fun readableArrayToTrackList(data: ReadableArray?): MutableList<Track> {
        val bundleList = Arguments.toList(data)
        if (bundleList !is ArrayList) {
            throw RejectionException("invalid_parameter", "Was not given an array of tracks")
        }
        return bundleList.map {
            if (it is Bundle) {
                bundleToTrack(it)
            } else {
                throw RejectionException(
                    "invalid_track_object",
                    "Track was not a dictionary type"
                )
            }
        }.toMutableList()
    }

    /* ****************************** API ****************************** */
    override fun getTypedExportedConstants(): MutableMap<String, Any> {
        return HashMap<String, Any>().apply {
            // Capabilities
            this["CAPABILITY_PLAY"] = Capability.PLAY.ordinal
            this["CAPABILITY_PLAY_FROM_ID"] = Capability.PLAY_FROM_ID.ordinal
            this["CAPABILITY_PLAY_FROM_SEARCH"] = Capability.PLAY_FROM_SEARCH.ordinal
            this["CAPABILITY_PAUSE"] = Capability.PAUSE.ordinal
            this["CAPABILITY_STOP"] = Capability.STOP.ordinal
            this["CAPABILITY_SEEK_TO"] = Capability.SEEK_TO.ordinal
            this["CAPABILITY_SKIP"] = OnErrorAction.SKIP.ordinal
            this["CAPABILITY_SKIP_TO_NEXT"] = Capability.SKIP_TO_NEXT.ordinal
            this["CAPABILITY_SKIP_TO_PREVIOUS"] = Capability.SKIP_TO_PREVIOUS.ordinal
            this["CAPABILITY_SET_RATING"] = Capability.SET_RATING.ordinal
            this["CAPABILITY_JUMP_FORWARD"] = Capability.JUMP_FORWARD.ordinal
            this["CAPABILITY_JUMP_BACKWARD"] = Capability.JUMP_BACKWARD.ordinal

            // States
            this["STATE_NONE"] = State.None.state
            this["STATE_READY"] = State.Ready.state
            this["STATE_PLAYING"] = State.Playing.state
            this["STATE_PAUSED"] = State.Paused.state
            this["STATE_STOPPED"] = State.Stopped.state
            this["STATE_BUFFERING"] = State.Buffering.state
            this["STATE_LOADING"] = State.Loading.state

            // Rating Types
            this["RATING_HEART"] = RatingCompat.RATING_HEART
            this["RATING_THUMBS_UP_DOWN"] = RatingCompat.RATING_THUMB_UP_DOWN
            this["RATING_3_STARS"] = RatingCompat.RATING_3_STARS
            this["RATING_4_STARS"] = RatingCompat.RATING_4_STARS
            this["RATING_5_STARS"] = RatingCompat.RATING_5_STARS
            this["RATING_PERCENTAGE"] = RatingCompat.RATING_PERCENTAGE

            // Repeat Modes
            this["REPEAT_OFF"] = Player.REPEAT_MODE_OFF
            this["REPEAT_TRACK"] = Player.REPEAT_MODE_ONE
            this["REPEAT_QUEUE"] = Player.REPEAT_MODE_ALL

            // TODO: not implemented
            this["PITCH_ALGORITHM_LINEAR"] = -1
            this["PITCH_ALGORITHM_MUSIC"] = -1
            this["PITCH_ALGORITHM_VOICE"] = -1
            this["CAPABILITY_LIKE"] = -1
            this["CAPABILITY_DISLIKE"] = -1
            this["CAPABILITY_BOOKMARK"] = -1

        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun setupPlayer(options: ReadableMap?, background: Boolean, promise: Promise) {
        if (isServiceBound) {
            promise.reject(
                "player_already_initialized",
                "The player has already been initialized via setupPlayer."
            )
            return
        }

        // prevent crash Fatal Exception: android.app.RemoteServiceException$ForegroundServiceDidNotStartInTimeException
        if (!background
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && AppForegroundTracker.backgrounded) {
            promise.reject(
                "android_cannot_setup_player_in_background",
                "On Android the app must be in the foreground when setting up the player."
            )
            return
        }


        val bundledData = Arguments.toBundle(options)

        playerSetUpPromise = promise
        playerOptions = bundledData

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                MusicEvents(context),
                IntentFilter(EVENT_INTENT), Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                MusicEvents(context),
                IntentFilter(EVENT_INTENT)
            )
        }

        val musicModule = this
        try {
            Intent(context, MusicService::class.java).also { intent ->
                context.bindService(intent, musicModule, Context.BIND_AUTO_CREATE)
                val sessionToken =
                    SessionToken(context, ComponentName(context, MusicService::class.java))
                val browserFuture = MediaBrowser.Builder(context, sessionToken).buildAsync()
                // browser = browserFuture.get()
            }
        } catch (exception: Exception) {
            Timber.tag("RNTP").w(exception, "Could not initialize service")
            throw exception
        }
    }

    override fun updateOptions(data: ReadableMap?, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        val options = Arguments.toBundle(data)

        options?.let {
            musicService.updateOptions(it)
        }

        callback.resolve(null)
    }

    override fun setEqualizerPreset(preset: Double, callback: Promise) = launchInScope {
        musicService.setEqualizerPreset(preset.toInt())
        callback.resolve(null)
    }

    override fun getCurrentEqualizerPreset(callback: Promise) = launchInScope {
        callback.resolve(musicService.getCurrentEqualizerPreset())
    }

    override fun getEqualizerPresets(callback: Promise) = launchInScope {
        callback.resolve(Arguments.fromList(musicService.getEqualizerPresets()))
    }


    override fun setLoudnessEnhance(gain: Double, callback: Promise) = launchInScope {
        musicService.setLoudnessEnhance(gain.toInt())
        callback.resolve(null)
    }


    override fun add(data: ReadableArray?, insertBeforeIndex: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        val insertB4Index = insertBeforeIndex.toInt()

        try {
            val tracks = readableArrayToTrackList(data)
            if (insertB4Index < -1 || insertB4Index > musicService.tracks.size) {
                callback.reject("index_out_of_bounds", "The track index is out of bounds")
                return@launchInScope
            }
            val index = if (insertB4Index == -1) musicService.tracks.size else insertB4Index
            musicService.add(
                tracks,
                index
            )
            callback.resolve(index)
        } catch (exception: Exception) {
            rejectWithException(callback, exception)
        }
    }

    override fun load(data: ReadableMap?, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        if (data == null) {
            callback.resolve(null)
            return@launchInScope
        }
        val bundle = Arguments.toBundle(data)
        if (bundle is Bundle) {
            musicService.load(bundleToTrack(bundle))
            callback.resolve(null)
        } else {
            callback.reject("invalid_track_object", "Track was not a dictionary type")
        }
    }

    override fun move(fromIndex: Double, toIndex: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        musicService.move(fromIndex.toInt(), toIndex.toInt())
        callback.resolve(null)
    }

    override fun remove(data: ReadableArray?, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        val inputIndexes = Arguments.toList(data)
        if (inputIndexes != null) {
            val size = musicService.tracks.size
            val indexes: ArrayList<Int> = ArrayList()
            for (inputIndex in inputIndexes) {
                val index = inputIndex as? Int ?: inputIndex.toString().toInt()
                if (index < 0 || index >= size) {
                    callback.reject(
                        "index_out_of_bounds",
                        "One or more indexes was out of bounds"
                    )
                    return@launchInScope
                }
                indexes.add(index)
            }
            musicService.remove(indexes)
        }
        callback.resolve(null)
    }

    override fun updateMetadataForTrack(index: Double, map: ReadableMap?, callback: Promise) =
        launchInScope {
            if (verifyServiceBoundOrReject(callback)) return@launchInScope

            val indexInt = index.toInt()
            if (indexInt < 0 || indexInt >= musicService.tracks.size) {
                callback.reject("index_out_of_bounds", "The index is out of bounds")
            } else {
                val context: ReactContext = context
                val track = musicService.tracks[indexInt]
                track.setMetadata(context, Arguments.toBundle(map), 0)
                musicService.updateMetadataForTrack(indexInt, track)

                callback.resolve(null)
            }
        }

    override fun updateNowPlayingMetadata(map: ReadableMap?, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        if (musicService.tracks.isEmpty())
            callback.reject("no_current_item", "There is no current item in the player")

        Arguments.toBundle(map)?.let {
            val track = bundleToTrack(it)
            musicService.updateNowPlayingMetadata(track)
        }

        callback.resolve(null)
    }

    override fun removeUpcomingTracks(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.removeUpcomingTracks()
        callback.resolve(null)
    }

    override fun skip(index: Double, initialPosition: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        musicService.skip(index.toInt())
        if (initialPosition >= 0) {
            musicService.seekTo(initialPosition.toFloat())
        }

        callback.resolve(null)
    }

    override fun skipToNext(initialTime: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.skipToNext()

        if (initialTime >= 0) {
            musicService.seekTo(initialTime.toFloat())
        }

        callback.resolve(null)
    }

    override fun skipToPrevious(initialTime: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.skipToPrevious()

        if (initialTime >= 0) {
            musicService.seekTo(initialTime.toFloat())
        }

        callback.resolve(null)
    }

    override fun reset(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.stop()
        delay(300) // Allow playback to stop
        musicService.clear()

        callback.resolve(null)
    }

    override fun play(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.play()
        callback.resolve(null)
    }

    override fun pause(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.pause()
        callback.resolve(null)
    }

    override fun stop(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.stop()
        callback.resolve(null)
    }

    override fun seekTo(seconds: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.seekTo(seconds.toFloat())
        callback.resolve(null)
    }

    override fun seekBy(offset: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.seekBy(offset.toFloat())
        callback.resolve(null)
    }

    override fun retry(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.retry()
        callback.resolve(null)
    }

    override fun setVolume(volume: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.setVolume(volume.toFloat())
        callback.resolve(null)
    }

    override fun getVolume(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        callback.resolve(musicService.getVolume())
    }

    override fun setRate(rate: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.setRate(rate.toFloat())
        callback.resolve(null)
    }

    override fun getPitch(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        callback.resolve(musicService.getPitch())
    }

    override fun setPitch(rate: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.setPitch(rate.toFloat())
        callback.resolve(null)
    }

    override fun getRate(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        callback.resolve(musicService.getRate())
    }

    override fun setRepeatMode(mode: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.setRepeatMode(RepeatMode.fromOrdinal(mode.toInt()))
        callback.resolve(null)
    }

    override fun getRepeatMode(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        callback.resolve(musicService.getRepeatMode().ordinal)
    }

    override fun setPlayWhenReady(playWhenReady: Boolean, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        musicService.playWhenReady = playWhenReady
        callback.resolve(null)
    }

    override fun getPlayWhenReady(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        callback.resolve(musicService.playWhenReady)
    }

    override fun getTrack(index: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        if( index < 0 || index >= musicService.tracks.size) {
            callback.resolve(null)
            return@launchInScope
        }

        val originalItem = musicService.tracks[index.toInt()].originalItem
        callback.resolve(if(originalItem == null) null else Arguments.fromBundle(originalItem))
    }

    override fun getQueue(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        callback.resolve(Arguments.fromList(musicService.tracks.map { it.originalItem }))
    }

    override fun setQueue(data: ReadableArray?, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope

        try {
            musicService.clear()
            musicService.add(readableArrayToTrackList(data))
            callback.resolve(null)
        } catch (exception: Exception) {
            rejectWithException(callback, exception)
        }
    }

    override fun getActiveTrackIndex(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        callback.resolve(
            if (musicService.tracks.isEmpty()) null else musicService.getCurrentTrackIndex()
        )
    }

    override fun getActiveTrack(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        if(musicService.tracks.isEmpty()) {
            callback.resolve(null)
            return@launchInScope
        }

        val activeTrack = musicService.tracks[musicService.getCurrentTrackIndex()].originalItem
        callback.resolve(if(activeTrack == null) null else Arguments.fromBundle(activeTrack))
    }


    override fun getProgress(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        val bundle = Bundle()
        bundle.putDouble("duration", musicService.getDurationInSeconds())
        bundle.putDouble("position", musicService.getPositionInSeconds())
        bundle.putDouble("buffered", musicService.getBufferedPositionInSeconds())
        callback.resolve(Arguments.fromBundle(bundle))
    }

    override fun getPlaybackState(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        callback.resolve(Arguments.fromBundle(musicService.getPlayerStateBundle(musicService.state)))
    }

    override fun setAnimatedVolume(volume: Double, duration: Double, interval: Double, msg: String, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        musicService.setAnimatedVolume(volume.toFloat(), duration.toLong(), interval.toLong(), msg).await()
        delay(duration.toLong())
        callback.resolve(null)
    }

    override fun fadeOutPause(duration: Double, interval: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        musicService.fadeOutPause(duration.toLong(), interval.toLong())
        delay(duration.toLong())
        callback.resolve(null)
    }

    override fun fadeOutNext(duration: Double, interval: Double, toVolume: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        musicService.fadeOutNext(duration.toLong(), interval.toLong(), toVolume.toFloat())
        delay(duration.toLong())
        callback.resolve(null)
    }

    override fun fadeOutPrevious(duration: Double, interval: Double, toVolume: Double, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        musicService.fadeOutPrevious(duration.toLong(), interval.toLong(), toVolume.toFloat())
        delay(duration.toLong())
        callback.resolve(null)
    }

    override fun fadeOutJump(
        index: Double,
        duration: Double,
        interval: Double,
        toVolume: Double,
        callback: Promise
    ) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        musicService.fadeOutJump(index.toInt(), duration.toLong(), interval.toLong(), toVolume.toFloat())
        delay(duration.toLong())
        callback.resolve(null)
    }
    
    override fun setBrowseTree(mediaItems: ReadableMap, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        val mediaItemsMap = mediaItems.toHashMap()
        musicService.mediaTree = mediaItemsMap.mapValues { readableArrayToMediaItems(it.value as ArrayList<HashMap<String, String>>) }
        Timber.tag("APM").d("refreshing browseTree")
        musicService.notifyChildrenChanged()
        callback.resolve(musicService.mediaTree.toString())
    }

    // this method doesn't seem to affect style after onGetRoot is called, and won't change if notifyChildrenChanged is emitted.
    override fun setBrowseTreeStyle(
        browsableStyle: Double,
        playableStyle: Double,
        callback: Promise
    ) = launchInScope {
        fun getStyle(check: Int): Int {
            return when (check) {
                1 -> MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                2 -> MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM
                3 -> MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM
                else -> MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
            }
        }
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        musicService.mediaTreeStyle = listOf(
            getStyle(browsableStyle.toInt()),
            getStyle(playableStyle.toInt())
        )
        callback.resolve(null)
    }

    override fun setPlaybackState(mediaID: String, callback: Promise) = launchInScope {
        // TODO: not implemented!
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        callback.resolve(null)
    }

    override fun acquireWakeLock(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        musicService.acquireWakeLock()
        callback.resolve(null)
    }

    override fun abandonWakeLock(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        musicService.abandonWakeLock()
        callback.resolve(null)
    }

    override fun crossFadePrepare(previous: Boolean, seektoDouble: Double?, callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        musicService.crossFadePrepare(previous, seektoDouble ?: 0.0)
        callback.resolve(null)
    }

    override fun switchExoPlayer(
        fadeDuration: Double,
        fadeInterval: Double,
        fadeToVolume: Double,
        callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        musicService.switchExoPlayer(
            fadeDuration = fadeDuration.toLong(),
            fadeInterval = fadeInterval.toLong(),
            fadeToVolume = fadeToVolume.toFloat()
        )
        callback.resolve(null)
    }

    override fun getLastConnectedPackage(callback: Promise) = launchInScope {
        if (verifyServiceBoundOrReject(callback)) return@launchInScope
        callback.resolve(musicService.lastConnectedPackage)
    }

    // Bridgeless interop layer tries to pass the `Job` from `scope.launch` to the JS side
    // which causes an exception. We can work around this using a wrapper.
    private fun launchInScope(block: suspend () -> Unit) {
        scope.launch {
            block()
        }
    }
}
