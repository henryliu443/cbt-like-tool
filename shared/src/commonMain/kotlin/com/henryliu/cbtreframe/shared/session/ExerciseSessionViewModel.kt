package com.henryliu.cbtreframe.shared.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExerciseSessionViewModel(
    private val definition: ExerciseDefinition,
    private val scope: CoroutineScope
) {
    constructor(definition: ExerciseDefinition) : this(definition, CoroutineScope(Dispatchers.Main + SupervisorJob()))
    private val _state = MutableStateFlow(ExerciseSessionState())
    val state: StateFlow<ExerciseSessionState> = _state.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null
    
    private var currentPhaseIndex = 0
    private var elapsedInPhaseMs = 0L
    private val updateIntervalMs = 50L

    init {
        reset()
    }

    fun start() {
        if (_state.value.isPaused) {
            _state.update { it.copy(isPaused = false) }
            startTimer()
        }
    }

    fun pause() {
        if (!_state.value.isPaused) {
            _state.update { it.copy(isPaused = true) }
            timerJob?.cancel()
            timerJob = null
        }
    }

    fun reset() {
        pause()
        currentPhaseIndex = 0
        elapsedInPhaseMs = 0L
        val initialPhase = definition.sequence.firstOrNull()
        _state.value = ExerciseSessionState(
            phase = initialPhase?.phase ?: ExercisePhase.REST,
            progress = 0f,
            phaseDuration = initialPhase?.durationSeconds ?: 0,
            remainingTime = initialPhase?.durationSeconds ?: 0,
            totalCycleProgress = 0f,
            isPaused = true,
            cycle = 1
        )
    }

    private fun startTimer() {
        if (definition.sequence.isEmpty()) return

        timerJob = scope.launch {
            while (true) {
                delay(updateIntervalMs)
                tick(updateIntervalMs)
            }
        }
    }

    private fun tick(deltaMs: Long) {
        val currentPhaseDurationMs = definition.sequence[currentPhaseIndex].durationSeconds * 1000L
        elapsedInPhaseMs += deltaMs

        if (elapsedInPhaseMs >= currentPhaseDurationMs) {
            elapsedInPhaseMs -= currentPhaseDurationMs
            currentPhaseIndex++

            if (currentPhaseIndex >= definition.sequence.size) {
                currentPhaseIndex = 0
                val nextCycle = _state.value.cycle + 1
                if (nextCycle > definition.totalCycles) {
                    // Exercise complete
                    pause()
                    _state.update { it.copy(
                        phase = ExercisePhase.REST,
                        progress = 0f,
                        phaseDuration = 0,
                        remainingTime = 0,
                        totalCycleProgress = 1f,
                        isPaused = true
                    )}
                    return
                } else {
                    _state.update { it.copy(cycle = nextCycle) }
                }
            }
        }

        updateStateFromTick()
    }

    private fun updateStateFromTick() {
        if (definition.sequence.isEmpty()) return
        
        val currentPhaseDef = definition.sequence[currentPhaseIndex]
        val currentPhaseDurationMs = currentPhaseDef.durationSeconds * 1000L
        
        val phaseProgress = if (currentPhaseDurationMs > 0) {
            (elapsedInPhaseMs.toFloat() / currentPhaseDurationMs).coerceIn(0f, 1f)
        } else {
            1f
        }

        val remainingMs = currentPhaseDurationMs - elapsedInPhaseMs
        val remainingSec = kotlin.math.ceil(remainingMs / 1000.0).toInt().coerceAtLeast(0)

        var totalCycleDurationMs = 0L
        var elapsedInCycleMs = 0L
        for (i in definition.sequence.indices) {
            val durationMs = definition.sequence[i].durationSeconds * 1000L
            totalCycleDurationMs += durationMs
            if (i < currentPhaseIndex) {
                elapsedInCycleMs += durationMs
            } else if (i == currentPhaseIndex) {
                elapsedInCycleMs += elapsedInPhaseMs
            }
        }
        
        val cycleProgress = if (totalCycleDurationMs > 0) {
            (elapsedInCycleMs.toFloat() / totalCycleDurationMs).coerceIn(0f, 1f)
        } else {
            1f
        }

        _state.update {
            it.copy(
                phase = currentPhaseDef.phase,
                progress = phaseProgress,
                phaseDuration = currentPhaseDef.durationSeconds,
                remainingTime = remainingSec,
                totalCycleProgress = cycleProgress
            )
        }
    }

    fun clear() {
        timerJob?.cancel()
        scope.cancel()
    }
}
