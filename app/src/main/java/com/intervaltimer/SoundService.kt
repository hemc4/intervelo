package com.intervaltimer

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

class SoundService : Service() {

    private var beepMediaPlayer: MediaPlayer? = null
    private var periodMediaPlayer: MediaPlayer? = null

    companion object {
        const val ACTION_PLAY_BEEP = "com.intervaltimer.ACTION_PLAY_BEEP"
        const val ACTION_STOP_BEEP = "com.intervaltimer.ACTION_STOP_BEEP"
        const val ACTION_PLAY_WORK_SOUND = "com.intervaltimer.ACTION_PLAY_WORK_SOUND"
        const val ACTION_PLAY_REST_SOUND = "com.intervaltimer.ACTION_PLAY_REST_SOUND"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_BEEP -> startBeepSound()
            ACTION_STOP_BEEP -> stopBeepSound()
            ACTION_PLAY_WORK_SOUND -> playPeriodSound(R.raw.work)
            ACTION_PLAY_REST_SOUND -> playPeriodSound(R.raw.rest)
        }
        return START_NOT_STICKY
    }

    private fun playPeriodSound(resId: Int) {
        periodMediaPlayer?.release()
        periodMediaPlayer = MediaPlayer.create(this, resId)
        periodMediaPlayer?.start()
    }

    private fun startBeepSound() {
        stopBeepSound() // Ensure any previous beep sound is stopped and released
        beepMediaPlayer = MediaPlayer.create(this, R.raw.beep)
        beepMediaPlayer?.isLooping = true
        beepMediaPlayer?.setVolume(1.0f, 1.0f)
        beepMediaPlayer?.start()
    }

    private fun stopBeepSound() {
        beepMediaPlayer?.stop()
        beepMediaPlayer?.release()
        beepMediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBeepSound()
        periodMediaPlayer?.release()
        periodMediaPlayer = null
    }
}