package com.meteoapp.data

import android.content.Context

/**
 * Localisateur de services léger : construit une seule instance de
 * [WeatherRepository] par process et la partage entre le ViewModel, les
 * widgets et les workers (ils partagent déjà le cache via le contexte
 * applicatif). Remplace la construction dispersée `WeatherRepository(context)`
 * et facilite le remplacement dans les tests.
 */
object ServiceLocator {

    @Volatile
    private var repository: WeatherRepository? = null

    fun weatherRepository(context: Context): WeatherRepository =
        repository ?: synchronized(this) {
            repository ?: WeatherRepository(context.applicationContext).also { repository = it }
        }
}
