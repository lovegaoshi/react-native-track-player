package com.doublesymmetry.trackplayer.utils

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class JsiBridge(private val reactContext: ReactApplicationContext) {

    // Must be constructed before init block uses it
    private val initialized = AtomicBoolean(false)
    // All async orchestration lives here
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Load native lib first
    init {
        System.loadLibrary("trackplayer")
        initialize() // self-init
    }

    /** ---- Native methods ---- */
    private external fun initializeJSI(runtimePtr: Long)
    private external fun nativeCallJS(fnName: String, input: String, callback: PromiseCallback)

    /** ---- Setup / init ---- */
    private fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            // Prefer RuntimeExecutor when available (RN >= 0.75)
            val usedRuntimeExecutor = try {
                val getRuntimeExecutor = reactContext.javaClass.getMethod("getRuntimeExecutor")
                val runtimeExecutor = getRuntimeExecutor.invoke(reactContext)
                val execMethod = runtimeExecutor.javaClass.getMethod("execute", Runnable::class.java)
                execMethod.invoke(runtimeExecutor, Runnable {
                    // Still need the pointer; read via JavaScriptContextHolder on JS thread
                    val ptr = getJSRuntimePointer()
                    if (ptr != 0L) initializeJSI(ptr)
                })
                true
            } catch (_: Throwable) {
                false
            }

            if (!usedRuntimeExecutor) {
                val runtimePtr = getJSRuntimePointer()
                initializeJSI(runtimePtr)
            }
        }
    }

    // NOTE: uses the (deprecated) JavaScriptContextHolder for simplicity.
    // If you’re on RN 0.75+, prefer using RuntimeExecutor to schedule onto the JS thread.
    private fun getJSRuntimePointer(): Long {
        val holder = reactContext.javaClass
            .getMethod("getJavaScriptContextHolder")
            .invoke(reactContext)
        val getPtr = holder.javaClass.getMethod("get")
        @Suppress("UNCHECKED_CAST")
        return getPtr.invoke(holder) as Long
    }

    /** ---- Public API ---- */

    suspend fun callJSAndResolve(fnName: String, input: String): Map<String, Any?> =
        suspendCancellableCoroutine { cont ->
            runOnJsThreadCompat {
                nativeCallJS(fnName, input, object : PromiseCallback {
                    override fun onResolve(result: Map<String, Any?>) {
                        cont.resume(result)
                    }

                    override fun onReject(error: String) {
                        cont.resumeWithException(RuntimeException(error))
                    }
                })
            }
        }

    /** Prefer RuntimeExecutor / JSCallInvoker on RN >= 0.75, fallback to legacy JS queue */
    private fun runOnJsThreadCompat(block: () -> Unit) {
        try {
            // Try JSCallInvoker first
            val getHolder = reactContext.javaClass.getMethod("getJSCallInvokerHolder")
            val holder = getHolder.invoke(reactContext)
            val holderClass = holder.javaClass
            val getInvoker = holderClass.getMethod("get")
            val invoker = getInvoker.invoke(holder)
            val invokerClass = invoker.javaClass
            val invokeAsync = invokerClass.getMethod("invokeAsync", Runnable::class.java)
            invokeAsync.invoke(invoker, Runnable { block() })
            return
        } catch (_: Throwable) {
            // ignore and fallback
        }

        try {
            reactContext.runOnJSQueueThread { block() }
        } catch (_: Throwable) {
            // Last resort, run inline (should not happen)
            block()
        }
    }



    interface PromiseCallback {
        fun onResolve(result: Map<String, Any?>)
        fun onReject(error: String)
    }
}