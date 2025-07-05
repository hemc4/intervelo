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
import android.view.View
import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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

    private lateinit var buttonPause: Button
    private lateinit var buttonRestart: Button
    private lateinit var buttonStop: Button

    private lateinit var editTextConfigName: EditText
    private lateinit var buttonSaveConfig: Button
    private lateinit var recyclerViewSavedConfigs: RecyclerView

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var configAdapter: ConfigAdapter
    private val savedConfigs = mutableListOf<TimerConfig>()

    private var countDownTimer: CountDownTimer? = null
    private var currentSet = 0
    private var totalSets = 0
    private var workTimeSeconds = 0L
    private var restTimeSeconds = 0L
    private var isWorkPeriod = true
    private var timeRemainingMillis = 0L // To store remaining time when paused
    private var isPaused = false // To track pause state

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

        buttonPause = findViewById(R.id.buttonPause)
        buttonRestart = findViewById(R.id.buttonRestart)
        buttonStop = findViewById(R.id.buttonStop)

        editTextConfigName = findViewById(R.id.editTextConfigName)
        buttonSaveConfig = findViewById(R.id.buttonSaveConfig)
        recyclerViewSavedConfigs = findViewById(R.id.recyclerViewSavedConfigs)

        sharedPreferences = getSharedPreferences("TimerConfigs", Context.MODE_PRIVATE)

        buttonStart.setOnClickListener { startTimer() }
        buttonPause.setOnClickListener { pauseTimer() }
        buttonRestart.setOnClickListener { restartTimer() }
        buttonStop.setOnClickListener { stopTimer() }
        buttonSaveConfig.setOnClickListener { saveConfig() }

        // Initial button visibility
        buttonStart.visibility = View.VISIBLE
        buttonPause.visibility = View.GONE
        buttonRestart.visibility = View.GONE
        buttonStop.visibility = View.GONE

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

        // Setup RecyclerView
        configAdapter = ConfigAdapter(savedConfigs, {
            // Handle item click: load config into EditTexts
            editTextSets.setText(it.sets.toString())
            editTextWorkTime.setText(it.workTime.toString())
            editTextRestTime.setText(it.restTime.toString())
            Toast.makeText(this, "Loaded: ${it.name}", Toast.LENGTH_SHORT).show()
        }, {
            // Handle delete click
            deleteConfig(it)
        })
        recyclerViewSavedConfigs.layoutManager = LinearLayoutManager(this)
        recyclerViewSavedConfigs.adapter = configAdapter

        loadConfigs() // Load saved configs on startup
    }

    private fun deleteConfig(config: TimerConfig) {
        savedConfigs.remove(config)
        configAdapter.notifyDataSetChanged()
        saveConfigsToPrefs()
        Toast.makeText(this, "Deleted: ${config.name}", Toast.LENGTH_SHORT).show()
    }

    private fun saveConfigsToPrefs() {
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(savedConfigs)
        editor.putString("saved_timer_configs", json)
        editor.apply()
    }

    private fun enableInputFields(enable: Boolean) {
        editTextSets.isEnabled = enable
        editTextWorkTime.isEnabled = enable
        editTextRestTime.isEnabled = enable
        buttonMinusSets.isEnabled = enable
        buttonPlusSets.isEnabled = enable
        buttonMinusWorkTime.isEnabled = enable
        buttonPlusWorkTime.isEnabled = enable
        buttonMinusRestTime.isEnabled = enable
        buttonPlusRestTime.isEnabled = enable
        editTextConfigName.isEnabled = enable // Enable/disable config name input
        buttonSaveConfig.isEnabled = enable // Enable/disable save button
    }

    private fun saveConfig() {
        val configName = editTextConfigName.text.toString().trim()
        if (configName.isEmpty()) {
            Toast.makeText(this, "Please enter a configuration name", Toast.LENGTH_SHORT).show()
            return
        }

        val sets = editTextSets.text.toString().toIntOrNull()
        val workTime = editTextWorkTime.text.toString().toLongOrNull()
        val restTime = editTextRestTime.text.toString().toLongOrNull()

        if (sets == null || workTime == null || restTime == null) {
            Toast.makeText(this, "Please enter valid timer values", Toast.LENGTH_SHORT).show()
            return
        }

        val newConfig = TimerConfig(configName, sets, workTime, restTime)

        // Check if a config with the same name already exists and replace it
        val existingIndex = savedConfigs.indexOfFirst { it.name == configName }
        if (existingIndex != -1) {
            savedConfigs[existingIndex] = newConfig
            Toast.makeText(this, "Configuration \"$configName\" updated!", Toast.LENGTH_SHORT).show()
        } else {
            savedConfigs.add(newConfig)
            Toast.makeText(this, "Configuration \"$configName\" saved!", Toast.LENGTH_SHORT).show()
        }

        // Sort by name for consistent display
        savedConfigs.sortBy { it.name }
        configAdapter.notifyDataSetChanged()

        // Save to SharedPreferences
        saveConfigsToPrefs()

        editTextConfigName.text.clear()
    }

    private fun loadConfigs() {
        val gson = Gson()
        val json = sharedPreferences.getString("saved_timer_configs", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<TimerConfig>>() {}.type
            val loadedList: MutableList<TimerConfig> = gson.fromJson(json, type)
            savedConfigs.clear()
            savedConfigs.addAll(loadedList)
            configAdapter.notifyDataSetChanged()
        }
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

        // Hide Start button, show Pause and Stop
        buttonStart.visibility = View.GONE
        buttonPause.visibility = View.VISIBLE
        buttonStop.visibility = View.VISIBLE
        buttonRestart.visibility = View.GONE
        buttonPause.text = "Pause"

        enableInputFields(false)

        currentSet = 1
        isWorkPeriod = true
        isPaused = false

        // Start initial 3-second beep before the first work period
        startInitialOverallBeep()
    }

    private fun pauseTimer() {
        if (isPaused) { // Currently paused, so resume
            isPaused = false
            buttonPause.text = "Pause"
            buttonRestart.visibility = View.GONE // Hide restart when resuming
            // Resume the timer from timeRemainingMillis
            if (isWorkPeriod) {
                startPeriod(timeRemainingMillis, "Work")
            } else {
                startPeriod(timeRemainingMillis, "Rest")
            }
        } else { // Currently running, so pause
            countDownTimer?.cancel()
            startService(Intent(this, SoundService::class.java).apply { action = SoundService.ACTION_STOP_BEEP })
            isPaused = true
            buttonPause.text = "Resume"
            buttonRestart.visibility = View.VISIBLE // Show restart when paused
        }
    }

    private fun restartTimer() {
        stopTimer() // Reset everything
        // Visibility handled by stopTimer()
        textViewStatus.text = "Ready"
        updateTimerText(0)
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        startService(Intent(this, SoundService::class.java).apply { action = SoundService.ACTION_STOP_BEEP })
        stopService(Intent(this, SoundService::class.java)) // Stop the service completely
        isPaused = false
        timeRemainingMillis = 0L
        currentSet = 0
        totalSets = 0
        workTimeSeconds = 0L
        restTimeSeconds = 0L
        isWorkPeriod = true

        // Reset UI and enable input fields
        textViewStatus.text = "Stopped"
        updateTimerText(0)

        // Show Start button, hide Pause, Restart, Stop
        buttonStart.visibility = View.VISIBLE
        buttonPause.visibility = View.GONE
        buttonRestart.visibility = View.GONE
        buttonStop.visibility = View.GONE
        buttonPause.text = "Pause"

        enableInputFields(true)
    }

    private fun startInitialOverallBeep() {
        val initialBeepDuration = 3000L // 3 seconds
        textViewTimer.text = "00:03" // Display 3 seconds for the initial beep
        textViewStatus.text = "Get Ready!"
        startService(Intent(this, SoundService::class.java).apply { action = SoundService.ACTION_PLAY_BEEP })

        countDownTimer = object : CountDownTimer(initialBeepDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                textViewTimer.text = String.format("00:%02d", seconds)
            }

            override fun onFinish() {
                startService(Intent(this@MainActivity, SoundService::class.java).apply { action = SoundService.ACTION_STOP_BEEP })
                startPeriod(workTimeSeconds * 1000, "Work")
            }
        }.start()
    }

    private fun startPeriod(durationMillis: Long, periodType: String) {
        countDownTimer?.cancel()
        startService(Intent(this@MainActivity, SoundService::class.java).apply { action = SoundService.ACTION_STOP_BEEP })
        textViewStatus.text = "Set $currentSet: $periodType"

        // No period sound played at the beginning of work/rest periods anymore
        timeRemainingMillis = durationMillis // Store total duration for this period
        startMainTimer(timeRemainingMillis)
    }

    private fun startMainTimer(durationMillis: Long) {
        countDownTimer?.cancel() // Cancel any previous timers
        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMillis = millisUntilFinished // Update remaining time
                val seconds = millisUntilFinished / 1000
                updateTimerText(seconds)

                if (seconds == 3L) { // Start beep exactly at 3 seconds
                    startService(Intent(this@MainActivity, SoundService::class.java).apply { action = SoundService.ACTION_PLAY_BEEP })
                    // Play work/rest sound when 3 seconds remaining
                    if (isWorkPeriod) {
                        startService(Intent(this@MainActivity, SoundService::class.java).apply { action = SoundService.ACTION_PLAY_WORK_SOUND })
                    } else {
                        startService(Intent(this@MainActivity, SoundService::class.java).apply { action = SoundService.ACTION_PLAY_REST_SOUND })
                    }
                } else if (seconds == 0L) { // Stop beep exactly at 0 seconds
                    startService(Intent(this@MainActivity, SoundService::class.java).apply { action = SoundService.ACTION_STOP_BEEP })
                }
            }

            override fun onFinish() {
                startService(Intent(this@MainActivity, SoundService::class.java).apply { action = SoundService.ACTION_STOP_BEEP })
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
            // Show Start button, hide Pause, Restart, Stop
            buttonStart.visibility = View.VISIBLE
            buttonPause.visibility = View.GONE
            buttonRestart.visibility = View.GONE
            buttonStop.visibility = View.GONE
            buttonPause.text = "Pause"
            enableInputFields(true)
            stopService(Intent(this@MainActivity, SoundService::class.java)) // Stop the service completely
        }
    }

    private fun updateTimerText(seconds: Long) {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        textViewTimer.text = String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        stopService(Intent(this@MainActivity, SoundService::class.java)) // Stop the service completely
    }
}