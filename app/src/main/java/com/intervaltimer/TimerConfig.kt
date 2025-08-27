package com.intervelo

data class TimerConfig(
    val name: String,
    val sets: Int,
    val workTime: Long,
    val restTime: Long
)

data class MultiSetConfig(
    val name: String,
    val configs: List<TimerConfig>
)
