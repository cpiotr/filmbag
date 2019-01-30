package pl.ciruk.filmbag.film

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import pl.ciruk.filmbag.FilmBagApplication
import pl.ciruk.filmbag.boundary.ClosedRange
import pl.ciruk.filmbag.boundary.LeftClosedRange
import pl.ciruk.filmbag.boundary.RightClosedRange
import pl.ciruk.filmbag.integration.TestConfiguration
import pl.ciruk.filmbag.request.RequestProcessor
import pl.ciruk.filmbag.testFilmRequest
import pl.ciruk.filmbag.testOtherFilmRequest

@ExtendWith(SpringExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = [FilmBagApplication::class, TestConfiguration::class])
internal class FilmServiceTest(
        @Autowired val requestProcessor: RequestProcessor,
        @Autowired val filmService: FilmService) {

    val firstFilm = testFilmRequest(2009)
    val secondFilm = testFilmRequest(2011)
    val thirdFilm = testOtherFilmRequest(2012)

    @BeforeEach
    internal fun setUp() {
        requestProcessor.storeAll(listOf(firstFilm, secondFilm, thirdFilm))
    }

    @Test
    fun shouldFindStoredFilmsByClosedYearRange() {
        val films = filmService.find(year = ClosedRange(2011, 2012))

        assertThat(films.map { it.title })
                .isEqualTo(listOf(secondFilm.title, thirdFilm.title))
    }

    @Test
    fun shouldFindStoredFilmsByLeftClosedYearRange() {
        val films = filmService.find(year = LeftClosedRange(2011))

        assertThat(films.map { it.title })
                .isEqualTo(listOf(secondFilm.title, thirdFilm.title))
    }

    @Test
    fun shouldFindStoredFilmsByRightClosedYearRange() {
        val films = filmService.find(year = RightClosedRange(2011))

        assertThat(films.map { it.title })
                .isEqualTo(listOf(firstFilm.title, secondFilm.title))
    }

    @Test
    fun shouldNotFindFilmsCreatedLaterThanSpecifiedRange() {
        val films = filmService.find(year = RightClosedRange(2008))

        assertThat(films.map { it.title })
                .isEmpty()
    }

    @Test
    fun shouldNotFindFilmsCreatedEarlierThanSpecifiedRange() {
        val films = filmService.find(year = LeftClosedRange(2020))

        assertThat(films.map { it.title })
                .isEmpty()
    }

    @Test
    fun shouldNotFindFilmsCreatedOutsideSpecifiedRange() {
        val films = filmService.find(year = ClosedRange(2010, 2010))

        assertThat(films.map { it.title })
                .isEmpty()
    }
}
