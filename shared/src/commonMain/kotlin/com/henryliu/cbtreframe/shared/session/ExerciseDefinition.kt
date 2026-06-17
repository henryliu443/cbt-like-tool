package com.henryliu.cbtreframe.shared.session

/**
 * Defines a specific phase and its duration within a breathing exercise.
 */
data class PhaseDuration(
    val phase: ExercisePhase,
    val durationSeconds: Int
)

/**
 * Defines a breathing exercise sequence and cycle count.
 */
data class ExerciseDefinition(
    val id: String,
    val name: String,
    val sequence: List<PhaseDuration>,
    val totalCycles: Int
)
