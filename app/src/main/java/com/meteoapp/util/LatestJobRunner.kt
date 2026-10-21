package com.meteoapp.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Lance une coroutine en annulant la précédente : seul le dernier
 * chargement demandé peut encore mettre à jour l'état. Les coroutines
 * lancées à l'intérieur du bloc (`launch` sur son receveur) sont ses
 * enfants et sont annulées avec lui (BUG-005).
 *
 * À appeler depuis un seul thread (le thread principal pour un ViewModel).
 */
class LatestJobRunner(private val scope: CoroutineScope) {

    private var current: Job? = null

    fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        current?.cancel()
        return scope.launch(block = block).also { current = it }
    }
}
