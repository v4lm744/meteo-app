package com.meteoapp.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * BUG-003 : l'en-tête de fraîcheur était ajouté par un intercepteur
 * applicatif, sans effet sur le stockage en cache d'OkHttp.
 */
class ApiClientHttpCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = ApiClient.buildOkHttpClient(tempFolder.newFolder("http-cache")).build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun get(cacheControl: String? = null): okhttp3.Response {
        val builder = Request.Builder().url(server.url("/v1/forecast"))
        if (cacheControl != null) builder.header("Cache-Control", cacheControl)
        return client.newCall(builder.build()).execute()
    }

    private fun getBody(cacheControl: String? = null): String =
        get(cacheControl).use { it.body.string() }

    @Test
    fun successfulResponseWithoutCacheControlIsServedFromCache() {
        server.enqueue(MockResponse().setBody("first"))
        server.enqueue(MockResponse().setBody("second"))

        assertEquals("first", getBody())
        get().use { response ->
            assertEquals("first", response.body.string())
            assertNotNull("la 2e réponse doit venir du cache", response.cacheResponse)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun noCacheRequestHeaderBypassesFreshCache() {
        server.enqueue(MockResponse().setBody("first"))
        server.enqueue(MockResponse().setBody("second"))

        assertEquals("first", getBody())
        assertEquals("second", getBody(ApiClient.NO_CACHE))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun errorResponsesAreNotCached() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("error"))
        server.enqueue(MockResponse().setBody("ok"))

        getBody()
        assertEquals("ok", getBody())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun serverCacheControlIsKept() {
        server.enqueue(MockResponse().setBody("first").setHeader("Cache-Control", "no-store"))
        server.enqueue(MockResponse().setBody("second"))

        assertEquals("first", getBody())
        assertEquals("second", getBody())
        assertEquals(2, server.requestCount)
    }
}
