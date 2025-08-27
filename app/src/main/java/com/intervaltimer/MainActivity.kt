package com.intervelo

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
import android.app.AlertDialog
import androidx.core.content.ContextCompat
import android.widget.ScrollView

class MainActivity : AppCompatActivity() {

    private lateinit var editTextSets: EditText
    private lateinit var editTextWorkTimeMinutes: EditText
    private lateinit var editTextWorkTimeSeconds: EditText
    private lateinit var editTextRestTimeMinutes: EditText
    private lateinit var editTextRestTimeSeconds: EditText
    private lateinit var buttonStart: Button
    private lateinit var textViewTimer: TextView
    private lateinit var textViewStatus: TextView
    private lateinit var textViewConfigInfo: TextView
    private lateinit var textViewMultiSetRunning: TextView

    private lateinit var buttonMinusSets: Button
    private lateinit var buttonPlusSets: Button
    private lateinit var buttonMinusWorkTime: Button
    private lateinit var buttonPlusWorkTime: Button
    private lateinit var buttonMinusRestTime: Button
    private lateinit var buttonPlusRestTime: Button

    // Layout references for hiding/showing UI sections
    private lateinit var layoutSetsInput: android.widget.LinearLayout
    private lateinit var layoutWorkTimeInput: android.widget.LinearLayout
    private lateinit var layoutRestTimeInput: android.widget.LinearLayout
    private lateinit var textViewSetsLabel: TextView
    private lateinit var textViewWorkTimeLabel: TextView
    private lateinit var textViewRestTimeLabel: TextView
    private lateinit var textViewMultiSetLabel: TextView
    private lateinit var buttonAddMultiSetConfig: Button

    // Background reference for color changes
    private lateinit var scrollViewBackground: ScrollView

    private lateinit var buttonPause: Button
    private lateinit var buttonRestart: Button
    private lateinit var buttonStop: Button

    private lateinit var sharedPreferences: SharedPreferences

    // Multi-set configuration variables
    private lateinit var recyclerViewMultiSetConfigs: RecyclerView
    private lateinit var multiSetConfigAdapter: MultiSetConfigAdapter
    private val multiSetConfigs = mutableListOf<MultiSetConfig>()
    private val currentMultiSetQueue = mutableListOf<TimerConfig>()
    private var currentMultiSetIndex = 0
    private var isRunningMultiSet = false
    private var isFirstConfig = true  // Flag to track if this is the first config in a multi-set
    private var currentMultiSetName = ""  // Store the name of the running multi-set

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
        editTextRestTimeMinutes = findViewById(R.id.editTextRestTimeMinutes)
        editTextRestTimeSeconds = findViewById(R.id.editTextRestTimeSeconds)
        buttonStart = findViewById(R.id.buttonStart)
        textViewTimer = findViewById(R.id.textViewTimer)
        textViewStatus = findViewById(R.id.textViewStatus)
        textViewConfigInfo = findViewById(R.id.textViewConfigInfo)
        textViewMultiSetRunning = findViewById(R.id.textViewMultiSetRunning)

        // Get references to layout components for hiding/showing
        layoutSetsInput = findViewById(R.id.layoutSetsInput)
        layoutWorkTimeInput = findViewById(R.id.layoutWorkTimeInput)
        layoutRestTimeInput = findViewById(R.id.layoutRestTimeInput)
        textViewSetsLabel = findViewById(R.id.textViewSetsLabel)
        textViewWorkTimeLabel = findViewById(R.id.textViewWorkTimeLabel)
        textViewRestTimeLabel = findViewById(R.id.textViewRestTimeLabel)
        textViewMultiSetLabel = findViewById(R.id.textViewMultiSetLabel)
        buttonAddMultiSetConfig = findViewById(R.id.buttonAddMultiSetConfig)
        
        // Initialize background reference  
        scrollViewBackground = findViewById(R.id.scrollViewMain)

        buttonMinusSets = findViewById(R.id.buttonMinusSets)
        buttonPlusSets = findViewById(R.id.buttonPlusSets)
        buttonMinusWorkTime = findViewById(R.id.buttonMinusWorkTime)
        buttonPlusWorkTime = findViewById(R.id.buttonPlusWorkTime)
        buttonMinusRestTime = findViewById(R.id.buttonMinusRestTime)
        buttonPlusRestTime = findViewById(R.id.buttonPlusRestTime)

        buttonPause = findViewById(R.id.buttonPause)
        buttonRestart = findViewById(R.id.buttonRestart)
        buttonStop = findViewById(R.id.buttonStop)


        // Initialize multi-set configuration components
        recyclerViewMultiSetConfigs = findViewById(R.id.recyclerViewMultiSetConfigs)

        sharedPreferences = getSharedPreferences("TimerConfigs", Context.MODE_PRIVATE)

        buttonStart.setOnClickListener { startTimer() }
        buttonPause.setOnClickListener { pauseTimer() }
        buttonRestart.setOnClickListener { restartTimer() }
        buttonStop.setOnClickListener { stopTimer() }

        // Initial button visibility
        buttonStart.visibility = View.VISIBLE
        buttonPause.visibility = View.GONE
        buttonRestart.visibility = View.GONE
        buttonStop.visibility = View.GONE

        buttonMinusSets.setOnClickListener { decrementValue(editTextSets) }
        buttonPlusSets.setOnClickListener { incrementValue(editTextSets) }
        buttonMinusWorkTime.setOnClickListener { decrementWorkTime() }
        buttonPlusWorkTime.setOnClickListener { incrementWorkTime() }
        buttonMinusRestTime.setOnClickListener { decrementRestTime() }
        buttonPlusRestTime.setOnClickListener { incrementRestTime() }

        // Set initial values
        editTextSets.setText("5")
        editTextWorkTimeMinutes.setText("0")
        editTextWorkTimeSeconds.setText("20")
        editTextRestTimeMinutes.setText("0")
        editTextRestTimeSeconds.setText("10")
        
        // Add input validation for seconds fields
        editTextWorkTimeSeconds.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndAdjustWorkTimeSeconds()
            }
        }
        
        editTextRestTimeSeconds.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndAdjustRestTimeSeconds()
            }
        }


        // Setup RecyclerView for multi-set configs
        multiSetConfigAdapter = MultiSetConfigAdapter(multiSetConfigs, {
            // Handle item click: start multi-set configuration
            startMultiSetConfiguration(it)
        }, {
            // Handle delete click
            deleteMultiSetConfig(it)
        })
        recyclerViewMultiSetConfigs.layoutManager = LinearLayoutManager(this)
        recyclerViewMultiSetConfigs.adapter = multiSetConfigAdapter

        // Set click listener for add multi-set config button
        buttonAddMultiSetConfig.setOnClickListener { addCurrentConfigToMultiSet() }

        loadMultiSetConfigs() // Load saved multi-set configs on startup
    }


    private fun enableInputFields(enable: Boolean) {
        editTextSets.isEnabled = enable
        editTextWorkTimeMinutes.isEnabled = enable
        editTextWorkTimeSeconds.isEnabled = enable
        editTextRestTimeMinutes.isEnabled = enable
        editTextRestTimeSeconds.isEnabled = enable
        buttonMinusSets.isEnabled = enable
        buttonPlusSets.isEnabled = enable
        buttonMinusWorkTime.isEnabled = enable
        buttonPlusWorkTime.isEnabled = enable
        buttonMinusRestTime.isEnabled = enable
        buttonPlusRestTime.isEnabled = enable
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
    
    private fun validateAndAdjustWorkTimeSeconds() {
        val seconds = editTextWorkTimeSeconds.text.toString().toIntOrNull() ?: 0
        if (seconds >= 60) {
            val minutes = editTextWorkTimeMinutes.text.toString().toIntOrNull() ?: 0
            val extraMinutes = seconds / 60
            val remainingSeconds = seconds % 60
            editTextWorkTimeMinutes.setText((minutes + extraMinutes).toString())
            editTextWorkTimeSeconds.setText(remainingSeconds.toString())
        }
    }
    
    private fun decrementRestTime() {
        val currentMinutes = editTextRestTimeMinutes.text.toString().toIntOrNull() ?: 0
        val currentSeconds = editTextRestTimeSeconds.text.toString().toIntOrNull() ?: 0
        
        var totalSeconds = currentMinutes * 60 + currentSeconds
        if (totalSeconds > 0) {
            totalSeconds--
            val newMinutes = totalSeconds / 60
            val newSeconds = totalSeconds % 60
            editTextRestTimeMinutes.setText(newMinutes.toString())
            editTextRestTimeSeconds.setText(newSeconds.toString())
        }
    }
    
    private fun incrementRestTime() {
        val currentMinutes = editTextRestTimeMinutes.text.toString().toIntOrNull() ?: 0
        val currentSeconds = editTextRestTimeSeconds.text.toString().toIntOrNull() ?: 0
        
        var totalSeconds = currentMinutes * 60 + currentSeconds
        totalSeconds++
        val newMinutes = totalSeconds / 60
        val newSeconds = totalSeconds % 60
        editTextRestTimeMinutes.setText(newMinutes.toString())
        editTextRestTimeSeconds.setText(newSeconds.toString())
    }
    
    private fun getRestTimeInSeconds(): Long {
        val minutes = editTextRestTimeMinutes.text.toString().toIntOrNull() ?: 0
        val seconds = editTextRestTimeSeconds.text.toString().toIntOrNull() ?: 0
        return (minutes * 60 + seconds).toLong()
    }
    
    private fun setRestTimeFromSeconds(totalSeconds: Long) {
        val minutes = (totalSeconds / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        editTextRestTimeMinutes.setText(minutes.toString())
        editTextRestTimeSeconds.setText(seconds.toString())
    }
    
    private fun validateAndAdjustRestTimeSeconds() {
        val seconds = editTextRestTimeSeconds.text.toString().toIntOrNull() ?: 0
        if (seconds >= 60) {
            val minutes = editTextRestTimeMinutes.text.toString().toIntOrNull() ?: 0
            val extraMinutes = seconds / 60
            val remainingSeconds = seconds % 60
            editTextRestTimeMinutes.setText((minutes + extraMinutes).toString())
            editTextRestTimeSeconds.setText(remainingSeconds.toString())
        }
    }

    private fun startTimer() {
        val setsStr = editTextSets.text.toString()
        val workTimeMinutesStr = editTextWorkTimeMinutes.text.toString()
        val workTimeSecondsStr = editTextWorkTimeSeconds.text.toString()
        val restTimeMinutesStr = editTextRestTimeMinutes.text.toString()
        val restTimeSecondsStr = editTextRestTimeSeconds.text.toString()

        if (setsStr.isEmpty() || workTimeMinutesStr.isEmpty() || workTimeSecondsStr.isEmpty() || restTimeMinutesStr.isEmpty() || restTimeSecondsStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        totalSets = setsStr.toInt()
        workTimeSeconds = getWorkTimeInSeconds()
        restTimeSeconds = getRestTimeInSeconds()

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
        
        // Hide configuration sections and show multi-set info when running multi-set
        if (isRunningMultiSet) {
            hideConfigurationSections()
            showMultiSetRunningInfo()
        }

        currentSet = 1
        isWorkPeriod = true
        isPaused = false

        // Start initial 3-second beep before the first work period, but only for first config in multi-set
        if (!isRunningMultiSet || isFirstConfig) {
            startInitialOverallBeep()
        } else {
            // Skip the Get Ready section for subsequent configs in a multi-set
            startPeriod(workTimeSeconds * 1000, "Work")
        }
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
        
        // Reset multi-set state
        isRunningMultiSet = false
        currentMultiSetQueue.clear()
        currentMultiSetIndex = 0
        isFirstConfig = true  // Reset first config flag when stopping

        // Reset background color
        setDefaultBackgroundColor()

        // Reset UI and enable input fields
        textViewConfigInfo.visibility = View.GONE
        textViewMultiSetRunning.visibility = View.GONE
        textViewStatus.text = "Stopped"
        updateTimerText(0)

        // Show Start button, hide Pause, Restart, Stop
        buttonStart.visibility = View.VISIBLE
        buttonPause.visibility = View.GONE
        buttonRestart.visibility = View.GONE
        buttonStop.visibility = View.GONE
        buttonPause.text = "Pause"
        
        enableInputFields(true)
        
        // Show configuration sections when stopped
        showConfigurationSections()
    }

    private fun startInitialOverallBeep() {
        val initialBeepDuration = 3000L // 3 seconds
        textViewTimer.text = "00:03" // Display 3 seconds for the initial beep
        
        // Show different Get Ready text for multi-set vs single config
        if (isRunningMultiSet) {
            textViewConfigInfo.text = "Config ${workTimeSeconds}s/${restTimeSeconds}s"
            textViewConfigInfo.visibility = View.VISIBLE
            textViewStatus.text = "Get Ready!"
        } else {
            textViewConfigInfo.visibility = View.GONE
            textViewStatus.text = "Get Ready!"
        }
        
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
        
        // Change background color based on period type
        if (periodType == "Work") {
            setWorkBackgroundColor()
        } else {
            setRestBackgroundColor()
        }
        
        // Show different status text for multi-set vs single config
        if (isRunningMultiSet) {
            textViewConfigInfo.text = "Config ${workTimeSeconds}s/${restTimeSeconds}s"
            textViewConfigInfo.visibility = View.VISIBLE
            textViewStatus.text = "Set $currentSet: $periodType"
        } else {
            textViewConfigInfo.visibility = View.GONE
            textViewStatus.text = "Set $currentSet: $periodType"
        }

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
            // Current configuration is finished
            if (isRunningMultiSet) {
                moveToNextMultiSetConfigOrFinish()
            } else {
                // Reset background color when single timer finishes
                setDefaultBackgroundColor()
                
                textViewConfigInfo.visibility = View.GONE
                textViewMultiSetRunning.visibility = View.GONE
                textViewStatus.text = "Finished!"
                updateTimerText(0)
                Toast.makeText(this, "Intervelo  Completed!", Toast.LENGTH_LONG).show()
                // Show Start button, hide Pause, Restart, Stop
                buttonStart.visibility = View.VISIBLE
                buttonPause.visibility = View.GONE
                buttonRestart.visibility = View.GONE
                buttonStop.visibility = View.GONE
                buttonPause.text = "Pause"
                
                enableInputFields(true)
                
                // Show configuration sections when single timer finishes
                showConfigurationSections()
                
                stopService(Intent(this@MainActivity, SoundService::class.java)) // Stop the service completely
            }
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

    // Multi-set configuration methods
    private fun addCurrentConfigToMultiSet() {
        val sets = editTextSets.text.toString().toIntOrNull()
        val workTime = getWorkTimeInSeconds()
        val restTime = getRestTimeInSeconds()

        if (sets == null || workTime <= 0 || restTime < 0) {
            Toast.makeText(this, "Please enter valid timer values", Toast.LENGTH_SHORT).show()
            return
        }

        showAddToMultiSetDialog(TimerConfig("Config ${multiSetConfigs.size + 1}", sets, workTime, restTime))
    }

    private fun showAddToMultiSetDialog(config: TimerConfig) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add to Multi-Set Configuration")
        
        val editText = EditText(this)
        editText.hint = "Enter configuration name"
        builder.setView(editText)

        builder.setPositiveButton("Add to New") { _, _ ->
            val multiSetName = editText.text.toString().trim()
            if (multiSetName.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            
            val newMultiSetConfig = MultiSetConfig(multiSetName, listOf(config))
            multiSetConfigs.add(newMultiSetConfig)
            multiSetConfigAdapter.notifyDataSetChanged()
            saveMultiSetConfigs()
            Toast.makeText(this, "Created new multi-set: $multiSetName", Toast.LENGTH_SHORT).show()
        }
        
        builder.setNegativeButton("Add to Existing") { _, _ ->
            if (multiSetConfigs.isEmpty()) {
                Toast.makeText(this, "No existing multi-set configurations", Toast.LENGTH_SHORT).show()
                return@setNegativeButton
            }
            showSelectExistingMultiSetDialog(config)
        }
        
        builder.setNeutralButton("Cancel", null)
        builder.show()
    }

    private fun showSelectExistingMultiSetDialog(config: TimerConfig) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Multi-Set Configuration")
        
        val names = multiSetConfigs.map { it.name }.toTypedArray()
        builder.setItems(names) { _, which ->
            val selectedMultiSet = multiSetConfigs[which]
            val updatedConfigs = selectedMultiSet.configs.toMutableList()
            updatedConfigs.add(config)
            
            multiSetConfigs[which] = selectedMultiSet.copy(configs = updatedConfigs)
            multiSetConfigAdapter.notifyDataSetChanged()
            saveMultiSetConfigs()
            Toast.makeText(this, "Added to ${selectedMultiSet.name}", Toast.LENGTH_SHORT).show()
        }
        
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun startMultiSetConfiguration(multiSetConfig: MultiSetConfig) {
        if (multiSetConfig.configs.isEmpty()) {
            Toast.makeText(this, "Multi-set configuration is empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        currentMultiSetQueue.clear()
        currentMultiSetQueue.addAll(multiSetConfig.configs)
        currentMultiSetIndex = 0
        isRunningMultiSet = true
        isFirstConfig = true  // Set this as the first config in the multi-set
        currentMultiSetName = multiSetConfig.name  // Store the multi-set name
        
        // Load the first config
        val firstConfig = currentMultiSetQueue[0]
        editTextSets.setText(firstConfig.sets.toString())
        setWorkTimeFromSeconds(firstConfig.workTime)
        setRestTimeFromSeconds(firstConfig.restTime)
        
        Toast.makeText(this, "Starting multi-set: ${multiSetConfig.name}", Toast.LENGTH_SHORT).show()
        startTimer()
    }

    private fun deleteMultiSetConfig(config: MultiSetConfig) {
        multiSetConfigs.remove(config)
        multiSetConfigAdapter.notifyDataSetChanged()
        saveMultiSetConfigs()
        Toast.makeText(this, "Deleted multi-set: ${config.name}", Toast.LENGTH_SHORT).show()
    }

    private fun loadMultiSetConfigs() {
        val gson = Gson()
        val json = sharedPreferences.getString("saved_multiset_configs", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<MultiSetConfig>>() {}.type
            val loadedList: MutableList<MultiSetConfig> = gson.fromJson(json, type)
            multiSetConfigs.clear()
            multiSetConfigs.addAll(loadedList)
            multiSetConfigAdapter.notifyDataSetChanged()
        }
    }

    private fun saveMultiSetConfigs() {
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(multiSetConfigs)
        editor.putString("saved_multiset_configs", json)
        editor.apply()
    }

    private fun moveToNextMultiSetConfigOrFinish() {
        if (isRunningMultiSet && currentMultiSetIndex < currentMultiSetQueue.size - 1) {
            // Move to next config in the multi-set
            currentMultiSetIndex++
            val nextConfig = currentMultiSetQueue[currentMultiSetIndex]
            
            // Update the current configuration
            totalSets = nextConfig.sets
            workTimeSeconds = nextConfig.workTime
            restTimeSeconds = nextConfig.restTime
            currentSet = 1
            isWorkPeriod = true
            isFirstConfig = false  // This is not the first config anymore
            
            // Show which config we're moving to with description
            Toast.makeText(this, "Starting config ${currentMultiSetIndex + 1}/${currentMultiSetQueue.size}: ${nextConfig.workTime}s/${nextConfig.restTime}s", Toast.LENGTH_SHORT).show()
            
            // Update the multi-set running info display
            showMultiSetRunningInfo()
            
            // Start the next config with a brief pause, but skip the Get Ready section
            Handler(Looper.getMainLooper()).postDelayed({
                startTimer()
            }, 2000) // 2 second break between configs
        } else {
            // Finished all configs in multi-set
            isRunningMultiSet = false
            currentMultiSetQueue.clear()
            currentMultiSetIndex = 0
            
            // Reset background color when multi-set finishes
            setDefaultBackgroundColor()
            
            textViewConfigInfo.visibility = View.GONE
            textViewMultiSetRunning.visibility = View.GONE
            textViewStatus.text = "Multi-Set Finished!"
            updateTimerText(0)
            Toast.makeText(this, "Multi-Set Configuration Completed!", Toast.LENGTH_LONG).show()
            
            // Show Start button, hide Pause, Restart, Stop
            buttonStart.visibility = View.VISIBLE
            buttonPause.visibility = View.GONE
            buttonRestart.visibility = View.GONE
            buttonStop.visibility = View.GONE
            buttonPause.text = "Pause"
            
            enableInputFields(true)
            
            // Show configuration sections when multi-set finishes
            showConfigurationSections()
            
            stopService(Intent(this@MainActivity, SoundService::class.java))
        }
    }

    private fun hideConfigurationSections() {
        // Hide input configuration section
        textViewSetsLabel.visibility = View.GONE
        layoutSetsInput.visibility = View.GONE
        textViewWorkTimeLabel.visibility = View.GONE
        layoutWorkTimeInput.visibility = View.GONE
        textViewRestTimeLabel.visibility = View.GONE
        layoutRestTimeInput.visibility = View.GONE
        
        // Hide multi-set configuration section
        textViewMultiSetLabel.visibility = View.GONE
        buttonAddMultiSetConfig.visibility = View.GONE
        recyclerViewMultiSetConfigs.visibility = View.GONE
    }

    private fun showConfigurationSections() {
        // Show input configuration section
        textViewSetsLabel.visibility = View.VISIBLE
        layoutSetsInput.visibility = View.VISIBLE
        textViewWorkTimeLabel.visibility = View.VISIBLE
        layoutWorkTimeInput.visibility = View.VISIBLE
        textViewRestTimeLabel.visibility = View.VISIBLE
        layoutRestTimeInput.visibility = View.VISIBLE
        
        // Show multi-set configuration section
        textViewMultiSetLabel.visibility = View.VISIBLE
        buttonAddMultiSetConfig.visibility = View.VISIBLE
        recyclerViewMultiSetConfigs.visibility = View.VISIBLE
    }

    private fun showMultiSetRunningInfo() {
        if (isRunningMultiSet) {
            val totalConfigs = currentMultiSetQueue.size
            val currentConfigNum = currentMultiSetIndex + 1

            // Build details text showing each config in the sequence (same format as saved listing)
            val details = currentMultiSetQueue.joinToString(" → ") { subConfig ->
                "${subConfig.sets} sets (${subConfig.workTime}s/${subConfig.restTime}s)"
            }

            // Show multi-set name, full details sequence, and current progress
            val description = "$currentMultiSetName\n$details\nConfig $currentConfigNum of $totalConfigs"
            textViewMultiSetRunning.text = description
            textViewMultiSetRunning.visibility = View.VISIBLE
        }
    }

    // Background color management methods
    private fun setWorkBackgroundColor() {
        scrollViewBackground.setBackgroundColor(0xFFFFEB99.toInt()) // Darker yellow color
    }

    private fun setRestBackgroundColor() {
        scrollViewBackground.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
    }

    private fun setDefaultBackgroundColor() {
        scrollViewBackground.setBackgroundColor(0xFFE3F2FD.toInt()) // Original blue background
    }
}
