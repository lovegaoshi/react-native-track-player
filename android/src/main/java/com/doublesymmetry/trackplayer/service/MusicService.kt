package com.doublesymmetry.trackplayer.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media.utils.MediaConstants
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.LibraryResult
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.lovegaoshi.kotlinaudio.models.*
import com.lovegaoshi.kotlinaudio.player.QueuedAudioPlayer
import com.doublesymmetry.trackplayer.HeadlessJsMediaService
import com.doublesymmetry.trackplayer.extensions.NumberExt.Companion.toMilliseconds
import com.doublesymmetry.trackplayer.extensions.NumberExt.Companion.toSeconds
import com.doublesymmetry.trackplayer.extensions.asLibState
import com.doublesymmetry.trackplayer.extensions.find
import com.doublesymmetry.trackplayer.model.MetadataAdapter
import com.doublesymmetry.trackplayer.model.PlaybackMetadata
import com.doublesymmetry.trackplayer.model.Track
import com.doublesymmetry.trackplayer.model.TrackAudioItem
import com.doublesymmetry.trackplayer.module.MusicEvents
import com.doublesymmetry.trackplayer.module.MusicEvents.Companion.METADATA_PAYLOAD_KEY
import com.doublesymmetry.trackplayer.R as TrackPlayerR
import com.doublesymmetry.trackplayer.utils.BundleUtils
import com.doublesymmetry.trackplayer.utils.BundleUtils.setRating
import com.doublesymmetry.trackplayer.utils.CoilBitmapLoader
import com.doublesymmetry.trackplayer.utils.buildMediaItem
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

@OptIn(UnstableApi::class)
@MainThread
class MusicService : HeadlessJsMediaService() {
    private lateinit var player: QueuedAudioPlayer
    private val binder = MusicBinder()
    private val scope = MainScope()
    private lateinit var fakePlayer: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    private var progressUpdateJob: Job? = null
    var mediaTree: Map<String, List<MediaItem>> = HashMap()
    var mediaTreeStyle: List<Int> = listOf(
        MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
        MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM)
    private var sessionCommands: SessionCommands? = null
    private var playerCommands: Player.Commands? = null
    private var customLayout: List<CommandButton> = listOf()
    var currentBitmap: MutableList<Bitmap?> = mutableListOf(null)
    private var lastWake: Long = 0

    @ExperimentalCoroutinesApi
    override fun onCreate() {
        Log.d("APM", "RNTP musicservice created.")
        fakePlayer = ExoPlayer.Builder(this).build()
        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Add the Uri data so apps can identify that it was a notification click
            data = Uri.parse("trackplayer://notification.click")
            action = Intent.ACTION_VIEW
        }
        mediaSession = MediaLibrarySession.Builder(this, fakePlayer, APMMediaSessionCallback() )
            .setBitmapLoader(CacheBitmapLoader(CoilBitmapLoader(this, cacheBitmap = currentBitmap)))
            .setId("APM-MediaSession")
            // https://github.com/androidx/media/issues/1218
            .setSessionActivity(PendingIntent.getActivity(this, 0, openAppIntent, getPendingIntentFlags()))
            .build()
        super.onCreate()
    }

    /**
     * Use [appKilledPlaybackBehavior] instead.
     */
    @Deprecated("This will be removed soon")
    var stoppingAppPausesPlayback = true
        private set

    enum class AppKilledPlaybackBehavior(val string: String) {
        CONTINUE_PLAYBACK("continue-playback"),
        PAUSE_PLAYBACK("pause-playback"),
        STOP_PLAYBACK_AND_REMOVE_NOTIFICATION("stop-playback-and-remove-notification")
    }

    private var appKilledPlaybackBehavior = AppKilledPlaybackBehavior.STOP_PLAYBACK_AND_REMOVE_NOTIFICATION
    private var stopForegroundGracePeriod: Int = DEFAULT_STOP_FOREGROUND_GRACE_PERIOD

    val tracks: List<Track>
        get() = player.items.map { (it as TrackAudioItem).track }

    val currentTrack
        get() = (player.currentItem as TrackAudioItem).track

    val state
        get() = player.playerState


    val playbackError
        get() = player.playbackError

    val event
        get() = player.playerEventHolder

    var playWhenReady: Boolean
        get() = player.playWhenReady
        set(value) {
            player.playWhenReady = value
        }

    private var latestOptions: Bundle? = null
    private var compactCapabilities: List<Capability> = emptyList()
    private var commandStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("APM", "onStartCommand: ${intent?.action}, ${intent?.`package`}")
        // HACK: Why is onPlay triggering onStartCommand??
        if (!commandStarted) {
            commandStarted = true
            super.onStartCommand(intent, flags, startId)
            // HACK: this is due to MusicModule starts the service but not immediately starting the playback;
            // thus the exception is thrown. the workaround is to not "start service" in MusicModule.
            // however I found initiating playback via notification wont start this way
            // ie. onStartCommand will not start. need to investigate further on why
            startAndStopEmptyNotificationToAvoidANR()
        }
        return START_STICKY
    }

    /**
     * Workaround for the "Context.startForegroundService() did not then call Service.startForeground()"
     * within 5s" ANR and crash by creating an empty notification and stopping it right after. For more
     * information see https://github.com/doublesymmetry/react-native-track-player/issues/1666
     */
    private fun startAndStopEmptyNotificationToAvoidANR() {
        Log.d("APM", "startAndStopEmptyNotificationToAvoidANR")
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(NotificationChannel(
                getString(TrackPlayerR.string.rntp_temporary_channel_id),
                getString(TrackPlayerR.string.rntp_temporary_channel_name),
                NotificationManager.IMPORTANCE_LOW)
            )
        }

        val notificationBuilder = NotificationCompat.Builder(this, getString(TrackPlayerR.string.rntp_temporary_channel_id))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
        }
        val notification = notificationBuilder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(EMPTY_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(EMPTY_NOTIFICATION_ID, notification)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }


    @MainThread
    fun setupPlayer(playerOptions: Bundle?) {
        if (this::player.isInitialized) {
            print("Player was initialized. Prevent re-initializing again")
            return
        }

        Log.d("APM", "RNTP musicservice set up.")
        val mPlayerOptions = PlayerOptions(
            cacheSize = playerOptions?.getDouble(MAX_CACHE_SIZE_KEY)?.toLong() ?: 0,
            audioContentType = when(playerOptions?.getString(ANDROID_AUDIO_CONTENT_TYPE)) {
                "music" -> 0
                "speech" -> 1
                "sonification" -> 2
                "movie" -> 3
                "unknown" -> 4
                else -> 0
            },
            handleAudioFocus = playerOptions?.getBoolean(AUTO_HANDLE_INTERRUPTIONS) ?: true,
            handleAudioBecomingNoisy = true,
            bufferOptions = BufferOptions(
                playerOptions?.getDouble(MIN_BUFFER_KEY)?.toMilliseconds()?.toInt(),
                playerOptions?.getDouble(MAX_BUFFER_KEY)?.toMilliseconds()?.toInt(),
                playerOptions?.getDouble(PLAY_BUFFER_KEY)?.toMilliseconds()?.toInt(),
                playerOptions?.getDouble(BACK_BUFFER_KEY)?.toMilliseconds()?.toInt(),
            )
        )
        player = QueuedAudioPlayer(this@MusicService, mPlayerOptions)
        fakePlayer.release()
        mediaSession.player = player.player
        observeEvents()
    }

    @MainThread
    fun updateOptions(options: Bundle) {
        latestOptions = options
        val androidOptions = options.getBundle(ANDROID_OPTIONS_KEY)

        if (androidOptions?.containsKey(AUDIO_OFFLOAD_KEY) == true) {
            player.setAudioOffload(androidOptions.getBoolean(AUDIO_OFFLOAD_KEY))
        }

        appKilledPlaybackBehavior =
            AppKilledPlaybackBehavior::string.find(androidOptions?.getString(APP_KILLED_PLAYBACK_BEHAVIOR_KEY)) ?:
                    AppKilledPlaybackBehavior.CONTINUE_PLAYBACK

        BundleUtils.getIntOrNull(androidOptions, STOP_FOREGROUND_GRACE_PERIOD_KEY)?.let { stopForegroundGracePeriod = it }

        // TODO: This handles a deprecated flag. Should be removed soon.
        options.getBoolean(STOPPING_APP_PAUSES_PLAYBACK_KEY).let {
            stoppingAppPausesPlayback = options.getBoolean(STOPPING_APP_PAUSES_PLAYBACK_KEY)
            if (stoppingAppPausesPlayback) {
                appKilledPlaybackBehavior = AppKilledPlaybackBehavior.PAUSE_PLAYBACK
            }
        }

        player.alwaysPauseOnInterruption = androidOptions?.getBoolean(PAUSE_ON_INTERRUPTION_KEY) ?: false

        // setup progress update events if configured
        progressUpdateJob?.cancel()
        val updateInterval = BundleUtils.getDoubleOrNull(options, PROGRESS_UPDATE_EVENT_INTERVAL_KEY)
        if (updateInterval != null && updateInterval > 0) {
            progressUpdateJob = scope.launch {
                progressUpdateEventFlow(updateInterval).collect { emit(MusicEvents.PLAYBACK_PROGRESS_UPDATED, it) }
            }
        }

        val capabilities = options.getIntegerArrayList("capabilities")?.map { Capability.entries[it] } ?: emptyList()
        var notificationCapabilities = options.getIntegerArrayList("notificationCapabilities")?.map { Capability.entries[it] } ?: emptyList()
        compactCapabilities = options.getIntegerArrayList("compactCapabilities")?.map { Capability.entries[it] } ?: emptyList()
        val customActions = options.getBundle(CUSTOM_ACTIONS_KEY)
        val customActionsList = customActions?.getStringArrayList(CUSTOM_ACTIONS_LIST_KEY)
        if (notificationCapabilities.isEmpty()) notificationCapabilities = capabilities

        val playerCommandsBuilder = Player.Commands.Builder().addAll(
            // HACK: without COMMAND_GET_CURRENT_MEDIA_ITEM, notification cannot be created
            Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            Player.COMMAND_GET_TRACKS,
            Player.COMMAND_GET_TIMELINE,
            Player.COMMAND_GET_METADATA,
            Player.COMMAND_GET_AUDIO_ATTRIBUTES,
            Player.COMMAND_GET_VOLUME,
            Player.COMMAND_GET_DEVICE_VOLUME,
            Player.COMMAND_GET_TEXT,
            Player.COMMAND_SEEK_TO_MEDIA_ITEM,
            Player.COMMAND_SET_MEDIA_ITEM,
            Player.COMMAND_PREPARE,
            Player.COMMAND_RELEASE,
        )
        notificationCapabilities.forEach {
            when (it) {
                Capability.PLAY, Capability.PAUSE -> {
                    playerCommandsBuilder.add(Player.COMMAND_PLAY_PAUSE)
                }
                Capability.STOP -> {
                    playerCommandsBuilder.add(Player.COMMAND_STOP)
                }
                Capability.SKIP_TO_NEXT -> {
                    playerCommandsBuilder.add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    playerCommandsBuilder.add(Player.COMMAND_SEEK_TO_NEXT)
                }
                Capability.SKIP_TO_PREVIOUS -> {
                    playerCommandsBuilder.add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    playerCommandsBuilder.add(Player.COMMAND_SEEK_TO_PREVIOUS)
                }
                Capability.JUMP_FORWARD -> {
                    playerCommandsBuilder.add(Player.COMMAND_SEEK_FORWARD)
                }
                Capability.JUMP_BACKWARD -> {
                    playerCommandsBuilder.add(Player.COMMAND_SEEK_BACK)
                }
                Capability.SEEK_TO -> {
                    playerCommandsBuilder.add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                }
                else -> { }
            }
        }
        customLayout = customActionsList?.map {
                v -> CustomButton(
            displayName = v,
            sessionCommand = v,
            iconRes = BundleUtils.getCustomIcon(
                this,
                customActions,
                v,
                TrackPlayerR.drawable.ifl_24px
            )
        ).commandButton
        } ?: ImmutableList.of()

        val sessionCommandsBuilder = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
        customLayout.forEach {
                v ->
            v.sessionCommand?.let { sessionCommandsBuilder.add(it) }
        }

        sessionCommands = sessionCommandsBuilder.build()
        playerCommands = playerCommandsBuilder.build()

        if (mediaSession.mediaNotificationControllerInfo != null) {
            // https://github.com/androidx/media/blob/c35a9d62baec57118ea898e271ac66819399649b/demos/session_service/src/main/java/androidx/media3/demo/session/DemoMediaLibrarySessionCallback.kt#L107
            mediaSession.setCustomLayout(
                mediaSession.mediaNotificationControllerInfo!!,
                customLayout
            )
            mediaSession.setAvailableCommands(
                mediaSession.mediaNotificationControllerInfo!!,
                sessionCommands!!,
                playerCommands!!
            )
        }
    }

    @MainThread
    private fun progressUpdateEventFlow(interval: Double) = flow {
        while (true) {
            if (player.isPlaying) {
                val bundle = progressUpdateEvent()
                emit(bundle)
            }

            delay((interval * 1000).toLong())
        }
    }

    @MainThread
    private suspend fun progressUpdateEvent(): Bundle {
        return withContext(Dispatchers.Main) {
            Bundle().apply {
                putDouble(POSITION_KEY, player.position.toSeconds())
                putDouble(DURATION_KEY, player.duration.toSeconds())
                putDouble(BUFFERED_POSITION_KEY, player.bufferedPosition.toSeconds())
                putInt(TRACK_KEY, player.currentIndex)
            }
        }
    }

    @MainThread
    fun add(track: Track) {
        add(listOf(track))
    }

    @MainThread
    fun add(tracks: List<Track>) {
        val items = tracks.map { it.toAudioItem() }
        player.add(items)
    }

    @MainThread
    fun add(tracks: List<Track>, atIndex: Int) {
        val items = tracks.map { it.toAudioItem() }
        player.add(items, atIndex)
    }

    @MainThread
    fun load(track: Track) {
        player.load(track.toAudioItem())
    }

    @MainThread
    fun move(fromIndex: Int, toIndex: Int) {
        player.move(fromIndex, toIndex);
    }

    @MainThread
    fun remove(index: Int) {
        remove(listOf(index))
    }

    @MainThread
    fun remove(indexes: List<Int>) {
        player.remove(indexes)
    }

    @MainThread
    fun clear() {
        player.clear()
    }

    @MainThread
    fun play() {
        player.play()
    }

    @MainThread
    fun pause() {
        player.pause()
    }

    @MainThread
    fun stop() {
        player.stop()
    }

    @MainThread
    fun removeUpcomingTracks() {
        player.removeUpcomingItems()
    }

    @MainThread
    fun removePreviousTracks() {
        player.removePreviousItems()
    }

    @MainThread
    fun skip(index: Int) {
        player.jumpToItem(index)
    }

    @MainThread
    fun skipToNext() {
        player.next()
    }

    @MainThread
    fun skipToPrevious() {
        player.previous()
    }

    @MainThread
    fun seekTo(seconds: Float) {
        player.seek((seconds * 1000).toLong(), TimeUnit.MILLISECONDS)
    }

    @MainThread
    fun seekBy(offset: Float) {
        player.seekBy((offset.toLong()), TimeUnit.SECONDS)
    }

    @MainThread
    fun retry() {
        player.prepare()
    }

    @MainThread
    fun getCurrentTrackIndex(): Int = player.currentIndex

    @MainThread
    fun getRate(): Float = player.playbackSpeed

    @MainThread
    fun setRate(value: Float) {
        player.playbackSpeed = value
    }

    @MainThread
    fun getRepeatMode(): RepeatMode = player.repeatMode

    @MainThread
    fun setRepeatMode(value: RepeatMode) {
        player.repeatMode = value
    }

    @MainThread
    fun getVolume(): Float = player.volume

    @MainThread
    fun setVolume(value: Float) {
        player.volume = value
    }

    @MainThread
    fun setAnimatedVolume(value: Float, duration: Long = 500L, interval: Long = 20L, emitEventMsg: String = ""): Deferred<Unit> {
        val eventMsgBundle = Bundle()
        eventMsgBundle.putString(DATA_KEY, emitEventMsg)
        return player.fadeVolume(value, duration, interval) {
            emit(
                MusicEvents.PLAYBACK_ANIMATED_VOLUME_CHANGED,
                eventMsgBundle
            )
        }
    }

    fun fadeOutPause (duration: Long = 500L, interval: Long = 20L) {
        player.fadeVolume(0f, duration, interval) {
            player.pause()
        }
    }

    fun fadeOutNext (duration: Long = 500L, interval: Long = 20L, toVolume: Float = 1f) {
        player.fadeVolume(0f, duration, interval) {
            player.next()
            player.fadeVolume(toVolume, duration, interval)
        }
    }

    fun fadeOutPrevious (duration: Long = 500L, interval: Long = 20L, toVolume: Float = 1f) {
        player.fadeVolume(0f, duration, interval) {
            player.previous()
            player.fadeVolume(toVolume, duration, interval)
        }
    }

    fun fadeOutJump (index: Int, duration: Long = 500L, interval: Long = 20L, toVolume: Float = 1f) {
        player.fadeVolume(0f, duration, interval) {
            player.jumpToItem(index)
            player.fadeVolume(toVolume, duration, interval)
        }
    }
    @MainThread
    fun getDurationInSeconds(): Double = player.duration.toSeconds()

    @MainThread
    fun getPositionInSeconds(): Double = player.position.toSeconds()

    @MainThread
    fun getBufferedPositionInSeconds(): Double = player.bufferedPosition.toSeconds()

    @MainThread
    fun getPlayerStateBundle(state: AudioPlayerState): Bundle {
        val bundle = Bundle()
        bundle.putString(STATE_KEY, state.asLibState.state)
        if (state == AudioPlayerState.ERROR) {
            bundle.putBundle(ERROR_KEY, getPlaybackErrorBundle())
        }
        return bundle
    }

    @MainThread
    fun updateMetadataForTrack(index: Int, track: Track) {
        player.replaceItem(index, track.toAudioItem())
    }

    @MainThread
    fun updateNowPlayingMetadata(track: Track) {
        updateMetadataForTrack(player.currentIndex, track)
    }

    @MainThread
    fun clearNotificationMetadata() {
    }

    private fun emitPlaybackTrackChangedEvents(
        index: Int?,
        previousIndex: Int?,
        oldPosition: Double
    ) {
        val a = Bundle()
        a.putDouble(POSITION_KEY, oldPosition)
        if (index != null) {
            a.putInt(NEXT_TRACK_KEY, index)
        }

        if (previousIndex != null) {
            a.putInt(TRACK_KEY, previousIndex)
        }

        emit(MusicEvents.PLAYBACK_TRACK_CHANGED, a)

        val b = Bundle()
        b.putDouble("lastPosition", oldPosition)
        if (tracks.isNotEmpty()) {
            b.putInt("index", player.currentIndex)
            b.putBundle("track", tracks[player.currentIndex].originalItem)
            if (previousIndex != null) {
                b.putInt("lastIndex", previousIndex)
                b.putBundle("lastTrack", tracks[previousIndex].originalItem)
            }
        }
        emit(MusicEvents.PLAYBACK_ACTIVE_TRACK_CHANGED, b)
    }

    private fun emitQueueEndedEvent() {
        val bundle = Bundle()
        bundle.putInt(TRACK_KEY, player.currentIndex)
        bundle.putDouble(POSITION_KEY, player.position.toSeconds())
        emit(MusicEvents.PLAYBACK_QUEUE_ENDED, bundle)
    }

    @Suppress("DEPRECATION")
    fun isForegroundService(): Boolean {
        val manager = baseContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (MusicService::class.java.name == service.service.className) {
                return service.foreground
            }
        }
        Timber.e("isForegroundService found no matching service")
        return false
    }

    @MainThread
    private fun observeEvents() {
        scope.launch {
            event.stateChange.collect {
                emit(MusicEvents.PLAYBACK_STATE, getPlayerStateBundle(it))

                if (it == AudioPlayerState.ENDED && player.nextItem == null) {
                    emitQueueEndedEvent()
                }
            }
        }

        scope.launch {
            event.audioItemTransition.collect {
                if (it !is AudioItemTransitionReason.REPEAT) {
                    emitPlaybackTrackChangedEvents(
                        player.currentIndex,
                        player.previousIndex,
                        (it?.oldPosition ?: 0).toSeconds()
                    )
                }
            }
        }

        scope.launch {
            event.onAudioFocusChanged.collect {
                Bundle().apply {
                    putBoolean(IS_FOCUS_LOSS_PERMANENT_KEY, it.isFocusLostPermanently)
                    putBoolean(IS_PAUSED_KEY, it.isPaused)
                    emit(MusicEvents.BUTTON_DUCK, this)
                }
            }
        }

        scope.launch {
            event.onPlayerActionTriggeredExternally.collect {
                when (it) {
                    is MediaSessionCallback.RATING -> {
                        Bundle().apply {
                            setRating(this, "rating", it.rating)
                            emit(MusicEvents.BUTTON_SET_RATING, this)
                        }
                    }
                    is MediaSessionCallback.SEEK -> {
                        Bundle().apply {
                            putDouble("position", it.positionMs.toSeconds())
                            emit(MusicEvents.BUTTON_SEEK_TO, this)
                        }
                    }
                    MediaSessionCallback.PLAY -> emit(MusicEvents.BUTTON_PLAY)
                    MediaSessionCallback.PAUSE -> emit(MusicEvents.BUTTON_PAUSE)
                    MediaSessionCallback.NEXT -> emit(MusicEvents.BUTTON_SKIP_NEXT)
                    MediaSessionCallback.PREVIOUS -> emit(MusicEvents.BUTTON_SKIP_PREVIOUS)
                    MediaSessionCallback.STOP -> emit(MusicEvents.BUTTON_STOP)
                    MediaSessionCallback.FORWARD -> {
                        Bundle().apply {
                            val interval = latestOptions?.getDouble(FORWARD_JUMP_INTERVAL_KEY, DEFAULT_JUMP_INTERVAL) ?:
                            DEFAULT_JUMP_INTERVAL
                            putInt("interval", interval.toInt())
                            emit(MusicEvents.BUTTON_JUMP_FORWARD, this)
                        }
                    }
                    MediaSessionCallback.REWIND -> {
                        Bundle().apply {
                            val interval = latestOptions?.getDouble(BACKWARD_JUMP_INTERVAL_KEY, DEFAULT_JUMP_INTERVAL) ?:
                            DEFAULT_JUMP_INTERVAL
                            putInt("interval", interval.toInt())
                            emit(MusicEvents.BUTTON_JUMP_BACKWARD, this)
                        }
                    }

                    is MediaSessionCallback.CUSTOMACTION -> {
                        Bundle().apply {
                            putString("customAction", it.customAction)
                            emit(MusicEvents.BUTTON_CUSTOM_ACTION, this)
                        }
                    }
                }
            }
        }

        scope.launch {
            event.onTimedMetadata.collect {
                val data = MetadataAdapter.fromMetadata(it)
                val bundle = Bundle().apply {
                    putParcelableArrayList(METADATA_PAYLOAD_KEY, ArrayList(data))
                }
                emit(MusicEvents.METADATA_TIMED_RECEIVED, bundle)

                // TODO: Handle the different types of metadata and publish to new events
                val metadata = PlaybackMetadata.fromId3Metadata(it)
                    ?: PlaybackMetadata.fromIcy(it)
                    ?: PlaybackMetadata.fromVorbisComment(it)
                    ?: PlaybackMetadata.fromQuickTime(it)

                if (metadata != null) {
                    Bundle().apply {
                        putString("source", metadata.source)
                        putString("title", metadata.title)
                        putString("url", metadata.url)
                        putString("artist", metadata.artist)
                        putString("album", metadata.album)
                        putString("date", metadata.date)
                        putString("genre", metadata.genre)
                        emit(MusicEvents.PLAYBACK_METADATA, this)
                    }
                }
            }
        }

        scope.launch {
            event.onCommonMetadata.collect {
                val data = MetadataAdapter.fromMediaMetadata(it)
                val bundle = Bundle().apply {
                    putBundle(METADATA_PAYLOAD_KEY, data)
                }
                emit(MusicEvents.METADATA_COMMON_RECEIVED, bundle)
            }
        }

        scope.launch {
            event.playWhenReadyChange.collect {
                Bundle().apply {
                    putBoolean("playWhenReady", it.playWhenReady)
                    emit(MusicEvents.PLAYBACK_PLAY_WHEN_READY_CHANGED, this)
                }
            }
        }

        scope.launch {
            event.playbackError.collect {
                emit(MusicEvents.PLAYBACK_ERROR, getPlaybackErrorBundle())
            }
        }
    }

    private fun getPlaybackErrorBundle(): Bundle {
        val bundle = Bundle()
        val error = playbackError
        if (error?.message != null) {
            bundle.putString("message", error.message)
        }
        if (error?.code != null) {
            bundle.putString("code", "android-" + error.code)
        }
        return bundle
    }

    @MainThread
    fun emit(event: String, data: Bundle? = null) {
        reactNativeHost.reactInstanceManager.currentReactContext
            ?.emitDeviceEvent(event, data?.let { Arguments.fromBundle(it) })
    }

    @MainThread
    private fun emitList(event: String, data: List<Bundle> = emptyList()) {
        val payload = Arguments.createArray()
        data.forEach { payload.pushMap(Arguments.fromBundle(it)) }

        reactNativeHost.reactInstanceManager.currentReactContext
            ?.emitDeviceEvent(event, payload)
    }

    override fun getTaskConfig(intent: Intent?): HeadlessJsTaskConfig {
        return HeadlessJsTaskConfig(TASK_KEY, Arguments.createMap(), 0, true)
    }

    @MainThread
    override fun onBind(intent: Intent?): IBinder? {
        val intentAction = intent?.action
        Log.d("APM", "onbind: $intentAction", )
        return if (intentAction != null) {
            super.onBind(intent)
        } else {
            binder
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("APM", "unbind: ${intent?.action}")
        return super.onUnbind(intent)
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // https://github.com/androidx/media/issues/843#issuecomment-1860555950
        super.onUpdateNotification(session, true)
    }

    @MainThread
    override fun onTaskRemoved(rootIntent: Intent?) {
        onUnbind(rootIntent)
        Log.d("APM", "onTaskRemoved: ${::player.isInitialized}, $appKilledPlaybackBehavior")
        if (!::player.isInitialized) {
            mediaSession.release()
            return
        }

        when (appKilledPlaybackBehavior) {
            AppKilledPlaybackBehavior.PAUSE_PLAYBACK -> player.pause()
            AppKilledPlaybackBehavior.STOP_PLAYBACK_AND_REMOVE_NOTIFICATION -> {
                Log.d("APM", "onTaskRemoved: Killing service")
                mediaSession.release()
                player.clear()
                player.stop()
                // HACK: the service first stops, then starts, then call onTaskRemove. Why system
                // registers the service being restarted?
                player.destroy()
                scope.cancel()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                onDestroy()
                // https://github.com/androidx/media/issues/27#issuecomment-1456042326
                stopSelf()
                exitProcess(0)
            }
            else -> {}
        }
    }

    private fun selfWake(clientPackageName: String): Boolean {
        val reactActivity = reactNativeHost.reactInstanceManager.currentReactContext?.currentActivity
        if (
        // HACK: validate reactActivity is present; if not, send wake intent
            (reactActivity == null || reactActivity.isDestroyed)
            && Settings.canDrawOverlays(this)
        ) {
            val currentTime = System.currentTimeMillis()
            Log.d("APM", "wake from $clientPackageName from ${currentTime - lastWake} ago")
            if (currentTime - lastWake < 100000) {
                return false
            }
            lastWake = currentTime
            Log.d("APM", "$clientPackageName is in the white list of waking activity.")
            val activityIntent = packageManager.getLaunchIntentForPackage(packageName)
            activityIntent!!.data = Uri.parse("trackplayer://service-bound")
            activityIntent.action = Intent.ACTION_VIEW
            activityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            var activityOptions = ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activityOptions = activityOptions.setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            }
            this.startActivity(activityIntent, activityOptions.toBundle())
            return true
        } else {
            Log.d("APM", "$clientPackageName cannot wake up the activity.")
        }
        return false
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        Log.d("APM", "onGetSession: ${controllerInfo.packageName}")
        return mediaSession
    }

    fun notifyChildrenChanged() {
        mediaSession.connectedControllers.forEach {
                controller ->
            mediaTree.forEach {
                    it -> mediaSession.notifyChildrenChanged(controller, it.key, it.value.size, null)
            }

        }
    }

    @MainThread
    override fun onHeadlessJsTaskFinish(taskId: Int) {
        // This is empty so ReactNative doesn't kill this service
    }

    @MainThread
    override fun onDestroy() {
        Log.d("APM", "RNTP service is destroyed.")
        if (::player.isInitialized) {
            mediaSession.release()
            player.destroy()
        }

        progressUpdateJob?.cancel()
        super.onDestroy()
    }

    @MainThread
    inner class MusicBinder : Binder() {
        val service = this@MusicService
    }

    private inner class APMMediaSessionCallback: MediaLibrarySession.Callback {
        // HACK: I'm sure most of the callbacks were not implemented correctly.
        // ATM I only care that andorid auto still functions.

        private val rootItem = buildMediaItem(title = "root", mediaId = AA_ROOT_KEY, isPlayable = false)
        private val forYouItem = buildMediaItem(title = "For You", mediaId = AA_FOR_YOU_KEY, isPlayable = false)

        // Configure commands available to the controller in onConnect()
        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            Log.d("APM", "connection via: ${controller.packageName}")

            if (controller.packageName in arrayOf(
                    "com.android.systemui",
                    // https://github.com/googlesamples/android-media-controller
                    "com.example.android.mediacontroller",
                    // Android Auto
                    "com.google.android.projection.gearhead"
                )) {
                // HACK: attempt to wake up activity (for legacy APM). if not, start headless.
                if (!selfWake(controller.packageName)) {
                    onStartCommand(null, 0, 0)
                }
            }
            return if (
                session.isMediaNotificationController(controller) ||
                session.isAutomotiveController(controller) ||
                session.isAutoCompanionController(controller)
            ) {
                MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setCustomLayout(customLayout)
                    .setAvailableSessionCommands(sessionCommands ?: MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS)
                    .setAvailablePlayerCommands(playerCommands ?: MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                    .build()
            } else {
                super.onConnect(session, controller)
            }
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            emit(MusicEvents.BUTTON_CUSTOM_ACTION, Bundle().apply { putString("customAction", customCommand.customAction) })
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootExtras = Bundle().apply {
                putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
                putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", mediaTreeStyle[0])
                putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT",  mediaTreeStyle[1])
            }
            val libraryParams = LibraryParams.Builder().setExtras(rootExtras).build()
            Log.d("APM", "acquiring root: ${browser.packageName}")
            // https://github.com/androidx/media/issues/1731#issuecomment-2411109462
            val mRootItem = when (browser.packageName) {
                "com.google.android.googlequicksearchbox" -> {
                    if (mediaTree[AA_FOR_YOU_KEY] == null) rootItem else forYouItem
                }
                else -> rootItem
            }
            return Futures.immediateFuture(LibraryResult.ofItem(mRootItem, libraryParams))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            emit(MusicEvents.BUTTON_BROWSE, Bundle().apply { putString("mediaId", parentId) });
            return Futures.immediateFuture(LibraryResult.ofItemList(mediaTree[parentId] ?: listOf(), null))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            Log.d("APM", "acquiring item: ${browser.packageName}, $mediaId")
            // emit(MusicEvents.BUTTON_PLAY_FROM_ID, Bundle().apply { putString("id", mediaId) })
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, null))
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            Log.d("APM", "searching: ${browser.packageName}, $query")
            return super.onSearch(session, browser, query, params)
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            Log.d("APM", "addMediaItem: ${controller.packageName}, ${mediaItems[0].mediaId}, ${mediaItems.size}")
            return super.onAddMediaItems(mediaSession, controller, mediaItems)
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            Log.d("APM", "setMediaItem: ${controller.packageName}, ${mediaItems[0].mediaId}")
            if (mediaItems[0].requestMetadata.searchQuery == null) {
                emit(MusicEvents.BUTTON_PLAY_FROM_ID, Bundle().apply {
                    putString("id", mediaItems[0].mediaId)
                })
            } else {
                emit(MusicEvents.BUTTON_PLAY_FROM_SEARCH, Bundle().apply {
                    putString("query", mediaItems[0].requestMetadata.searchQuery)
                })
            }
            return super.onSetMediaItems(
                mediaSession,
                controller,
                mediaItems,
                startIndex,
                startPositionMs
            )
        }

        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent
        ): Boolean {
            val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            }
            Log.d("APM", "onMediaBtn: ${controllerInfo.packageName}, $intent, $keyEvent")

            if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        emit(MusicEvents.BUTTON_SKIP_NEXT)
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        emit(MusicEvents.BUTTON_SKIP_PREVIOUS)
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD, KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                        emit(MusicEvents.BUTTON_JUMP_FORWARD)
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD, KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                        emit(MusicEvents.BUTTON_JUMP_BACKWARD)
                        return true
                    }
                    else -> {
                    }
                }
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            Log.d("APM", "searching2: ${browser.packageName}, $query")
            return super.onGetSearchResult(session, browser, query, page, pageSize, params)
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            emit(MusicEvents.PLAYBACK_RESUME, Bundle().apply {
                putString("package", controller.packageName)
            })
            return super.onPlaybackResumption(mediaSession, controller)
        }
    }

    private fun getPendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        } else {
            PendingIntent.FLAG_CANCEL_CURRENT
        }
    }

    companion object {
        const val EMPTY_NOTIFICATION_ID = 1
        const val STATE_KEY = "state"
        const val ERROR_KEY  = "error"
        const val EVENT_KEY = "event"
        const val DATA_KEY = "data"
        const val TRACK_KEY = "track"
        const val NEXT_TRACK_KEY = "nextTrack"
        const val POSITION_KEY = "position"
        const val DURATION_KEY = "duration"
        const val BUFFERED_POSITION_KEY = "buffered"

        const val TASK_KEY = "TrackPlayer"

        const val MIN_BUFFER_KEY = "minBuffer"
        const val MAX_BUFFER_KEY = "maxBuffer"
        const val PLAY_BUFFER_KEY = "playBuffer"
        const val BACK_BUFFER_KEY = "backBuffer"

        const val FORWARD_JUMP_INTERVAL_KEY = "forwardJumpInterval"
        const val BACKWARD_JUMP_INTERVAL_KEY = "backwardJumpInterval"
        const val PROGRESS_UPDATE_EVENT_INTERVAL_KEY = "progressUpdateEventInterval"

        const val MAX_CACHE_SIZE_KEY = "maxCacheSize"

        const val ANDROID_OPTIONS_KEY = "android"

        const val CUSTOM_ACTIONS_KEY = "customActions"
        const val CUSTOM_ACTIONS_LIST_KEY = "customActionsList"

        const val STOPPING_APP_PAUSES_PLAYBACK_KEY = "stoppingAppPausesPlayback"
        const val APP_KILLED_PLAYBACK_BEHAVIOR_KEY = "appKilledPlaybackBehavior"
        const val AUDIO_OFFLOAD_KEY = "audioOffload"
        const val STOP_FOREGROUND_GRACE_PERIOD_KEY = "stopForegroundGracePeriod"
        const val PAUSE_ON_INTERRUPTION_KEY = "alwaysPauseOnInterruption"
        const val AUTO_UPDATE_METADATA = "autoUpdateMetadata"
        const val AUTO_HANDLE_INTERRUPTIONS = "autoHandleInterruptions"
        const val ANDROID_AUDIO_CONTENT_TYPE = "androidAudioContentType"
        const val IS_FOCUS_LOSS_PERMANENT_KEY = "permanent"
        const val IS_PAUSED_KEY = "paused"

        const val AA_FOR_YOU_KEY = "for-you"
        const val AA_ROOT_KEY = "/"

        const val DEFAULT_JUMP_INTERVAL = 15.0
        const val DEFAULT_STOP_FOREGROUND_GRACE_PERIOD = 5
    }
}

