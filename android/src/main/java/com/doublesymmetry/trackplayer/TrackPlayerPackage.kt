package com.doublesymmetry.trackplayer

import com.doublesymmetry.trackplayer.module.MusicModule
import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class TrackPlayerPackage : BaseReactPackage() {
    override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? = null

    override fun getReactModuleInfoProvider() = ReactModuleInfoProvider {
          mapOf(
              CalculatorModule.NAME to ReactModuleInfo(
                  CalculatorModule.NAME,
                  CalculatorModule.NAME,
                  false, // canOverrideExistingModule
                  false, // needsEagerInit
                      false, // isCxxModule
                  true // isTurboModule
                            )
                     )
         }

}