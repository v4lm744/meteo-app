package com.meteoapp.data.api

import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://api.openweathermap.org/"
    private const val OPEN_METEO_BASE_URL = "https://api.open-meteo.com/"
    private const val OPEN_METEO_AIR_QUALITY_BASE_URL = "https://air-quality-api.open-meteo.com/"
    private const val HTTP_CACHE_SIZE_BYTES = 10L * 1024 * 1024
    private const val DEFAULT_CACHE_CONTROL = "public, max-age=300"

    /**
     * Valeur d'en-tête `Cache-Control` de requête qui contourne la fraîcheur
     * du cache HTTP : passée par le pull-to-refresh (`forceRefresh`) pour
     * toujours obtenir des données du serveur.
     */
    const val NO_CACHE = "no-cache"

    /**
     * Les API météo ne renvoient pas d'en-tête Cache-Control : OkHttp ne
     * réutilise une réponse que si le serveur l'autorise. Cet intercepteur
     * ajoute une fraîcheur courte côté client (5 min) aux réponses réussies.
     * Il doit être un intercepteur **réseau** : OkHttp décide du stockage
     * en cache avant les intercepteurs applicatifs, qui n'y avaient donc
     * aucun effet (BUG-003).
     */
    private val defaultCacheControlInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        if (response.isSuccessful && response.header("Cache-Control") == null) {
            response.newBuilder()
                .header("Cache-Control", DEFAULT_CACHE_CONTROL)
                .build()
        } else {
            response
        }
    }

    /**
     * Cache HTTP disque partagé par les deux fournisseurs, pour qu'un widget
     * ou un écran rouvert ne redescende pas tout le payload.
     */
    private val okHttpClient: OkHttpClient by lazy {
        val builder = buildOkHttpClient(File(httpsCacheDir(), "http-cache"))
        if (com.meteoapp.BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }
        builder.build()
    }

    internal fun buildOkHttpClient(cacheDir: File): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .cache(Cache(cacheDir, HTTP_CACHE_SIZE_BYTES))
            .addNetworkInterceptor(defaultCacheControlInterceptor)

    private fun httpsCacheDir(): File {
        // Sous Android, java.io.tmpdir pointe vers le cache interne de
        // l'application ; Robolectric mappe la même propriété.
        val dir = File(System.getProperty("java.io.tmpdir"), "meteo-http-cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    val moshi: Moshi by lazy {
        Moshi.Builder().build()
    }

    val api: OpenWeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenWeatherApi::class.java)
    }

    val openMeteoApi: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl(OPEN_METEO_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApi::class.java)
    }

    val openMeteoAirQualityApi: OpenMeteoAirQualityApi by lazy {
        Retrofit.Builder()
            .baseUrl(OPEN_METEO_AIR_QUALITY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoAirQualityApi::class.java)
    }
}
