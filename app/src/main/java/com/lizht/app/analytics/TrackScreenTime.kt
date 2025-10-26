package com.lizht.app.analytics

import android.os.Bundle
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * Logs a "screen_time" event with { screen_name, duration_ms } whenever the
 * Composable's lifecycle goes to background (ON_PAUSE/ON_STOP).
 */
@Composable
fun TrackScreenTime(screenName: String) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var startMs by remember(screenName) { mutableStateOf<Long?>(null) }

    DisposableEffect(lifecycleOwner, screenName) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    startMs = System.currentTimeMillis()
                }
                // Record when leaving the screen
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    val duration = (System.currentTimeMillis() - (startMs ?: 0L)).coerceAtLeast(0L)
                    val bundle = Bundle().apply {
                        putString("screen_name", screenName)
                        putLong("duration_ms", duration)
                    }
                    Firebase.analytics.logEvent("screen_time", bundle)
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}
