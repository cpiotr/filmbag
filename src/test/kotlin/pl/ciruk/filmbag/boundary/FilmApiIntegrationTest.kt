package pl.ciruk.filmbag.boundary

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import pl.ciruk.filmbag.IntegrationTest
import pl.ciruk.filmbag.TestData
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import pl.ciruk.filmbag.film.FilmData
import pl.ciruk.filmbag.film.ScoreData
import pl.ciruk.filmbag.film.GenreService
import pl.ciruk.filmbag.film.ScoreType
import pl.ciruk.filmbag.testFilm
import pl.ciruk.filmbag.testOtherFilm
import pl.ciruk.filmbag.testScore
import java.util.UUID

@IntegrationTest
class FilmApiIntegrationTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val genreService: GenreService,
    @Autowired val testData: TestData
) {

    @BeforeEach
    fun deleteAllData() {
        testData.deleteAll()
    }

    @Test
    fun `should get created films`() {
        val filmRequest = testFilm()
        val otherFilmRequest = testOtherFilm()

        postFilms(filmRequest, otherFilmRequest)
        val arrayOfFilmRequests = getFilms()

        assertThat(arrayOfFilmRequests).containsOnly(filmRequest, otherFilmRequest)
    }

    @Test
    fun `should find created film by year`() {
        val filmRequest = testFilm(year = 2020)
        val otherFilmRequest = testOtherFilm(year = 2010)
        val yetAnotherFilmRequest = testFilm(year = 2021)
        val nextFilmRequest = testFilm(year = 2030)

        postFilms(filmRequest, otherFilmRequest, yetAnotherFilmRequest, nextFilmRequest)
        val arrayOfFilmRequests = getFilms(yearFrom = 2019, yearTo = 2025)

        assertThat(arrayOfFilmRequests).containsOnly(filmRequest, yetAnotherFilmRequest)
    }

    @Test
    fun `should find created film by score`() {
        val filmRequest = testFilm(score = 0.61)
        val otherFilmRequest = testOtherFilm(score = 0.65)
        val yetAnotherFilmRequest = testFilm(score = 0.7)
        val nextFilmRequest = testFilm(score = 0.8)

        postFilms(filmRequest, otherFilmRequest, yetAnotherFilmRequest, nextFilmRequest)
        val arrayOfFilmRequests = getFilms(scoreFrom = 0.625, scoreTo = 0.75)

        assertThat(arrayOfFilmRequests).containsOnly(otherFilmRequest, yetAnotherFilmRequest)
    }

    @Test
    fun `should find created film by score and year`() {
        val filmRequest = testFilm(score = 0.61, year = 2005)
        val otherFilmRequest = testOtherFilm(score = 0.65, year = 2010)
        val yetAnotherFilmRequest = testFilm(score = 0.7, year = 2015)
        val nextFilmRequest = testFilm(score = 0.8, year = 2020)

        postFilms(filmRequest, otherFilmRequest, yetAnotherFilmRequest, nextFilmRequest)
        val arrayOfFilmRequests = getFilms(scoreFrom = 0.625, scoreTo = 0.75, yearFrom = 2013)

        assertThat(arrayOfFilmRequests).containsOnly(yetAnotherFilmRequest)
    }

    @Test
    fun `should not store single film twice`() {
        val filmRequest = testFilm()

        postFilms(filmRequest)
        postFilms(filmRequest)

        val arrayOfFilmRequests = getFilms()
        assertThat(arrayOfFilmRequests)
            .hasSize(1)
            .containsOnly(filmRequest)
    }

    @Test
    fun `should store a film once when one request names it twice`() {
        val filmRequest = testOtherFilm(score = 0.5)
        val laterCopy = filmRequest.copy(score = 0.6, scores = setOf(testScore(0.6, 20)))

        val response = postFilms(filmRequest, laterCopy)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        val storedFilm = getFilms().single()
        assertThat(storedFilm.score).isEqualTo(0.6)
        assertThat(storedFilm.scores).isEqualTo(filmRequest.scores + laterCopy.scores)
    }

    @Test
    fun `should update scores when a film is posted twice`() {
        val filmRequest = testFilm()
        val updatedFilmRequest = filmRequest.copy(
            score = filmRequest.score?.plus(1.0),
            scores = filmRequest.scores.plus(testScore(0.789, 1234)),
            numberOfScores = filmRequest.numberOfScores + 1
        )

        postFilms(filmRequest)
        postFilms(updatedFilmRequest)

        val arrayOfFilmRequests = getFilms()
        assertThat(arrayOfFilmRequests)
            .hasSize(1)
            .containsOnly(updatedFilmRequest)
    }

    @Test
    fun `should get paginated films sorted by creation time in descending order`() {
        val filmRequest = testFilm()
        postFilms(filmRequest)
        val otherFilmRequest = testOtherFilm()
        postFilms(otherFilmRequest)

        val firstPage = getPage("/resources/films?page=0&pageSize=1")
        val secondPage = getPage(checkNotNull(firstPage.next))

        assertThat(firstPage.items).containsOnly(otherFilmRequest)
        assertThat(firstPage.next).contains("/resources/films?").contains("page=1").contains("pageSize=1")
        assertThat(secondPage.items).containsOnly(filmRequest)
        assertThat(secondPage.next).isNull()
    }

    @Test
    fun `should not create duplicate genres`() {
        assertThat(genreService.findAll())
            .isEmpty()

        val filmRequest = testFilm(year = 2020)
        val otherFilmRequest = testOtherFilm(year = 2010)
        val yetAnotherFilmRequest = testFilm(year = 2021)
        val nextFilmRequest = testFilm(year = 2030)

        postFilms(filmRequest, otherFilmRequest, yetAnotherFilmRequest, nextFilmRequest)

        assertThat(genreService.findAll().map { it.name })
            .containsExactlyInAnyOrder("Genre1", "Genre2", "Genre3", "Genre4")
    }

    @Test
    fun `should map score types`() {
        assertThat(genreService.findAll())
            .isEmpty()

        val filmRequest = testFilm(year = 2020)

        postFilms(filmRequest)
        val foundFilms = getFilms(page = 0, pageSize = 1)

        val foundScoreTypes = foundFilms
            .flatMap { it.scores }
            .map { it.type }
            .toSet()
        assertThat(foundScoreTypes).containsExactlyInAnyOrderElementsOf(ScoreType.values().map { it.name })
    }

    @Test
    fun `should reject requests without credentials`() {
        val response = restTemplate.getForEntity("/resources/films", String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `should report health without credentials but protect other actuator endpoints`() {
        val health = restTemplate.getForEntity("/resources/actuator/health/readiness", String::class.java)
        val actuator = restTemplate.getForEntity("/resources/actuator", String::class.java)

        assertThat(health.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(health.body).isEqualTo("""{"status":"UP"}""")
        assertThat(actuator.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `should not change state over GET`() {
        val response = restTemplate
            .withBasicAuth("user", "password")
            .getForEntity("/resources/journal/replay", String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
    }

    @Test
    fun `should reject films without required fields, naming them in the problem details`() {
        val response = restTemplate
            .withBasicAuth("user", "password")
            .postForEntity("/resources/films", arrayOf(testFilm(), FilmData(title = "No year")), Map::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON)
        assertThat(response.body!!["invalidFilms"])
            .isEqualTo(listOf(mapOf("index" to 1, "problems" to listOf("year is missing", "link is missing", "score is missing"))))
        assertThat(getFilms()).isEmpty()
    }

    @Test
    fun `should reject a malformed filter as a bad request`() {
        val response = get("/resources/films?yearFrom=199x")

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON)
    }

    @Test
    fun `should replay the journal and report the result`() {
        val filmRequest = testOtherFilm()
        postFilms(filmRequest)
        testData.deleteFilms()

        val response = post("/resources/journal/replay")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(mapOf("replayed" to 1, "failed" to emptyList<String>()))
        assertThat(getFilms()).containsOnly(filmRequest)
    }

    @Test
    fun `should record all films in a snapshot and report the number of entries`() {
        postFilms(testFilm(), testOtherFilm())

        val response = post("/resources/journal/snapshot")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(mapOf("journalEntries" to 1))
    }

    @Test
    fun `should reject page size above the limit`() {
        val response = restTemplate
            .withBasicAuth("user", "password")
            .getForEntity("/resources/films?pageSize=101", String::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `should store score without url`() {
        val filmRequest = testOtherFilm().copy(scores = setOf(ScoreData(0.5, 100, url = null)))

        val response = postFilms(filmRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(getFilms()).containsOnly(filmRequest)
    }

    @Test
    fun `should store genre after a rejected request with the same new genre`() {
        val tooLongPoster = "https://poster/" + "x".repeat(600)
        val failingFilmRequest = testFilm().copy(genres = setOf("Noir"), poster = tooLongPoster)
        val filmRequest = testOtherFilm().copy(genres = setOf("Noir"))

        val failedResponse = postFilms(failingFilmRequest)
        val response = postFilms(filmRequest)

        assertThat(failedResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(getFilms()).containsOnly(filmRequest)
    }

    @Test
    fun `should keep scores that differ only by type or url`() {
        val filmRequest = testOtherFilm().copy(scores = setOf(
            ScoreData(0.5, 100, type = "CRITIC", url = "http://critics"),
            ScoreData(0.5, 100, type = "AMATEUR", url = "http://critics"),
            ScoreData(0.5, 100, type = "CRITIC", url = "http://other-critics")))

        postFilms(filmRequest)

        assertThat(getFilms().single().scores).isEqualTo(filmRequest.scores)
    }

    @Test
    fun `should keep genres that differ only in letter case`() {
        val filmRequest = testFilm().copy(genres = setOf("Drama"))
        val otherFilmRequest = testOtherFilm().copy(genres = setOf("drama"))

        val response = postFilms(filmRequest, otherFilmRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(genreService.findAll().map { it.name }).containsExactlyInAnyOrder("Drama", "drama")
    }

    @Test
    fun `should store titles and genres with 4-byte characters`() {
        val filmRequest = testOtherFilm().copy(title = "Popcorn 🍿 " + UUID.randomUUID(), genres = setOf("Horror 👻"))

        val response = postFilms(filmRequest)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(getFilms()).containsOnly(filmRequest)
    }

    private fun postFilms(vararg films: FilmData): ResponseEntity<String> {
        return restTemplate
            .withBasicAuth("user", "password")
            .postForEntity("/resources/films", films, String::class.java)
    }

    private fun getFilms(
        yearFrom: Int? = null,
        yearTo: Int? = null,
        scoreFrom: Double? = null,
        scoreTo: Double? = null,
        page: Int = 0,
        pageSize: Int = 100
    ): Array<FilmData> {
        val filters = mapOf("yearFrom" to yearFrom, "yearTo" to yearTo, "scoreFrom" to scoreFrom, "scoreTo" to scoreTo)
            .filterValues { it != null }
            .map { (name, value) -> "&$name=$value" }
            .joinToString("")
        return getPage("/resources/films?page=$page&pageSize=$pageSize$filters").items.toTypedArray()
    }

    private fun getPage(url: String): FilmPage {
        val page = restTemplate
            .withBasicAuth("user", "password")
            .getForObject(url, FilmPage::class.java)
        return checkNotNull(page) { "No response body for GET $url" }
    }

    private fun post(url: String): ResponseEntity<Map<*, *>> {
        return restTemplate
            .withBasicAuth("user", "password")
            .postForEntity(url, null, Map::class.java)
    }

    private fun get(url: String): ResponseEntity<Map<*, *>> {
        return restTemplate
            .withBasicAuth("user", "password")
            .getForEntity(url, Map::class.java)
    }
}
