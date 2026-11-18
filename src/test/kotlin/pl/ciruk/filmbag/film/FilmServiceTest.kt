package pl.ciruk.filmbag.film

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import pl.ciruk.filmbag.IntegrationTest
import pl.ciruk.filmbag.TestData
import pl.ciruk.filmbag.testFilm
import pl.ciruk.filmbag.testOtherFilm
import java.time.ZonedDateTime

@IntegrationTest
internal class FilmServiceTest(
    @Autowired val filmStore: FilmStore,
    @Autowired val filmService: FilmService,
    @Autowired val testData: TestData
) {

    val firstFilm = testFilm(2009, 0.9)
    val secondFilm = testFilm(2011, 0.7)
    val thirdFilm = testOtherFilm(2012, 0.6)

    @BeforeEach
    internal fun setUp() {
        testData.deleteAll()
        filmStore.storeAll(listOf(firstFilm, secondFilm, thirdFilm))
    }

    @Test
    fun `should find stored films by closed year range`() {
        val films = filmService.find(year = Between(2011, 2012))

        assertThat(films.map { it.title })
            .containsExactlyInAnyOrder(secondFilm.title, thirdFilm.title)
    }

    @Test
    fun `should find stored films by left closed year range`() {
        val films = filmService.find(year = AtLeast(2011))

        assertThat(films.map { it.title })
            .containsExactlyInAnyOrder(secondFilm.title, thirdFilm.title)
    }

    @Test
    fun `should find stored films by right closed year range`() {
        val films = filmService.find(year = AtMost(2011))

        assertThat(films.map { it.title })
            .containsExactlyInAnyOrder(firstFilm.title, secondFilm.title)
    }

    @Test
    fun `should not find films created later than specified range`() {
        val films = filmService.find(year = AtMost(2008))

        assertThat(films.map { it.title })
            .isEmpty()
    }

    @Test
    fun `should not find films created earlier than specified range`() {
        val films = filmService.find(year = AtLeast(2020))

        assertThat(films.map { it.title })
            .isEmpty()
    }

    @Test
    fun `should not find films created outside specified range`() {
        val films = filmService.find(year = Between(2010, 2010))

        assertThat(films.map { it.title })
            .isEmpty()
    }

    @Test
    fun `should find stored films by closed score range`() {
        val films = filmService.find(score = Between(0.6.toBigDecimal(), 0.8999.toBigDecimal()))

        assertThat(films.map { it.title })
            .containsExactlyInAnyOrder(secondFilm.title, thirdFilm.title)
    }

    @Test
    fun `should find stored films by left closed score range`() {
        val films = filmService.find(score = AtLeast(0.7.toBigDecimal()))

        assertThat(films.map { it.title })
            .containsExactlyInAnyOrder(firstFilm.title, secondFilm.title)
    }

    @Test
    fun `should find stored films by right closed score range`() {
        val films = filmService.find(score = AtMost(0.89999.toBigDecimal()))

        assertThat(films.map { it.title })
            .containsExactlyInAnyOrder(secondFilm.title, thirdFilm.title)
    }

    @Test
    fun `should not find films with score greater than specified range`() {
        val films = filmService.find(score = AtMost(0.5999.toBigDecimal()))

        assertThat(films.map { it.title })
            .isEmpty()
    }

    @Test
    fun `should not find films with score lower than specified range`() {
        val films = filmService.find(score = AtLeast(0.999.toBigDecimal()))

        assertThat(films.map { it.title })
            .isEmpty()
    }

    @Test
    fun `should not find films with score outside specified range`() {
        val films = filmService.find(score = Between(0.8.toBigDecimal(), 0.8.toBigDecimal()))

        assertThat(films.map { it.title })
            .isEmpty()
    }

    @Test
    fun `should page through every film once, newest first, even when films share a creation time`() {
        val sameTime = ZonedDateTime.parse("2001-01-01T00:00:00Z")
        val filmsCreatedTogether = List(5) { testOtherFilm(year = 2000 + it).copy(created = sameTime) }
        filmStore.storeAll(filmsCreatedTogether)

        val pages = filmService.findAllInPages(pageSize = 2).toList()

        assertThat(pages.map { it.size }).containsExactly(2, 2, 2, 2)
        assertThat(pages.flatten().map { it.title })
            .containsExactlyElementsOf((listOf(thirdFilm, secondFilm, firstFilm) + filmsCreatedTogether.reversed()).map { it.title })
        assertThat(pages.flatten().flatMap { it.scores }).isNotEmpty()
    }

    @Test
    fun `should find films whose scores and genres can be read after the transaction`() {
        val films = filmService.find()

        assertThat(films.content.flatMap { it.scores }).isNotEmpty()
        assertThat(films.content.flatMap { it.genres }).isNotEmpty()
    }
}
