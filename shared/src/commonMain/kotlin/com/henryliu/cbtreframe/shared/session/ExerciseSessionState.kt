package com.henryliu.cbtreframe.shared.session

/**
 * Represents the ongoing state of a breathing exercise session.
 */
data class ExerciseSessionState(
    val phase: ExercisePhase = ExercisePhase.REST,
    val progress: Float = 0f,
    val phaseDuration: Int = 0,
    val remainingTime: Int = 0,
    val totalCycleProgress: Float = 0f,
    val isPaused: Boolean = true,
    val cycle: Int = 1
)
