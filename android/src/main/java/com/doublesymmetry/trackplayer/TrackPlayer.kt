package com.doublesymmetry.trackplayer

import com.doublesymmetry.trackplayer.module.MusicModule
import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class TrackPlayer : BaseReactPackage() {
    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? =
        if (name == MusicModule.NAME) {
            MusicModule(reactContext)
        } else {
            null
        }

    override fun getReactModuleInfoProvider() = ReactModuleInfoProvider {
          mapOf(
              MusicModule.NAME to ReactModuleInfo(
                  MusicModule.NAME,
                  MusicModule.NAME,
                  canOverrideExistingModule = false, // canOverrideExistingModule
                  needsEagerInit = false, // needsEagerInit
                      isCxxModule = false, // isCxxModule
                  isTurboModule = true // isTurboModule
              )
          )
    }
}