package pl.ciruk.filmbag

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisKeyValueAdapter
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.boundary.ScoreRequest
import pl.ciruk.filmbag.request.DataLoader

@ExtendWith(SpringExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = [FilmBagApplication::class, TestConfiguration::class])
class RequestProcessorIntegrationTest(@Autowired val restTemplate: TestRestTemplate) {
    @Test
    fun `should get created films`() {
        val filmRequest = FilmRequest(
                title = "Test title",
                year = 1912,
                score = 0.623,
                numberOfScores = 2,
                scores = setOf(ScoreRequest(0.1, 2), ScoreRequest(0.2, 3)),
                genres = setOf("Genre1", "Genre2", "Genre3"),
                link = "http://test/image.png"
        )
        val otherFilmRequest = FilmRequest(
                title = "Other title",
                year = 1999,
                score = 0.87,
                numberOfScores = 1,
                scores = setOf(ScoreRequest(0.1, 10)),
                genres = setOf("Genre2", "Genre1", "Genre4"),
                link = "http://other/image.png"
        )

        executePutRequest(filmRequest, otherFilmRequest)
        val arrayOfFilmRequests = executeGetRequest()

        assertThat(arrayOfFilmRequests).containsOnly(filmRequest, otherFilmRequest)
    }

    @Test
    fun `should not store single film twice`() {
        val filmRequest = FilmRequest(
                title = "Some title",
                year = 1914,
                score = 0.123,
                numberOfScores = 3,
                scores = setOf(ScoreRequest(0.1, 2), ScoreRequest(0.2, 3), ScoreRequest(0.4, 4)),
                genres = setOf("Genre1", "Genre3"),
                link = "http://test123/image.png"
        )

        executePutRequest(filmRequest)
        executePutRequest(filmRequest)

        val arrayOfFilmRequests = executeGetRequest()

        assertThat(arrayOfFilmRequests)
                .hasSize(1)
                .containsOnly(filmRequest)
    }

    private fun executeGetRequest() = restTemplate.getForObject("/resources/films", Array<FilmRequest>::class.java)

    private fun executePutRequest(vararg filmRequests: FilmRequest) {
        restTemplate.put("/resources/films", filmRequests)
    }
}

@Configuration
class TestConfiguration {
    @Bean
    fun dataLoader(): DataLoader {
        return mock(DataLoader::class.java)
    }

    @Bean
    @Primary
    fun redisConnectionFactory(): RedisConnectionFactory {
        return mock(RedisConnectionFactory::class.java)
    }

    @Bean
    @Primary
    fun redisTemplate(): RedisTemplate<ByteArray, ByteArray> {
        val redisTemplate = mock(RedisTemplate::class.java)

        doReturn(mock(ValueOperations::class.java)).`when`(redisTemplate).opsForValue()
        return redisTemplate as RedisTemplate<ByteArray, ByteArray>
    }

    @Bean
    fun redisKeyValueAdapter(): RedisKeyValueAdapter {
        return mock(RedisKeyValueAdapter::class.java)
    }
}
