package com.intervaltimer

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var editTextSets: EditText
    private lateinit var editTextWorkTimeMinutes: EditText
    private lateinit var editTextWorkTimeSeconds: EditText
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
    private lateinit var buttonSaveConfigIcon: ImageButton
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
        editTextWorkTimeMinutes = findViewById(R.id.editTextWorkTimeMinutes)
        editTextWorkTimeSeconds = findViewById(R.id.editTextWorkTimeSeconds)
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
        buttonSaveConfigIcon = findViewById(R.id.buttonSaveConfigIcon)
        recyclerViewSavedConfigs = findViewById(R.id.recyclerViewSavedConfigs)

        sharedPreferences = getSharedPreferences("TimerConfigs", Context.MODE_PRIVATE)

        buttonStart.setOnClickListener { startTimer() }
        buttonPause.setOnClickListener { pauseTimer() }
        buttonRestart.setOnClickListener { restartTimer() }
        buttonStop.setOnClickListener { stopTimer() }
        buttonSaveConfig.setOnClickListener { saveConfig() }
        buttonSaveConfigIcon.setOnClickListener { toggleSaveConfigVisibility() }

        // Initial button visibility
        buttonStart.visibility = View.VISIBLE
        buttonPause.visibility = View.GONE
        buttonRestart.visibility = View.GONE
        buttonStop.visibility = View.GONE
        editTextConfigName.visibility = View.GONE
        buttonSaveConfig.visibility = View.GONE

        buttonMinusSets.setOnClickListener { decrementValue(editTextSets) }
        buttonPlusSets.setOnClickListener { incrementValue(editTextSets) }
        buttonMinusWorkTime.setOnClickListener { decrementWorkTime() }
        buttonPlusWorkTime.setOnClickListener { incrementWorkTime() }
        buttonMinusRestTime.setOnClickListener { decrementValue(editTextRestTime) }
        buttonPlusRestTime.setOnClickListener { incrementValue(editTextRestTime) }

        // Set initial values
        editTextSets.setText("5")
        editTextWorkTimeMinutes.setText("0")
        editTextWorkTimeSeconds.setText("20")
        editTextRestTime.setText("10")
        
        // Add input validation for seconds field
        editTextWorkTimeSeconds.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndAdjustSeconds()
            }
        }

        // Setup RecyclerView
        configAdapter = ConfigAdapter(savedConfigs, {
            // Handle item click: load config into EditTexts
            editTextSets.setText(it.sets.toString())
            setWorkTimeFromSeconds(it.workTime)
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
        editTextWorkTimeMinutes.isEnabled = enable
        editTextWorkTimeSeconds.isEnabled = enable
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
        val workTime = getWorkTimeInSeconds()
        val restTime = editTextRestTime.text.toString().toLongOrNull()

        if (sets == null || workTime <= 0 || restTime == null) {
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
        editTextConfigName.visibility = View.GONE
        buttonSaveConfig.visibility = View.GONE
    }

    private fun toggleSaveConfigVisibility() {
        if (editTextConfigName.visibility == View.VISIBLE) {
            editTextConfigName.visibility = View.GONE
            buttonSaveConfig.visibility = View.GONE
            // Hide keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editTextConfigName.windowToken, 0)
        } else {
            editTextConfigName.visibility = View.VISIBLE
            buttonSaveConfig.visibility = View.VISIBLE
            editTextConfigName.requestFocus()
            // Show keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextConfigName, InputMethodManager.SHOW_IMPLICIT)
        }
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
    
    private fun decrementWorkTime() {
        val currentMinutes = editTextWorkTimeMinutes.text.toString().toIntOrNull() ?: 0
        val currentSeconds = editTextWorkTimeSeconds.text.toString().toIntOrNull() ?: 0
        
        var totalSeconds = currentMinutes * 60 + currentSeconds
        if (totalSeconds > 0) {
            totalSeconds--
            val newMinutes = totalSeconds / 60
            val newSeconds = totalSeconds % 60
            editTextWorkTimeMinutes.setText(newMinutes.toString())
            editTextWorkTimeSeconds.setText(newSeconds.toString())
        }
    }
    
    private fun incrementWorkTime() {
        val currentMinutes = editTextWorkTimeMinutes.text.toString().toIntOrNull() ?: 0
        val currentSeconds = editTextWorkTimeSeconds.text.toString().toIntOrNull() ?: 0
        
        var totalSeconds = currentMinutes * 60 + currentSeconds
        totalSeconds++
        val newMinutes = totalSeconds / 60
        val newSeconds = totalSeconds % 60
        editTextWorkTimeMinutes.setText(newMinutes.toString())
        editTextWorkTimeSeconds.setText(newSeconds.toString())
    }
    
    private fun getWorkTimeInSeconds(): Long {
        val minutes = editTextWorkTimeMinutes.text.toString().toIntOrNull() ?: 0
        val seconds = editTextWorkTimeSeconds.text.toString().toIntOrNull() ?: 0
        return (minutes * 60 + seconds).toLong()
    }
    
    private fun setWorkTimeFromSeconds(totalSeconds: Long) {
        val minutes = (totalSeconds / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        editTextWorkTimeMinutes.setText(minutes.toString())
        editTextWorkTimeSeconds.setText(seconds.toString())
    }
    
    private fun validateAndAdjustSeconds() {
        val seconds = editTextWorkTimeSeconds.text.toString().toIntOrNull() ?: 0
        if (seconds >= 60) {
            val minutes = editTextWorkTimeMinutes.text.toString().toIntOrNull() ?: 0
            val extraMinutes = seconds / 60
            val remainingSeconds = seconds % 60
            editTextWorkTimeMinutes.setText((minutes + extraMinutes).toString())
            editTextWorkTimeSeconds.setText(remainingSeconds.toString())
        }
    }

    private fun startTimer() {
        val setsStr = editTextSets.text.toString()
        val workTimeMinutesStr = editTextWorkTimeMinutes.text.toString()
        val workTimeSecondsStr = editTextWorkTimeSeconds.text.toString()
        val restTimeStr = editTextRestTime.text.toString()

        if (setsStr.isEmpty() || workTimeMinutesStr.isEmpty() || workTimeSecondsStr.isEmpty() || restTimeStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        totalSets = setsStr.toInt()
        workTimeSeconds = getWorkTimeInSeconds()
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
        
        // Hide save config icon when timer is active
        buttonSaveConfigIcon.visibility = View.GONE
        // Also hide save input fields if they're visible
        editTextConfigName.visibility = View.GONE
        buttonSaveConfig.visibility = View.GONE

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
        
        // Show save config icon when timer is stopped
        buttonSaveConfigIcon.visibility = View.VISIBLE

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
                // Only update UI if activity is still valid
                if (!isDestroyed && !isFinishing) {
                    textViewTimer.text = String.format("00:%02d", seconds)
                }
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
                
                // Only update UI if activity is still valid
                if (!isDestroyed && !isFinishing) {
                    updateTimerText(seconds)
                }

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
            
            // Show save config icon when timer finishes
            buttonSaveConfigIcon.visibility = View.VISIBLE
            
            enableInputFields(true)
            stopService(Intent(this@MainActivity, SoundService::class.java)) // Stop the service completely
        }
    }

    private fun updateTimerText(seconds: Long) {
        // Check if activity is still valid before updating UI
        if (!isDestroyed && !isFinishing) {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            textViewTimer.text = String.format("%02d:%02d", minutes, remainingSeconds)
        }
    }

    override fun onPause() {
        super.onPause()
        // If the timer is running, save its state
        if (!isPaused && countDownTimer != null) {
            isPaused = true
            // Consider starting the service if needed
            startService(Intent(this, SoundService::class.java).apply { action = SoundService.ACTION_START_FOREGROUND })
        }
    }

    override fun onResume() {
        super.onResume()
        // Restore the timer state if needed
        if (isPaused && timeRemainingMillis > 0) {
            if (isWorkPeriod) {
                startPeriod(timeRemainingMillis, "Work")
            } else {
                startPeriod(timeRemainingMillis, "Rest")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        startService(Intent(this, SoundService::class.java).apply { action = SoundService.ACTION_STOP_FOREGROUND })
        stopService(Intent(this@MainActivity, SoundService::class.java)) // Stop the service completely
    }
}