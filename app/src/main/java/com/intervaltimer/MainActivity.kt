package com.intervaltimer

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.os.Handler
import android.os.Looper

class MainActivity : AppCompatActivity() {

    private lateinit var editTextSets: EditText
    private lateinit var editTextWorkTime: EditText
    private lateinit var editTextRestTime: EditText
    private lateinit var buttonStart: Button
    private lateinit var textViewTimer: TextView
    private lateinit var textViewStatus: TextView

    private lateinit var buttonMinusSets: Button
    private lateinit var buttonPlusSets: Button
    private lateinit var buttonMinusWorkTime: Button
    private lateinit var buttonPlusWorkTime: Button
    private lateinit var buttonMinusRestTime: Button
    private lateinit var buttonPlusRestTime: Button

    private var countDownTimer: CountDownTimer? = null
    private var currentSet = 0
    private var totalSets = 0
    private var workTimeSeconds = 0L
    private var restTimeSeconds = 0L
    private var isWorkPeriod = true

    private var periodMediaPlayer: MediaPlayer? = null // For work/rest sounds
    private var beepMediaPlayer: MediaPlayer? = null // For continuous beep sound

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextSets = findViewById(R.id.editTextSets)
        editTextWorkTime = findViewById(R.id.editTextWorkTime)
        editTextRestTime = findViewById(R.id.editTextRestTime)
        buttonStart = findViewById(R.id.buttonStart)
        textViewTimer = findViewById(R.id.textViewTimer)
        textViewStatus = findViewById(R.id.textViewStatus)

        buttonMinusSets = findViewById(R.id.buttonMinusSets)
        buttonPlusSets = findViewById(R.id.buttonPlusSets)
        buttonMinusWorkTime = findViewById(R.id.buttonMinusWorkTime)
        buttonPlusWorkTime = findViewById(R.id.buttonPlusWorkTime)
        buttonMinusRestTime = findViewById(R.id.buttonMinusRestTime)
        buttonPlusRestTime = findViewById(R.id.buttonPlusRestTime)

        buttonStart.setOnClickListener { startTimer() }

        buttonMinusSets.setOnClickListener { decrementValue(editTextSets) }
        buttonPlusSets.setOnClickListener { incrementValue(editTextSets) }
        buttonMinusWorkTime.setOnClickListener { decrementValue(editTextWorkTime) }
        buttonPlusWorkTime.setOnClickListener { incrementValue(editTextWorkTime) }
        buttonMinusRestTime.setOnClickListener { decrementValue(editTextRestTime) }
        buttonPlusRestTime.setOnClickListener { incrementValue(editTextRestTime) }

        // Set initial values
        editTextSets.setText("5")
        editTextWorkTime.setText("20")
        editTextRestTime.setText("10")
    }

    private fun decrementValue(editText: EditText) {
        var value = editText.text.toString().toIntOrNull() ?: 0
        if (value > 0) {
            value--
            editText.setText(value.toString())
        }
    }

    private fun incrementValue(editText: EditText) {
        var value = editText.text.toString().toIntOrNull() ?: 0
        value++
        editText.setText(value.toString())
    }

    private fun startTimer() {
        val setsStr = editTextSets.text.toString()
        val workTimeStr = editTextWorkTime.text.toString()
        val restTimeStr = editTextRestTime.text.toString()

        if (setsStr.isEmpty() || workTimeStr.isEmpty() || restTimeStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        totalSets = setsStr.toInt()
        workTimeSeconds = workTimeStr.toLong()
        restTimeSeconds = restTimeStr.toLong()

        if (totalSets <= 0 || workTimeSeconds <= 0 || restTimeSeconds < 0) {
            Toast.makeText(this, "Please enter valid positive numbers", Toast.LENGTH_SHORT).show()
            return
        }

        currentSet = 1
        isWorkPeriod = true

        // Start initial 5-second beep before the first work period
        startInitialOverallBeep()
    }

    private fun startInitialOverallBeep() {
        val initialBeepDuration = 5000L // 5 seconds
        textViewTimer.text = "00:05" // Display 5 seconds for the initial beep
        textViewStatus.text = "Get Ready!"
        startBeepSound()

        countDownTimer = object : CountDownTimer(initialBeepDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                textViewTimer.text = String.format("00:%02d", seconds)
            }

            override fun onFinish() {
                stopBeepSound()
                startPeriod(workTimeSeconds * 1000, "Work")
            }
        }.start()
    }

    private fun startPeriod(durationMillis: Long, periodType: String) {
        countDownTimer?.cancel()
        stopBeepSound()
        textViewStatus.text = "Set $currentSet: $periodType"

        if (periodType == "Rest") {
            playPeriodSound(R.raw.rest)
        }
        startMainTimer(durationMillis)
    }

    private fun startMainTimer(durationMillis: Long) {
        countDownTimer?.cancel() // Cancel any previous timers
        if (isWorkPeriod) {
            playPeriodSound(R.raw.work) // Play work sound when work period actually starts
        }
        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                updateTimerText(seconds)

                if (seconds == 5L) { // Start beep exactly at 5 seconds
                    startBeepSound()
                } else if (seconds == 0L) { // Stop beep exactly at 0 seconds
                    stopBeepSound()
                }
            }

            override fun onFinish() {
                stopBeepSound()
                if (isWorkPeriod) {
                    if (restTimeSeconds > 0) {
                        isWorkPeriod = false
                        startPeriod(restTimeSeconds * 1000, "Rest")
                    } else {
                        // If rest time is 0, directly go to next set or finish
                        moveToNextSetOrFinish()
                    }
                } else {
                    isWorkPeriod = true
                    moveToNextSetOrFinish()
                }
            }
        }.start()
    }

    private fun moveToNextSetOrFinish() {
        if (currentSet < totalSets) {
            currentSet++
            startPeriod(workTimeSeconds * 1000, "Work")
        } else {
            textViewStatus.text = "Finished!"
            updateTimerText(0)
            Toast.makeText(this, "Interval Timer Completed!", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateTimerText(seconds: Long) {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        textViewTimer.text = String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun playPeriodSound(resId: Int) {
        periodMediaPlayer?.release()
        periodMediaPlayer = MediaPlayer.create(this, resId)
        periodMediaPlayer?.start()
    }

    private fun startBeepSound() {
        // Ensure any previous beep sound is stopped and released before starting a new one
        stopBeepSound() // This will release and nullify beepMediaPlayer

        beepMediaPlayer = MediaPlayer.create(this, R.raw.beep)
        beepMediaPlayer?.isLooping = true
        beepMediaPlayer?.setVolume(1.0f, 1.0f) // Set volume to maximum
        beepMediaPlayer?.start()
    }

    private fun stopBeepSound() {
        beepMediaPlayer?.stop()
        beepMediaPlayer?.release()
        beepMediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        periodMediaPlayer?.release()
        periodMediaPlayer = null
        stopBeepSound()
    }
}