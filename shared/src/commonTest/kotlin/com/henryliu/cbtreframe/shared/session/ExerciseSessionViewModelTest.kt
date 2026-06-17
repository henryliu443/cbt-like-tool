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
class ExerciseSessionViewModelTest {

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
    fun debugTest() = testScope.runTest {
        val definition = ExerciseDefinition(
            id = "test_1",
            name = "Test Exercise",
            sequence = listOf(
                PhaseDuration(ExercisePhase.INHALE, 2)
            ),
            totalCycles = 1
        )
        val viewModel = ExerciseSessionViewModel(definition, testScope)
        viewModel.start()
        
        advanceTimeBy(1000)
        println("After 1000ms: state = ${viewModel.state.value}")
        advanceTimeBy(1000)
        println("After 2000ms: state = ${viewModel.state.value}")
    }
}
