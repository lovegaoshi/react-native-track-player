#include <jni.h>
#include <jsi/jsi.h>
#include <android/log.h>
#include <string>

using namespace facebook;

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "RNTP", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "RNTP", __VA_ARGS__)

static jsi::Runtime* globalRuntime = nullptr;
static JavaVM* gJvm = nullptr;

// Forward declaration
static jobject jsiValueToJava(JNIEnv* env, jsi::Runtime& rt, const jsi::Value& value);

// Convert JS object to java.util.HashMap
static jobject jsiObjectToMap(JNIEnv* env, jsi::Runtime& rt, const jsi::Object& obj) {
    jclass hashMapClass = env->FindClass("java/util/HashMap");
    jmethodID hashMapCtor = env->GetMethodID(hashMapClass, "<init>", "()V");
    jmethodID putMethod = env->GetMethodID(hashMapClass, "put",
                                           "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jobject map = env->NewObject(hashMapClass, hashMapCtor);

    jsi::Array keys = obj.getPropertyNames(rt);
    for (size_t i = 0; i < keys.size(rt); i++) {
        std::string key = keys.getValueAtIndex(rt, i).toString(rt).utf8(rt);
        jsi::Value val = obj.getProperty(rt, key.c_str());
        jstring jKey = env->NewStringUTF(key.c_str());
        jobject jVal = jsiValueToJava(env, rt, val);
        env->CallObjectMethod(map, putMethod, jKey, jVal);
        env->DeleteLocalRef(jKey);
        if (jVal) env->DeleteLocalRef(jVal);
    }
    return map;
}

// Convert JS array to java.util.ArrayList
static jobject jsiArrayToList(JNIEnv* env, jsi::Runtime& rt, const jsi::Array& arr) {
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListCtor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jobject list = env->NewObject(arrayListClass, arrayListCtor);

    for (size_t i = 0; i < arr.size(rt); i++) {
        jobject elem = jsiValueToJava(env, rt, arr.getValueAtIndex(rt, i));
        env->CallBooleanMethod(list, addMethod, elem);
        if (elem) env->DeleteLocalRef(elem);
    }
    return list;
}

// Convert JSI value to appropriate Java object
static jobject jsiValueToJava(JNIEnv* env, jsi::Runtime& rt, const jsi::Value& value) {
    if (value.isUndefined() || value.isNull()) return nullptr;

    if (value.isBool()) {
        jclass booleanClass = env->FindClass("java/lang/Boolean");
        jmethodID ctor = env->GetMethodID(booleanClass, "<init>", "(Z)V");
        return env->NewObject(booleanClass, ctor, value.getBool());
    }

    if (value.isNumber()) {
        jclass doubleClass = env->FindClass("java/lang/Double");
        jmethodID ctor = env->GetMethodID(doubleClass, "<init>", "(D)V");
        return env->NewObject(doubleClass, ctor, value.asNumber());
    }

    if (value.isString()) {
        return env->NewStringUTF(value.getString(rt).utf8(rt).c_str());
    }

    if (value.isObject()) {
        jsi::Object obj = value.getObject(rt);
        if (obj.isArray(rt)) return jsiArrayToList(env, rt, obj.asArray(rt));
        else return jsiObjectToMap(env, rt, obj);
    }

    return nullptr;
}

// JNI: call JS function that returns a promise and resolve/reject via callback
extern "C"
JNIEXPORT void JNICALL
Java_com_doublesymmetry_trackplayer_utils_JsiBridge_nativeCallJS(
    JNIEnv* env, jobject thiz, jstring jFnName, jstring jInput, jobject jCallback) {

    if (!globalRuntime) {
        LOGI("JSI Runtime not initialized");
        return;
    }

    jobject callbackGlobal = env->NewGlobalRef(jCallback);
    const char* cFn = env->GetStringUTFChars(jFnName, nullptr);
    const char* cInput = env->GetStringUTFChars(jInput, nullptr);
    std::string fnName(cFn);
    std::string input(cInput);
    env->ReleaseStringUTFChars(jFnName, cFn);
    env->ReleaseStringUTFChars(jInput, cInput);

    auto rt = globalRuntime;

    try {
        // Use JS helper: global.__nativeJSIAsyncCall(fnName, arg, resolve, reject) which handles then/catch in JS
        jsi::Value callVal = rt->global().getProperty(*rt, "__nativeJSIAsyncCall");
        if (!callVal.isObject() || !callVal.getObject(*rt).isFunction(*rt)) {
            LOGI("CPP: __nativeJSIAsyncCall not found or not a function");
            JNIEnv* envCb;
            if (gJvm && gJvm->GetEnv(reinterpret_cast<void**>(&envCb), JNI_VERSION_1_6) == JNI_OK) {
                jclass cls = envCb->GetObjectClass(callbackGlobal);
                jmethodID onReject = envCb->GetMethodID(cls, "onReject", "(Ljava/lang/String;)V");
                jstring jErr = envCb->NewStringUTF("__nativeJSIAsyncCall not available");
                envCb->CallVoidMethod(callbackGlobal, onReject, jErr);
                envCb->DeleteLocalRef(jErr);
            }
            env->DeleteGlobalRef(callbackGlobal);
            return;
        }
        jsi::Function callFn = callVal.getObject(*rt).asFunction(*rt);
        // Build resolve/reject native handlers first

        // Capture callbackGlobal; acquire JNIEnv from JavaVM inside callbacks
        auto resolveFn = jsi::Function::createFromHostFunction(
            *rt, jsi::PropNameID::forAscii(*rt, "resolveHandler"), 1,
            [callbackGlobal](jsi::Runtime& runtime, const jsi::Value&, const jsi::Value* args, size_t count) -> jsi::Value {
                JNIEnv* envCb = nullptr;
                bool didAttach = false;
                if (gJvm && gJvm->GetEnv(reinterpret_cast<void**>(&envCb), JNI_VERSION_1_6) != JNI_OK) {
                    if (gJvm->AttachCurrentThread(&envCb, nullptr) == JNI_OK) {
                        didAttach = true;
                    }
                }
                if (!envCb) {
                    LOGE("CPP: Failed to get JNIEnv in resolve");
                    return jsi::Value::undefined();
                }

                jclass cls = envCb->GetObjectClass(callbackGlobal);
                jmethodID onResolve = envCb->GetMethodID(cls, "onResolve", "(Ljava/util/Map;)V");
                jobject argMap = nullptr;

                if (count > 0) {
                    const jsi::Value& v = args[0];
                    if (v.isObject()) {
                        jsi::Object o = v.getObject(runtime);
                        if (!o.isArray(runtime)) {
                            argMap = jsiObjectToMap(envCb, runtime, o);
                        }
                    }
                    if (!argMap) {
                        // Wrap non-object (or arrays) into a map under key "value"
                        jclass hashMapClass = envCb->FindClass("java/util/HashMap");
                        jmethodID hashMapCtor = envCb->GetMethodID(hashMapClass, "<init>", "()V");
                        jmethodID putMethod = envCb->GetMethodID(hashMapClass, "put",
                            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
                        argMap = envCb->NewObject(hashMapClass, hashMapCtor);
                        jstring jKey = envCb->NewStringUTF("value");
                        jobject jVal = jsiValueToJava(envCb, runtime, v);
                        envCb->CallObjectMethod(argMap, putMethod, jKey, jVal);
                        envCb->DeleteLocalRef(jKey);
                        if (jVal) envCb->DeleteLocalRef(jVal);
                    }
                }

                envCb->CallVoidMethod(callbackGlobal, onResolve, argMap);
                if (argMap) envCb->DeleteLocalRef(argMap);

                envCb->DeleteGlobalRef(callbackGlobal);
                if (didAttach && gJvm) {
                    gJvm->DetachCurrentThread();
                }
                return jsi::Value::undefined();
            }
        );

        auto rejectFn = jsi::Function::createFromHostFunction(
            *rt, jsi::PropNameID::forAscii(*rt, "rejectHandler"), 1,
            [callbackGlobal](jsi::Runtime& runtime, const jsi::Value&, const jsi::Value* args, size_t count) -> jsi::Value {
                JNIEnv* envCb = nullptr;
                bool didAttach = false;
                if (gJvm && gJvm->GetEnv(reinterpret_cast<void**>(&envCb), JNI_VERSION_1_6) != JNI_OK) {
                    if (gJvm->AttachCurrentThread(&envCb, nullptr) == JNI_OK) {
                        didAttach = true;
                    }
                }
                if (!envCb) {
                    return jsi::Value::undefined();
                }

                jclass cls = envCb->GetObjectClass(callbackGlobal);
                jmethodID onReject = envCb->GetMethodID(cls, "onReject", "(Ljava/lang/String;)V");
                std::string err = count > 0 ? args[0].toString(runtime).utf8(runtime) : "Unknown error";
                jstring jErr = envCb->NewStringUTF(err.c_str());

                envCb->CallVoidMethod(callbackGlobal, onReject, jErr);
                envCb->DeleteLocalRef(jErr);
                envCb->DeleteGlobalRef(callbackGlobal);

                if (didAttach && gJvm) {
                    gJvm->DetachCurrentThread();
                }
                return jsi::Value::undefined();
            }
        );

        // Let JS perform Promise resolution; we just pass handlers
        callFn.call(
            *rt,
            jsi::String::createFromUtf8(*rt, fnName.c_str()),
            jsi::String::createFromUtf8(*rt, input),
            resolveFn,
            rejectFn
        );
    } catch (const std::exception& e) {
        LOGE("CPP: Exception: %s", e.what());
    }

    // Global ref is deleted in resolve/reject handlers
}

// JNI: Initialize JSI runtime
extern "C"
JNIEXPORT void JNICALL
Java_com_doublesymmetry_trackplayer_utils_JsiBridge_initializeJSI(
    JNIEnv* env, jobject thiz, jlong runtimePtr) {
    if (runtimePtr == 0) {
        return;
    }
    globalRuntime = reinterpret_cast<jsi::Runtime*>(runtimePtr);
}

// Cache JavaVM for later thread attachment from JSI callbacks
jint JNI_OnLoad(JavaVM* vm, void*) {
    gJvm = vm;
    LOGI("RNTP JSI Runtime initialized");
    return JNI_VERSION_1_6;
}