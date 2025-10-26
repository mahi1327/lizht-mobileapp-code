package com.lizht.app

import android.app.ActivityOptions
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class IntroActivity : ComponentActivity() {

    private var canExit = false
    private lateinit var player: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContentView(R.layout.activity_intro)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (canExit) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        val playerView = findViewById<PlayerView>(R.id.playerView)

        // ExoPlayer with decoder fallback enabled
        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)

        player = ExoPlayer.Builder(this, renderersFactory).build().also {
            playerView.player = it
            val uri = Uri.parse("android.resource://$packageName/${R.raw.intro}")
            val mediaItem = MediaItem.fromUri(uri)
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    canExit = true
                    goHome()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // If playback fails, skip video and go home
                goHome()
            }
        })

        // Fallback timeout (max wait 6 seconds)
        handler.postDelayed({
            if (!canExit) {
                goHome()
            }
        }, 6000)
    }

    private fun goHome() {
        if (!isFinishing) {
            val intent = Intent(this, MainActivity::class.java)
            val options: Bundle = ActivityOptions
                .makeCustomAnimation(this, 0, 0) // no enter/exit animation
                .toBundle()

            startActivity(intent, options)
            finish()
        }
    }


    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return if (!canExit) true else super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        player.release()
    }
}
