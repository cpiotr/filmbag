package pl.ciruk.filmbag.film

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import pl.ciruk.filmbag.FilmBagApplication
import pl.ciruk.filmbag.integration.KMariaDbContainer
import pl.ciruk.filmbag.integration.TestConfiguration
import pl.ciruk.filmbag.request.RequestProcessor
import pl.ciruk.filmbag.testFilmRequest
import pl.ciruk.filmbag.testOtherFilmRequest

@ExtendWith(SpringExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = [FilmBagApplication::class, TestConfiguration::class])
@Testcontainers
internal class GenreServiceTest(
        @Autowired val requestProcessor: RequestProcessor,
        @Autowired val genreService: GenreService) {

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

    companion object {
        @Container
        val mariaDb = KMariaDbContainer()
                .withUsername("root")
                .withPassword("")!!

        @JvmStatic
        @DynamicPropertySource
        fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mariaDb.jdbcUrl }
        }
    }
}
