package com.intervaltimer

data class TimerConfig(
    val name: String,
    val sets: Int,
    val workTime: Long,
    val restTime: Long
)