package pl.ciruk.filmbag.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import pl.ciruk.filmbag.FilmBagApplication
import pl.ciruk.filmbag.request.Journal
import pl.ciruk.filmbag.testFilmRequest
import redis.embedded.RedisServer
import java.io.IOException
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@ExtendWith(SpringExtension::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(classes = [FilmBagApplication::class, EmbeddedRedis::class])
class JournalTest(@Autowired val journal: Journal) {
    @Test
    fun `should record request list to journal and replay it`() {
        val expectedRequests = mutableListOf(testFilmRequest())

        journal.record(expectedRequests)
        val replayedRequests = journal.replay().toList()

        assertThat(replayedRequests)
                .containsExactly(expectedRequests)
    }

    @Test
    fun `should replay empty journal`() {
        val replayedRequests = journal.replay().toList()

        assertThat(replayedRequests)
                .isEmpty()
    }
}

@Component
class EmbeddedRedis {
    @Value("\${redis.port}")
    private val redisPort: Int = 0

    private var redisServer: RedisServer? = null

    @PostConstruct
    @Throws(IOException::class)
    fun startRedis() {
        redisServer = RedisServer(redisPort)
        redisServer!!.start()
    }

    @PreDestroy
    fun stopRedis() {
        redisServer!!.stop()
    }
}
