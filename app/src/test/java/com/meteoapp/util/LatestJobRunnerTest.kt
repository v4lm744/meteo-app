package com.meteoapp.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BUG-005 : un chargement plus ancien (par exemple la géolocalisation) ne
 * doit pas écraser la ville choisie ensuite.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LatestJobRunnerTest {

    @Test
    fun newLaunchCancelsPreviousOne() = runTest {
        val runner = LatestJobRunner(this)
        val shown = mutableListOf<String>()

        val gps = runner.launch {
            delay(1_000)
            shown += "position"
        }
        runner.launch {
            delay(100)
            shown += "Lyon"
        }
        advanceUntilIdle()

        assertTrue(gps.isCancelled)
        assertEquals(listOf("Lyon"), shown)
    }

    @Test
    fun childrenOfCancelledLaunchAreCancelled() = runTest {
        val runner = LatestJobRunner(this)
        val shown = mutableListOf<String>()

        runner.launch {
            launch {
                delay(1_000)
                shown += "qualité de l'air Paris"
            }
        }
        runner.launch {
            launch {
                delay(1_000)
                shown += "qualité de l'air Lyon"
            }
        }
        advanceUntilIdle()

        assertEquals(listOf("qualité de l'air Lyon"), shown)
    }

    @Test
    fun completedLaunchDoesNotAffectNextOne() = runTest {
        val runner = LatestJobRunner(this)
        val shown = mutableListOf<String>()

        runner.launch { shown += "Paris" }
        advanceUntilIdle()
        runner.launch { shown += "Lyon" }
        advanceUntilIdle()

        assertEquals(listOf("Paris", "Lyon"), shown)
    }
}
