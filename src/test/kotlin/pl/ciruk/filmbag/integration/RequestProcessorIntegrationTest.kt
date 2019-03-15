package pl.ciruk.filmbag.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import pl.ciruk.filmbag.FilmBagApplication
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.boundary.FilmResource
import pl.ciruk.filmbag.request.DataLoader
import pl.ciruk.filmbag.testFilmRequest
import pl.ciruk.filmbag.testOtherFilmRequest
import redis.clients.jedis.JedisPool

@ExtendWith(SpringExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = [FilmBagApplication::class, TestConfiguration::class])
class RequestProcessorIntegrationTest(@Autowired val restTemplate: TestRestTemplate) {
    @Test
    fun `should get created films`() {
        val filmRequest = testFilmRequest()
        val otherFilmRequest = testOtherFilmRequest()

        executePutRequest(filmRequest, otherFilmRequest)
        val arrayOfFilmRequests = executeGetRequest()

        assertThat(arrayOfFilmRequests).containsOnly(filmRequest, otherFilmRequest)
    }

    @Test
    fun `should find created film by year`() {
        val filmRequest = testFilmRequest(year = 2020)
        val otherFilmRequest = testOtherFilmRequest(year = 2010)
        val yetAnotherFilmRequest = testFilmRequest(year = 2021)
        val nextFilmRequest = testFilmRequest(year = 2030)

        executePutRequest(filmRequest, otherFilmRequest, yetAnotherFilmRequest, nextFilmRequest)
        val arrayOfFilmRequests = executeGetRequest(yearFrom = 2019, yearTo = 2025)

        assertThat(arrayOfFilmRequests).containsOnly(filmRequest, yetAnotherFilmRequest)
    }

    @Test
    fun `should find created film by score`() {
        val filmRequest = testFilmRequest(score = 0.61)
        val otherFilmRequest = testOtherFilmRequest(score = 0.65)
        val yetAnotherFilmRequest = testFilmRequest(score = 0.7)
        val nextFilmRequest = testFilmRequest(score = 0.8)

        executePutRequest(filmRequest, otherFilmRequest, yetAnotherFilmRequest, nextFilmRequest)
        val arrayOfFilmRequests = executeGetRequest(scoreFrom = 0.625, scoreTo = 0.75)

        assertThat(arrayOfFilmRequests).containsOnly(otherFilmRequest, yetAnotherFilmRequest)
    }

    @Test
    fun `should find created film by score and year`() {
        val filmRequest = testFilmRequest(score = 0.61, year = 2005)
        val otherFilmRequest = testOtherFilmRequest(score = 0.65, year = 2010)
        val yetAnotherFilmRequest = testFilmRequest(score = 0.7, year = 2015)
        val nextFilmRequest = testFilmRequest(score = 0.8, year = 2020)

        executePutRequest(filmRequest, otherFilmRequest, yetAnotherFilmRequest, nextFilmRequest)
        val arrayOfFilmRequests = executeGetRequest(scoreFrom = 0.625, scoreTo = 0.75, yearFrom = 2013)

        assertThat(arrayOfFilmRequests).containsOnly(yetAnotherFilmRequest)
    }

    @Test
    fun `should not store single film twice`() {
        val filmRequest = testFilmRequest()

        executePutRequest(filmRequest)
        executePutRequest(filmRequest)

        val arrayOfFilmRequests = executeGetRequest()
        assertThat(arrayOfFilmRequests)
                .hasSize(1)
                .containsOnly(filmRequest)
    }

    @Test
    fun `should get paginated films`() {
        val filmRequest = testFilmRequest()
        val otherFilmRequest = testOtherFilmRequest()

        executePutRequest(filmRequest, otherFilmRequest)

        assertThat(executeGetRequest(page = 0, pageSize = 1)).containsOnly(filmRequest)
        assertThat(executeGetRequest(page = 1, pageSize = 1)).containsOnly(otherFilmRequest)
    }

    private fun executeGetRequest(
            yearFrom: Int = FilmResource.missingInt,
            yearTo: Int = FilmResource.missingInt,
            scoreFrom: Double = FilmResource.missingDecimal,
            scoreTo: Double = FilmResource.missingDecimal,
            page: Int = 0,
            pageSize: Int = 100): Array<FilmRequest> {
        val url = "/resources/films" +
                "?yearFrom=$yearFrom" +
                "&yearTo=$yearTo" +
                "&scoreFrom=$scoreFrom" +
                "&scoreTo=$scoreTo" +
                "&page=$page" +
                "&pageSize=$pageSize"
        return restTemplate.getForObject(url, Array<FilmRequest>::class.java)
    }

    private fun executePutRequest(vararg filmRequests: FilmRequest) {
        restTemplate.put("/resources/films", filmRequests)
    }
}

@Configuration
class TestConfiguration {
    @Bean
    @Primary
    fun testDataLoader(): DataLoader {
        return mock(DataLoader::class.java)
    }

    @Bean
    @Primary
    fun testJedisPool(): JedisPool {
        return mock(JedisPool::class.java)
    }
}
