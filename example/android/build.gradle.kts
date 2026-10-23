// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    extra.apply {
        set("buildToolsVersion", "37.0.0")
        set("minSdkVersion", 24)
        set("compileSdkVersion", 37)
        set("targetSdkVersion", 37)

        // We use NDK 23 which has both M1 support and is the side-by-side NDK version from AGP.

        set("ndkVersion", "27.1.12297006")
        set("kotlinVersion", "2.2.20")
    }
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle")
        classpath("com.facebook.react:react-native-gradle-plugin")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin")
    }
}

apply(plugin = "com.facebook.react.rootproject")
