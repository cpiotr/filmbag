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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import pl.ciruk.filmbag.FilmBagApplication
import pl.ciruk.filmbag.boundary.FilmReadResource
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.film.GenreService
import pl.ciruk.filmbag.film.ScoreType
import pl.ciruk.filmbag.request.DataLoader
import pl.ciruk.filmbag.testFilmRequest
import pl.ciruk.filmbag.testOtherFilmRequest
import pl.ciruk.filmbag.testScoreRequest
import redis.clients.jedis.JedisPool

@ExtendWith(SpringExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [FilmBagApplication::class, TestConfiguration::class]
)
@Testcontainers
class RequestProcessorIntegrationTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val genreService: GenreService,
    @Autowired val filService: FilmService
) {

    @Test
    fun `should get created films`() {
        val filmRequest = testFilmRequest()
        val otherFilmRequest = testOtherFilmRequest()

        executePutRequest(filmRequest, otherFilmRequest)
        val find = filService.find()
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
    fun `should update scores when film request is put twice`() {
        val filmRequest = testFilmRequest()
        val updatedFilmRequest = filmRequest.copy(
            score = filmRequest.score?.plus(1.0),
            scores = filmRequest.scores.plus(testScoreRequest(0.789, 1234)),
            numberOfScores = filmRequest.numberOfScores + 1
        )

        executePutRequest(filmRequest)
        executePutRequest(updatedFilmRequest)

        val arrayOfFilmRequests = executeGetRequest()
        assertThat(arrayOfFilmRequests)
            .hasSize(1)
            .containsOnly(updatedFilmRequest)
    }

    @Test
    fun `should get paginated films sorted by creation time in descending order`() {
        val filmRequest = testFilmRequest()
        executePutRequest(filmRequest)
        val otherFilmRequest = testOtherFilmRequest()
        executePutRequest(otherFilmRequest)

        assertThat(executeGetRequest(page = 0, pageSize = 1)).containsOnly(otherFilmRequest)
        assertThat(executeGetRequest(page = 1, pageSize = 1)).containsOnly(filmRequest)
    }

    @Test
    fun `should not create duplicate genres`() {
        assertThat(genreService.findAll())
            .isEmpty()

        val filmRequest = testFilmRequest(year = 2020)
        val otherFilmRequest = testOtherFilmRequest(year = 2010)
        val yetAnotherFilmRequest = testFilmRequest(year = 2021)
        val nextFilmRequest = testFilmRequest(year = 2030)

        executePutRequest(filmRequest, otherFilmRequest, yetAnotherFilmRequest, nextFilmRequest)

        assertThat(genreService.findAll().map { it.name })
            .containsExactlyInAnyOrder("Genre1", "Genre2", "Genre3", "Genre4")
    }

    @Test
    fun `should map score types`() {
        assertThat(genreService.findAll())
            .isEmpty()

        val filmRequest = testFilmRequest(year = 2020)

        executePutRequest(filmRequest)
        val foundFilms = executeGetRequest(page = 0, pageSize = 1)

        val foundScoreTypes = foundFilms
            .flatMap { it.scores }
            .map { it.type }
            .toSet()
        assertThat(foundScoreTypes).containsExactlyInAnyOrderElementsOf(ScoreType.values().map { it.name })
    }

    private fun executeGetRequest(
        yearFrom: Int = FilmReadResource.missingInt,
        yearTo: Int = FilmReadResource.missingInt,
        scoreFrom: Double = FilmReadResource.missingDecimal,
        scoreTo: Double = FilmReadResource.missingDecimal,
        page: Int = 0,
        pageSize: Int = 100
    ): Array<FilmRequest> {
        val url = "/resources/films" +
                "?yearFrom=$yearFrom" +
                "&yearTo=$yearTo" +
                "&scoreFrom=$scoreFrom" +
                "&scoreTo=$scoreTo" +
                "&page=$page" +
                "&pageSize=$pageSize"
        return restTemplate
            .withBasicAuth("user", "password")
            .getForObject(url, Array<FilmRequest>::class.java)
    }

    private fun executePutRequest(vararg filmRequests: FilmRequest) {
        restTemplate
            .withBasicAuth("user", "password")
            .put("/resources/films", filmRequests)
    }

    companion object {
        @Container
        val mariaDb = KMariaDbContainer()
            .withUsername("root")
            .withPassword("")!!

        @JvmStatic
        @DynamicPropertySource
        fun databaseProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mariaDb.jdbcUrl }
        }
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
