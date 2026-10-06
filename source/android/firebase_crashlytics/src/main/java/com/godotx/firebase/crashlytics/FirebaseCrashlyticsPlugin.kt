package com.godotx.firebase.crashlytics

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

/** Lỗi script của game (GDScript) ghi như ngoại lệ non-fatal; stack dựng từ backtrace của Godot. */
class GodotScriptError(message: String) : Exception(message)

class FirebaseCrashlyticsPlugin(godot: Godot) : GodotPlugin(godot) {

    private var crashlytics: FirebaseCrashlytics? = null

    companion object {
        val TAG = FirebaseCrashlyticsPlugin::class.java.simpleName
    }

    init {
        Log.v(TAG, "Firebase Crashlytics plugin loaded")
    }

    override fun getPluginName(): String {
        return "GodotxFirebaseCrashlytics"
    }

    override fun getPluginSignals(): Set<SignalInfo> {
        return setOf(
            SignalInfo("crashlytics_initialized",
                Boolean::class.javaObjectType
            ),
            SignalInfo("crashlytics_error",
                String::class.java
            )
        )
    }

    @UsedByGodot
    fun initialize() {
        try {
            crashlytics = FirebaseCrashlytics.getInstance()
            Log.d(TAG, "Firebase Crashlytics initialized")
            emitSignal("crashlytics_initialized", true)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Crashlytics init failed", e)
            emitSignal("crashlytics_initialized", false)
            emitSignal("crashlytics_error", e.message ?: "init_error")
        }
    }

    @UsedByGodot
    fun crash() {
        Log.d(TAG, "Forcing crash for testing...")
        val crash: String? = null
        crash!!.length
    }

    @UsedByGodot
    fun log_message(message: String) {
        val crashlyticsInstance = crashlytics
        if (crashlyticsInstance == null) {
            Log.e(TAG, "Firebase Crashlytics not initialized")
            emitSignal("crashlytics_error", "crashlytics_not_initialized")
            return
        }

        try {
            crashlyticsInstance.log(message)
            Log.d(TAG, "Logged message to Crashlytics: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log message", e)
            emitSignal("crashlytics_error", e.message ?: "log_error")
        }
    }

    @UsedByGodot
    fun set_user_id(user_id: String) {
        val crashlyticsInstance = crashlytics
        if (crashlyticsInstance == null) {
            Log.e(TAG, "Firebase Crashlytics not initialized")
            emitSignal("crashlytics_error", "crashlytics_not_initialized")
            return
        }

        try {
            crashlyticsInstance.setUserId(user_id)
            Log.d(TAG, "Set user ID: $user_id")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user ID", e)
            emitSignal("crashlytics_error", e.message ?: "set_user_error")
        }
    }

    @UsedByGodot
    fun set_custom_value_string(key: String, value: String) {
        val crashlyticsInstance = crashlytics
        if (crashlyticsInstance == null) {
            Log.e(TAG, "Firebase Crashlytics not initialized")
            emitSignal("crashlytics_error", "crashlytics_not_initialized")
            return
        }

        try {
            crashlyticsInstance.setCustomKey(key, value)
            Log.d(TAG, "Set custom value: $key = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom value", e)
            emitSignal("crashlytics_error", e.message ?: "set_custom_value_error")
        }
    }

    @UsedByGodot
    fun set_custom_value_int(key: String, value: Int) {
        val crashlyticsInstance = crashlytics
        if (crashlyticsInstance == null) {
            Log.e(TAG, "Firebase Crashlytics not initialized")
            emitSignal("crashlytics_error", "crashlytics_not_initialized")
            return
        }

        try {
            crashlyticsInstance.setCustomKey(key, value.toLong())
            Log.d(TAG, "Set custom value: $key = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom value", e)
            emitSignal("crashlytics_error", e.message ?: "set_custom_value_error")
        }
    }

    @UsedByGodot
    fun set_custom_value_bool(key: String, value: Boolean) {
        val crashlyticsInstance = crashlytics
        if (crashlyticsInstance == null) {
            Log.e(TAG, "Firebase Crashlytics not initialized")
            emitSignal("crashlytics_error", "crashlytics_not_initialized")
            return
        }

        try {
            crashlyticsInstance.setCustomKey(key, value)
            Log.d(TAG, "Set custom value: $key = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom value", e)
            emitSignal("crashlytics_error", e.message ?: "set_custom_value_error")
        }
    }

    @UsedByGodot
    fun set_custom_value_float(key: String, value: Float) {
        val crashlyticsInstance = crashlytics
        if (crashlyticsInstance == null) {
            Log.e(TAG, "Firebase Crashlytics not initialized")
            emitSignal("crashlytics_error", "crashlytics_not_initialized")
            return
        }

        try {
            crashlyticsInstance.setCustomKey(key, value.toDouble())
            Log.d(TAG, "Set custom value: $key = $value")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom value", e)
            emitSignal("crashlytics_error", e.message ?: "set_custom_value_error")
        }
    }

    @UsedByGodot
    fun set_crashlytics_collection_enabled(enabled: Boolean) {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)
    }

    /**
     * Ghi một lỗi non-fatal để Crashlytics nhóm theo stack. [stack] là các dòng `đường_dẫn|số_dòng|hàm`
     * (đường dẫn dạng res://src/a/b.gd), tối đa 30 dòng; thiếu thì Crashlytics nhóm theo thông điệp.
     */
    @UsedByGodot
    fun record_error(kind: String, message: String, stack: String) {
        val crashlyticsInstance = crashlytics
        if (crashlyticsInstance == null) {
            emitSignal("crashlytics_error", "crashlytics_not_initialized")
            return
        }
        try {
            val error = GodotScriptError("$kind: $message")
            val frames = stack.lineSequence().filter { it.isNotBlank() }.take(30).map { line ->
                val parts = line.split('|')
                val path = parts.getOrElse(0) { "unknown" }.removePrefix("res://")
                val declaring = path.removeSuffix(".gd").replace('/', '.')
                StackTraceElement(
                    declaring,
                    parts.getOrElse(2) { "?" },
                    path.substringAfterLast('/'),
                    parts.getOrNull(1)?.toIntOrNull() ?: -1
                )
            }.toList()
            if (frames.isNotEmpty()) {
                error.stackTrace = frames.toTypedArray()
            }
            crashlyticsInstance.recordException(error)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record error", e)
            emitSignal("crashlytics_error", e.message ?: "record_error_failed")
        }
    }
}
