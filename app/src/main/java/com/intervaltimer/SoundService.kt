package com.intervaltimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class SoundService : Service() {

    private var beepMediaPlayer: MediaPlayer? = null
    private var periodMediaPlayer: MediaPlayer? = null

    companion object {
        const val ACTION_PLAY_BEEP = "com.intervaltimer.ACTION_PLAY_BEEP"
        const val ACTION_STOP_BEEP = "com.intervaltimer.ACTION_STOP_BEEP"
        const val ACTION_PLAY_WORK_SOUND = "com.intervaltimer.ACTION_PLAY_WORK_SOUND"
        const val ACTION_PLAY_REST_SOUND = "com.intervaltimer.ACTION_PLAY_REST_SOUND"
        const val ACTION_START_FOREGROUND = "com.intervaltimer.ACTION_START_FOREGROUND"
        const val ACTION_STOP_FOREGROUND = "com.intervaltimer.ACTION_STOP_FOREGROUND"
        
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "IntervalTimerChannel"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> startForegroundService()
            ACTION_STOP_FOREGROUND -> stopForegroundService()
            ACTION_PLAY_BEEP -> startBeepSound()
            ACTION_STOP_BEEP -> stopBeepSound()
            ACTION_PLAY_WORK_SOUND -> playPeriodSound(R.raw.work)
            ACTION_PLAY_REST_SOUND -> playPeriodSound(R.raw.rest)
        }
        return START_STICKY // Changed from START_NOT_STICKY to keep service alive
    }

    private fun playPeriodSound(resId: Int) {
        try {
            periodMediaPlayer?.release()
            periodMediaPlayer = null
            periodMediaPlayer = MediaPlayer.create(this, resId).apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                start()
            }
        } catch (e: Exception) {
            // Handle error gracefully - log or ignore
            periodMediaPlayer = null
        }
    }

    private fun startBeepSound() {
        try {
            stopBeepSound() // Ensure any previous beep sound is stopped and released
            beepMediaPlayer = MediaPlayer.create(this, R.raw.beep).apply {
                isLooping = true
                setVolume(1.0f, 1.0f)
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                start()
            }
        } catch (e: Exception) {
            // Handle error gracefully
            beepMediaPlayer = null
        }
    }

    private fun stopBeepSound() {
        try {
            beepMediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            // Handle gracefully - MediaPlayer might be in invalid state
        } finally {
            beepMediaPlayer = null
        }
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Interval Timer Service",
                NotificationManager.IMPORTANCE_LOW
            )
            serviceChannel.description = "Keeps interval timer running in background"
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
    
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Interval Timer")
            .setContentText("Timer is running")
            .setSmallIcon(R.drawable.ic_launcher_watch)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBeepSound()
        try {
            periodMediaPlayer?.release()
        } catch (e: Exception) {
            // Handle gracefully
        } finally {
            periodMediaPlayer = null
        }
    }
}