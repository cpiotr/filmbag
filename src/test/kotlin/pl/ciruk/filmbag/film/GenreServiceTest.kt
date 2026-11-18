package pl.ciruk.filmbag.film

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import pl.ciruk.filmbag.IntegrationTest
import pl.ciruk.filmbag.TestData
import pl.ciruk.filmbag.request.RequestProcessor
import pl.ciruk.filmbag.testFilmRequest
import pl.ciruk.filmbag.testOtherFilmRequest

@IntegrationTest
internal class GenreServiceTest(
        @Autowired val requestProcessor: RequestProcessor,
        @Autowired val genreService: GenreService,
        @Autowired val testData: TestData) {

    @BeforeEach
    fun deleteAllData() {
        testData.deleteAll()
    }

    val firstFilm = testFilmRequest(2009)
    val secondFilm = testFilmRequest(2011)
    val thirdFilm = testOtherFilmRequest(2012)

    @Test
    fun `should not create duplicate genres`() {
        val expectedGenres = (firstFilm.genres + secondFilm.genres + thirdFilm.genres).toSet()
        requestProcessor.storeAll(listOf(firstFilm, secondFilm, thirdFilm))

        val genres = genreService.findAll()

        assertThat(genres.map { it.name })
                .containsExactlyElementsOf(expectedGenres)
    }
}
