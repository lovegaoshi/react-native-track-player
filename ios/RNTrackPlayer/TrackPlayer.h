#import <React/RCTEventEmitter.h>

#ifdef RCT_NEW_ARCH_ENABLED

#import <NativeTrackPlayerSpec/NativeTrackPlayerSpec.h>
NS_ASSUME_NONNULL_BEGIN

@interface NativeTrackPlayer: RCTEventEmitter <NativeTrackPlayerSpec>

@end

NS_ASSUME_NONNULL_END

#endif
