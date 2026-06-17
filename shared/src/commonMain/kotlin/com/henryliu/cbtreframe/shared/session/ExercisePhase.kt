package com.henryliu.cbtreframe.shared.session

/**
 * Represents a discrete phase within a breathing exercise cycle.
 *
 * Each phase has a [displayName] for UI rendering and an [isActive] flag
 * indicating whether the user is performing an action (inhale/exhale)
 * versus resting (hold).
 */
enum class ExercisePhase(
    val displayName: String,
    val isActive: Boolean,
) {
    INHALE("吸气", isActive = true),
    HOLD("屏息", isActive = false),
    EXHALE("呼气", isActive = true),
    REST("休息", isActive = false);

    /** Label suitable for accessibility / VoiceOver announcements. */
    val accessibilityLabel: String
        get() = displayName
}
