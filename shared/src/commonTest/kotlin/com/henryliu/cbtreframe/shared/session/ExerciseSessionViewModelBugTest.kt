package com.henryliu.cbtreframe.shared.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseSessionViewModelBugTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testStartAfterCompletion() = testScope.runTest {
        val definition = ExerciseDefinition(
            id = "test_1",
            name = "Test",
            sequence = listOf(
                PhaseDuration(ExercisePhase.INHALE, 1) // 1 second
            ),
            totalCycles = 1
        )

        val viewModel = ExerciseSessionViewModel(definition, testScope)
        
        viewModel.start()
        
        // Advance 1 second to complete
        advanceTimeBy(1050)
        assertTrue(viewModel.state.value.isPaused)
        assertEquals(ExercisePhase.REST, viewModel.state.value.phase)
        
        // Start again!
        viewModel.start()
        
        // Advance a bit
        advanceTimeBy(500)
        // It starts playing again from cycle = 1? But state.cycle is still 1. 
        // Wait, definition.totalCycles is 1. nextCycle will be 2 > 1.
        // It completes after 1 cycle again.
        println("After restarting, phase: ${viewModel.state.value.phase}")
    }

    @Test
    fun testStartMultipleTimes() = testScope.runTest {
        val definition = ExerciseDefinition(
            id = "test_1",
            name = "Test",
            sequence = listOf(
                PhaseDuration(ExercisePhase.INHALE, 1) // 1 second
            ),
            totalCycles = 1
        )
        val viewModel = ExerciseSessionViewModel(definition, testScope)
        viewModel.start()
        viewModel.start() // should not create a second job
    }
}
