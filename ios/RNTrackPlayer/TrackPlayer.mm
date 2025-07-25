#import "TrackPlayer.h"

@implementation NativeTrackPlayer

RCT_EXPORT_MODULE()



- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params { 
    <#code#>
}

- (void)abandonWakeLock:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)acquireWakeLock:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)add:(nonnull NSArray *)tracks insertBeforeIndex:(double)insertBeforeIndex resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)addListener:(nonnull NSString *)eventName { 
    <#code#>
}

- (nonnull facebook::react::ModuleConstants<JS::NativeTrackPlayer::Constants::Builder>)constantsToExport { 
    <#code#>
}

- (void)crossFadePrepare:(BOOL)previous resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)fadeOutJump:(double)index duration:(double)duration interval:(double)interval toVolume:(double)toVolume resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)fadeOutNext:(double)duration interval:(double)interval toVolume:(double)toVolume resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)fadeOutPause:(double)duration interval:(double)interval resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)fadeOutPrevious:(double)duration interval:(double)interval toVolume:(double)toVolume resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)getActiveTrack:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)getActiveTrackIndex:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (nonnull facebook::react::ModuleConstants<JS::NativeTrackPlayer::Constants::Builder>)getConstants { 
    <#code#>
}

- (void)getCurrentEqualizerPreset:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)getEqualizerPresets:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)getLastConnectedPackage:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)getPitch:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)getPlayWhenReady:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)getPlaybackState:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)getProgress:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)getQueue:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)getRate:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)getRepeatMode:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)getTrack:(double)index resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)getVolume:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)load:(nonnull NSDictionary *)track resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)move:(double)fromIndex toIndex:(double)toIndex resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)pause:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)play:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)remove:(nonnull NSArray *)indexes resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)removeListeners:(double)count { 
    <#code#>
}

- (void)removeUpcomingTracks:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)reset:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)retry:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)seekBy:(double)offset resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)seekTo:(double)position resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)setAnimatedVolume:(double)volume duration:(double)duration interval:(double)interval msg:(nonnull NSString *)msg resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)setBrowseTree:(nonnull NSDictionary *)browseTree resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)setBrowseTreeStyle:(double)browsableStyle playableStyle:(double)playableStyle resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)setEqualizerPreset:(double)preset resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)setLoudnessEnhance:(double)gain resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)setPitch:(double)pitch resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)setPlayWhenReady:(BOOL)playWhenReady resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)setPlaybackState:(nonnull NSString *)mediaID resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)setQueue:(nonnull NSArray *)tracks resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)setRate:(double)rate resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)setRepeatMode:(double)mode resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)setVolume:(double)level resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)setupPlayer:(nonnull NSDictionary *)options background:(BOOL)background resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)skip:(double)index initialPosition:(double)initialPosition resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)skipToNext:(double)initialPosition resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)skipToPrevious:(double)initialPosition resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)stop:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)switchExoPlayer:(double)fadeDuration fadeInterval:(double)fadeInterval fadeToVolume:(double)fadeToVolume resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)updateMetadataForTrack:(double)trackIndex metadata:(nonnull NSDictionary *)metadata resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)updateNowPlayingMetadata:(nonnull NSDictionary *)metadata resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

- (void)updateOptions:(nonnull NSDictionary *)options resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    <#code#>
}

@end
