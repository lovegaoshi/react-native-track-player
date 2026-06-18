//
//  QueuedAudioPlayer.swift
//  SwiftAudio
//
//  Created by Jørgen Henrichsen on 24/03/2018.
//

import Foundation
import MediaPlayer

/**
 An audio player that can keep track of a queue of AudioItems.
 */
public class QueuedAudioPlayer: AudioPlayer, QueueManagerDelegate {
    let queue: QueueManager = QueueManager<AudioItem>()
    fileprivate var lastIndex: Int = -1
    fileprivate var lastItem: AudioItem? = nil
    
    func findOrInsert(item: AudioItem) -> Int {
        var itemIndex = queue.items.firstIndex(where: {$0.getSourceUrl() == item.getSourceUrl()})
        if (itemIndex == nil) {
            add(item: item)
            itemIndex = queue.items.count - 1
        }
        queue.currentIndex = itemIndex!
        return itemIndex!
    }

    public func crossfadePrepare(item: AudioItem) {
        if (!self.crossfade) {
            return
        }
        self.crossfadeWrapper.load(
            from: item.getSourceUrl(),
            type: item.getSourceType(),
            playWhenReady: false,
            initialTime: (item as? InitialTiming)?.getInitialTime(),
            options:(item as? AssetOptionsProviding)?.getAssetOptions()
        )
        self.crossfadeItem = item
    }
    
    public func crossfadePrepare(previous: Bool = false) {
        let nextIndex = queue.peek(direction: previous ? -1 : 1)
        if (nextIndex < 0) {
            // TODO: should throw error instead
            return
        }
        self.crossfadePrepare(item: queue.items[nextIndex])
    }
    
    public func switchExoPlayer(
        fadeDuration: Int = 2500,
        fadeInterval: Int = 20,
        fadeToVolume: Float = 1
    ) {
        if (!self.crossfade || self.crossfadeItem == nil) {
            return
        }
        if (self.currentAVPlayer) {
            self.crossfadeWrapper = self.wrapper1
            self.wrapper = self.wrapper2!
        } else {
            self.crossfadeWrapper = self.wrapper2!
            self.wrapper = self.wrapper1
        }

        // switch the event emittting delegate
        self.wrapper.delegate = self
        self.crossfadeWrapper.delegate = nil

        // broadcast nowplaying to system
        self.findOrInsert(item: self.crossfadeItem!)
        loadNowPlayingMetaValues()
        emitCurrentItemEvent()

        self.crossfadeItem = nil
        self.currentAVPlayer = !self.currentAVPlayer

        // fade volume
        Task {
            var fadeOutDuration = fadeDuration
            let startFadeOutTime = DispatchTime.now()
            let fadeFromVolume = self.crossfadeWrapper.volume
            while (fadeOutDuration > 0) {
                fadeOutDuration -= fadeInterval
                let timeDiff = DispatchTime.now().uptimeNanoseconds - startFadeOutTime.uptimeNanoseconds
                let timeElapsed = Float(min(Int(timeDiff) / 1_000_000, fadeDuration))
                self.crossfadeWrapper.volume = fadeFromVolume * (1 -  timeElapsed / Float(fadeDuration))
                print("crossfade fading out...\(self.crossfadeWrapper.volume)")
                try await Task.sleep(nanoseconds: UInt64(fadeInterval * 1000000))
            }
        }
        
        Task {
            self.wrapper.volume = 0
            if (fadeToVolume > 0) {
                self.wrapper.play()
                var fadeInDuration = fadeDuration
                let startTime = DispatchTime.now()
                while (fadeInDuration > 0) {
                    fadeInDuration -= fadeInterval
                    let timeDiff = DispatchTime.now().uptimeNanoseconds - startTime.uptimeNanoseconds
                    let timeElapsed = Float(min(Int(timeDiff) / 1_000_000, fadeDuration))
                    self.wrapper.volume = fadeToVolume * timeElapsed / Float(fadeDuration)
                    print("crossfade fading in...\(self.wrapper.volume)")
                    try await Task.sleep(nanoseconds: UInt64(fadeInterval * 1000000))
                }
            }
        }
    }
    
    public override init(
        nowPlayingInfoController: NowPlayingInfoControllerProtocol = NowPlayingInfoController(),
        remoteCommandController: RemoteCommandController = RemoteCommandController(),
        crossfade: Bool = false
    ) {
        super.init(nowPlayingInfoController: nowPlayingInfoController, remoteCommandController: remoteCommandController, crossfade: crossfade)
        queue.delegate = self
    }

    /// The repeat mode for the queue player.
    public var repeatMode: RepeatMode = .off

    public override var currentItem: AudioItem? {
        queue.current
    }

    /**
     The index of the current item.
     */
    public var currentIndex: Int {
        queue.currentIndex
    }

    override public func clear() {
        queue.clearQueue()
        super.clear()
    }

    /**
     All items currently in the queue.
     */
    public var items: [AudioItem] {
        queue.items
    }

    /**
     The previous items held by the queue.
     */
    public var previousItems: [AudioItem] {
        queue.previousItems
    }

    /**
     The upcoming items in the queue.
     */
    public var nextItems: [AudioItem] {
        queue.nextItems
    }

    /**
     Will replace the current item with a new one and load it into the player.

     - parameter item: The AudioItem to replace the current item.
     - parameter playWhenReady: Optional, whether to start playback when the item is ready.
     */
    public override func load(item: AudioItem, playWhenReady: Bool? = nil) {
        handlePlayWhenReady(playWhenReady) {
            queue.replaceCurrentItem(with: item)
        }
    }

    /**
     Add a single item to the queue.

     - parameter item: The item to add.
     - parameter playWhenReady: Optional, whether to start playback when the item is ready.
     */
    public func add(item: AudioItem, playWhenReady: Bool? = nil) {
        handlePlayWhenReady(playWhenReady) {
            queue.add(item)
        }
    }

    /**
     Add items to the queue.

     - parameter items: The items to add to the queue.
     - parameter playWhenReady: Optional, whether to start playback when the item is ready.
     */
    public func add(items: [AudioItem], playWhenReady: Bool? = nil) {
        handlePlayWhenReady(playWhenReady) {
            queue.add(items)
        }
    }

    public func add(items: [AudioItem], at index: Int) throws {
        try queue.add(items, at: index)
    }

    /**
     Step to the next item in the queue.
     */
    public func next() {
        let lastIndex = currentIndex
        let playbackWasActive = wrapper.playbackActive;
        _ = queue.next(wrap: repeatMode == .queue)
        if (playbackWasActive && lastIndex != currentIndex || repeatMode == .queue) {
            event.playbackEnd.emit(data: .skippedToNext)
        }
    }

    /**
     Step to the previous item in the queue.
     */
    public func previous() {
        let lastIndex = currentIndex
        let playbackWasActive = wrapper.playbackActive;
        _ = queue.previous(wrap: repeatMode == .queue)
        if (playbackWasActive && lastIndex != currentIndex || repeatMode == .queue) {
            event.playbackEnd.emit(data: .skippedToPrevious)
        }
    }

    /**
     Remove an item from the queue.

     - parameter index: The index of the item to remove.
     - throws: `AudioPlayerError.QueueError`
     */
    public func removeItem(at index: Int) throws {
        try queue.removeItem(at: index)
    }


    /**
     Jump to a certain item in the queue.

     - parameter index: The index of the item to jump to.
     - parameter playWhenReady: Optional, whether to start playback when the item is ready.
     - throws: `AudioPlayerError`
     */
    public func jumpToItem(atIndex index: Int, playWhenReady: Bool? = nil) throws {
        try handlePlayWhenReady(playWhenReady) {
            if (index == currentIndex) {
                seek(to: 0)
            } else {
                _ = try queue.jump(to: index)
            }
            event.playbackEnd.emit(data: .jumpedToIndex)
        }
    }

    /**
     Move an item in the queue from one position to another.

     - parameter fromIndex: The index of the item to move.
     - parameter toIndex: The index to move the item to.
     - throws: `AudioPlayerError.QueueError`
     */
    public func moveItem(fromIndex: Int, toIndex: Int) throws {
        try queue.moveItem(fromIndex: fromIndex, toIndex: toIndex)
    }

    /**
     Remove all upcoming items, those returned by `next()`
     */
    public func removeUpcomingItems() {
        queue.removeUpcomingItems()
    }

    /**
     Remove all previous items, those returned by `previous()`
     */
    public func removePreviousItems() {
        queue.removePreviousItems()
    }

    func replay() {
        seek(to: 0);
        play()
    }

    // MARK: - AVPlayerWrapperDelegate

    override func AVWrapperItemDidPlayToEndTime() {
        event.playbackEnd.emit(data: .playedUntilEnd)
        if (repeatMode == .track) {
            self.pause()

            // quick workaround for race condition - schedule a call after 2 frames
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.016 * 2) { [weak self] in self?.replay() }
        } else if (repeatMode == .queue) {
            _ = queue.next(wrap: true)
        } else if (currentIndex != items.count - 1) {
            _ = queue.next(wrap: false)
        } else {
            wrapper.state = .ended
        }
    }

    func emitCurrentItemEvent(lastPosition: Double = 0) {
        let currentItem = currentItem
        event.currentItem.emit(
            data: (
                item: currentItem,
                index: currentIndex == -1 ? nil : currentIndex,
                lastItem: lastItem,
                lastIndex: lastIndex == -1 ? nil : lastIndex,
                lastPosition: lastPosition
            )
        )
        lastItem = currentItem
        lastIndex = currentIndex
    }

    // MARK: - QueueManagerDelegate

    func onCurrentItemChanged() {
        let lastPosition = currentTime;
        if let currentItem = currentItem {
            super.load(item: currentItem)
        } else {
            super.clear()
        }
        emitCurrentItemEvent(lastPosition: lastPosition)
    }

    func onSkippedToSameCurrentItem() {
        if (wrapper.playbackActive) {
            replay()
        }
    }

    func onReceivedFirstItem() {
        try! queue.jump(to: 0)
    }
}
