import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.4.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

if (!rootProject.extra.has("native")) {
    apply(plugin = "com.facebook.react")
}

extra["getExtOrDefault"] = { name: String ->
    if (rootProject.extra.has(name)) {
        rootProject.extra[name]
    } else {
        findProperty("TrackPlayer_$name")
    }
}

fun getExtOrIntegerDefault(name: String, defaultInt: Int): Int {
    return try {
        val value = if (rootProject.extra.has(name)) {
            rootProject.extra[name]
        } else {
            findProperty("RNTP_$name")
        }
        when (value) {
            is Number -> value.toInt()
            is String -> value.toInt()
            else -> defaultInt
        }
    } catch (ignored: Exception) {
        defaultInt
    }
}

configure<LibraryExtension> {
    compileSdk = getExtOrIntegerDefault("compileSdkVersion", 35)
    namespace = "com.doublesymmetry.trackplayer"

    defaultConfig {
        minSdk = getExtOrIntegerDefault("minSdkVersion", 23) // RN's minimum version

        consumerProguardFiles("proguard-rules.txt")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    if (rootProject.extra.has("native")) {
        //noinspection WrongGradleMethod
        kotlin {
            compilerOptions {
                // HACK: does RN auto sets to 17? or am i crazy?
                jvmTarget.set(JvmTarget.JVM_11)
            }
        }
    }

    sourceSets {
        getByName("main") {
            java.directories.add("build/generated/source/codegen")
        }
    }
}

repositories {
    mavenLocal()
    maven {
        // As RN is not updated in jcenter anymore, we'll have to grab it from npm
        // Make sure you have installed the react-native npm package before compiling
        url = uri("../node_modules/react-native/android")
    }

    mavenCentral()
    google()
}

dependencies {
    //noinspection GradleDynamicVersion
    if (rootProject.extra.has("native")) {
        implementation("com.facebook.react:react-android:+")
    } else {
        implementation("com.facebook.react:react-native:+")
    }
    // Make sure we're using androidx
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.lifecycle:lifecycle-process:2.11.0")
    implementation("androidx.media:media:1.8.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.12.1")
    implementation("androidx.compose.ui:ui-graphics:1.12.1")

    implementation("androidx.media3:media3-exoplayer:1.12.0-alpha01")
    implementation("androidx.media3:media3-session:1.12.0-alpha01")
    implementation("androidx.media3:media3-ui:1.12.0-alpha01")
    implementation("androidx.media3:media3-exoplayer-hls:1.12.0-alpha01")
    implementation("androidx.media3:media3-exoplayer-dash:1.12.0-alpha01")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:1.12.0-alpha01")
    implementation("androidx.media3:media3-common:1.12.0-alpha01")
    implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.9.0+1")

    implementation("com.google.guava:guava:33.7.2-android")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    implementation("io.coil-kt:coil:2.7.0")
    api("com.jakewharton.timber:timber:5.0.1")

    implementation("androidx.test:rules:1.7.0")
    implementation("jp.wasabeef.transformers:coil:1.0.6")
}
