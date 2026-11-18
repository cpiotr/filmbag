package pl.ciruk.filmbag.request

import mu.KotlinLogging
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest
import redis.clients.jedis.RedisClient
import redis.clients.jedis.params.XAddParams
import redis.clients.jedis.resps.StreamEntry
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue
import java.util.concurrent.CompletableFuture

/**
 * Every stored list of films, in the order they were stored, so that the database can be rebuilt from it.
 * Entries are JSON in a Redis stream, which numbers them in order.
 */
@Service
class Journal(
    private val redisClient: RedisClient,
    private val taskExecutor: TaskExecutor
) {
    private val logger = KotlinLogging.logger {}

    // Its own mapper, so that changes to the API's JSON settings never change how entries are written or read
    private val mapper = JsonMapper.builder()
        .addModule(kotlinModule())
        .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    fun recordAsync(films: List<FilmRequest>) {
        CompletableFuture
            .runAsync({ record(films) }, taskExecutor)
            .exceptionally {
                logger.error(it) { "Cannot record ${films.size} films" }
                null
            }
    }

    fun record(films: List<FilmRequest>) {
        val entry = mapper.writeValueAsString(JournalEntry(films))
        val id = redisClient.xadd(STREAM_KEY, XAddParams.xAddParams(), mapOf(ENTRY_FIELD to entry))
        logger.info { "Recorded $id with ${films.size} films" }
    }

    /** Passes the films of each entry to [store], oldest first. Entries that can't be read or stored are reported. */
    fun replay(store: (List<FilmRequest>) -> Unit): ReplayResult {
        var replayed = 0
        val failed = mutableListOf<String>()
        for (entry in entries()) {
            try {
                store(mapper.readValue<JournalEntry>(entry.fields.getValue(ENTRY_FIELD)).films)
                replayed++
            } catch (e: Exception) {
                logger.error(e) { "Cannot replay journal entry ${entry.id}" }
                failed += entry.id.toString()
            }
        }
        logger.info { "Replayed $replayed journal entries, ${failed.size} failed" }
        return ReplayResult(replayed, failed)
    }

    fun deleteAll() {
        redisClient.del(STREAM_KEY)
    }

    private fun entries(): Sequence<StreamEntry> = sequence {
        var start = "-"
        do {
            val page = redisClient.xrange(STREAM_KEY, start, "+", PAGE_SIZE)
            yieldAll(page)
            page.lastOrNull()?.let { start = "(${it.id}" }
        } while (page.size == PAGE_SIZE)
    }

    companion object {
        const val STREAM_KEY = "filmbag:journal"
        private const val ENTRY_FIELD = "entry"
        private const val PAGE_SIZE = 100
    }
}

data class JournalEntry(val films: List<FilmRequest>)

/** [failed] holds the ids of the entries that could not be read or stored. */
data class ReplayResult(val replayed: Int, val failed: List<String>)
