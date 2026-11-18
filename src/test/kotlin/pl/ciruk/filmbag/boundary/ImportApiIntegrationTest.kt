package pl.ciruk.filmbag.boundary

import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import pl.ciruk.filmbag.IntegrationTest
import pl.ciruk.filmbag.TestData
import pl.ciruk.filmbag.film.FilmData
import pl.ciruk.filmbag.testFilm
import pl.ciruk.filmbag.testOtherFilm
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

@IntegrationTest
class ImportApiIntegrationTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val testData: TestData
) {
    @BeforeEach
    fun deleteAllData() {
        testData.deleteAll()
        pages.clear()
        failingStatus = null
    }

    @Test
    fun `should import films page by page and say where the next import continues`() {
        val firstPage = listOf(testFilm(), testOtherFilm())
        val secondPage = listOf(testOtherFilm())
        pages[1] = firstPage
        pages[2] = secondPage

        val response = post("/resources/import/0")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(mapOf("lastImportedOffset" to 1, "nextOffset" to 2))
        val films = restTemplate.withBasicAuth("user", "password").getForObject("/resources/films", FilmPage::class.java)
        assertThat(films!!.items).containsExactlyInAnyOrderElementsOf(firstPage + secondPage)
    }

    @Test
    fun `should report a failing provider as a bad gateway`() {
        failingStatus = 503

        val response = post("/resources/import/0")

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_GATEWAY)
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON)
        assertThat(response.body!!["detail"] as String).contains("HTTP 503")
    }

    @Test
    fun `should reject a negative offset`() {
        val response = post("/resources/import/-1")

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `should accept a cancel when no import runs`() {
        val response = post("/resources/import/cancel")

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }

    private fun post(url: String): ResponseEntity<Map<*, *>> {
        return restTemplate
            .withBasicAuth("user", "password")
            .postForEntity(url, null, Map::class.java)
    }

    companion object {
        private val pages = ConcurrentHashMap<Int, List<FilmData>>()

        @Volatile
        private var failingStatus: Int? = null

        private val provider: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            val mapper = jacksonObjectMapper()
            createContext("/films/") { exchange ->
                val page = exchange.requestURI.path.substringAfterLast('/').toInt()
                val status = failingStatus ?: 200
                val body = if (status == 200) mapper.writeValueAsBytes(pages[page].orEmpty()) else ByteArray(0)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(status, if (body.isEmpty()) -1 else body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun providerProperties(registry: DynamicPropertyRegistry) {
            registry.add("filmbag.provider.url") { "http://127.0.0.1:${provider.address.port}/films" }
        }
    }
}
