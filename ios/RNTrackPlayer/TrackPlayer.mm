#import "TrackPlayer.h"
#import "react_native_track_player-Swift.h"

@interface NativeTrackPlayer () <RNTPDelegate>
@end

@implementation NativeTrackPlayer {
    RNTrackPlayer *trackPlayer;
}

RCT_EXPORT_MODULE()

- (instancetype)init {
    self = [super init];
    if(self) {
        trackPlayer = [RNTrackPlayer new];
        trackPlayer.delegate = self;
    }
    return self;
}

+ (BOOL)requiresMainQueueSetup {
    return NO;
}

- (void)sendEvent:(NSString *)name body:(id)body {
  [super sendEventWithName:name body:body];
}

- (NSArray<NSString *> *) supportedEvents {
    return [RNTrackPlayer supportedEvents];
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:(const facebook::react::ObjCTurboModule::InitParams &)params {
    return std::make_shared<facebook::react::NativeTrackPlayerSpecJSI>(params);
}

- (void)add:(nonnull NSArray *)tracks insertBeforeIndex:(double)insertBeforeIndex resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
    [trackPlayer add:tracks before:insertBeforeIndex resolver:resolve rejecter:reject];
}


- (nonnull facebook::react::ModuleConstants<JS::NativeTrackPlayer::Constants::Builder>)constantsToExport {
    return [RNTrackPlayer constantsToExport];
}

- (void)getActiveTrack:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer getActiveTrack:resolve rejecter:reject];
}

- (void)getActiveTrackIndex:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer getActiveTrackIndex:resolve rejecter:reject];
}

- (nonnull facebook::react::ModuleConstants<JS::NativeTrackPlayer::Constants::Builder>)getConstants { 
    return [self constantsToExport];
}

- (void)getPlayWhenReady:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer getPlayWhenReady:resolve rejecter:reject];
}

- (void)getPlaybackState:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer getPlaybackState:resolve rejecter:reject];
}

- (void)getProgress:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer getProgress:resolve rejecter:reject];
}

- (void)getQueue:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer getQueue:resolve rejecter:reject];
}

- (void)getRate:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer getRate:resolve rejecter:reject];
}

- (void)getRepeatMode:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer getRepeatMode:resolve rejecter:reject];
}

- (void)getTrack:(double)index resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer getTrack:index resolver:resolve rejecter:reject];
}

- (void)getVolume:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer getVolume:resolve rejecter:reject];
}

- (void)load:(nonnull NSDictionary *)track resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer load:track resolver:resolve rejecter:reject];
}

- (void)move:(double)fromIndex toIndex:(double)toIndex resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
    [trackPlayer move:fromIndex toIndex:toIndex resolver:resolve rejecter:reject];
}

- (void)pause:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer pause:resolve rejecter:reject];
}

- (void)play:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer play:resolve rejecter:reject];
}

- (void)remove:(nonnull NSArray *)indexes resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer remove:indexes resolver:resolve rejecter:reject];
}

- (void)removeUpcomingTracks:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer removeUpcomingTracks:resolve rejecter:reject];
}

- (void)reset:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer reset:resolve rejecter:reject];
}

- (void)retry:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer retry:resolve rejecter:reject];
}

- (void)seekBy:(double)offset resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer seekBy:offset resolver:resolve rejecter:reject];
}

- (void)seekTo:(double)position resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer seekTo:position resolver:resolve rejecter:reject];
}

- (void)setPlayWhenReady:(BOOL)playWhenReady resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer setPlayWhenReady:playWhenReady resolver:resolve rejecter:reject];
}

- (void)setQueue:(nonnull NSArray *)tracks resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer setQueue:tracks resolver:resolve rejecter:reject];
}

- (void)setRate:(double)rate resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer setRate:rate resolver:resolve rejecter:reject];
}

- (void)setRepeatMode:(double)mode resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer setRepeatMode:mode resolver:resolve rejecter:reject];
}

- (void)setVolume:(double)level resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer setVolume:level resolver:resolve rejecter:reject];
}

- (void)setupPlayer:(nonnull NSDictionary *)options background:(BOOL)background resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer setupPlayer:options resolver:resolve rejecter:reject];
}

- (void)skip:(double)index initialPosition:(double)initialPosition resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer skip:index initialTime:initialPosition resolver:resolve rejecter:reject];
}

- (void)skipToNext:(double)initialPosition resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer skipToNext:initialPosition resolver:resolve rejecter:reject];
}

- (void)skipToPrevious:(double)initialPosition resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer skipToPrevious:initialPosition resolver:resolve rejecter:reject];
}

- (void)stop:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer stop:resolve rejecter:reject];
}

- (void)updateMetadataForTrack:(double)trackIndex metadata:(nonnull NSDictionary *)metadata resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer updateMetadataForTrack:trackIndex metadata:metadata resolver:resolve rejecter:reject];
}

- (void)updateNowPlayingMetadata:(nonnull NSDictionary *)metadata resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer updateNowPlayingMetadata:metadata resolver:resolve rejecter:reject];
}

- (void)updateOptions:(nonnull NSDictionary *)options resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject { 
    [trackPlayer updateOptions:options resolver:resolve rejecter:reject];
}

// android only. placeholder.
- (void)abandonWakeLock:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
    <#code#>
}

- (void)acquireWakeLock:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
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

- (void)setPlaybackState:(nonnull NSString *)mediaID resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
}

- (void)switchExoPlayer:(double)fadeDuration fadeInterval:(double)fadeInterval fadeToVolume:(double)fadeToVolume resolve:(nonnull RCTPromiseResolveBlock)resolve reject:(nonnull RCTPromiseRejectBlock)reject {
    <#code#>
}

@end
