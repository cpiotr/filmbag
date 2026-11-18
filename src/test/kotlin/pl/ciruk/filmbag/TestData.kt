package pl.ciruk.filmbag

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import pl.ciruk.filmbag.request.Journal
import java.time.Duration
import java.time.Instant

class TestData(
    private val jdbcTemplate: JdbcTemplate,
    private val journal: Journal,
    private val taskExecutor: ThreadPoolTaskExecutor
) {
    /** Deletes all films, genres and journal entries, once background work such as journal writes has finished. */
    fun deleteAll() {
        awaitBackgroundWork()
        jdbcTemplate.batchUpdate("DELETE FROM film_genres", "DELETE FROM score", "DELETE FROM film", "DELETE FROM genre")
        journal.deleteAll()
    }

    private fun awaitBackgroundWork() {
        val deadline = Instant.now() + Duration.ofSeconds(10)
        while (taskExecutor.activeCount > 0 || taskExecutor.threadPoolExecutor.queue.isNotEmpty()) {
            check(Instant.now() < deadline) { "Background tasks still running after 10 s" }
            Thread.onSpinWait()
        }
    }
}
