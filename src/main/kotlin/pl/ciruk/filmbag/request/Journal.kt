package pl.ciruk.filmbag.request

import mu.KotlinLogging
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.function.runWithoutFallback
import redis.clients.jedis.JedisPool
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class Journal(
        private val redisPool: JedisPool,
        private val journalSerializer: JournalSerializer) {
    private val logger = KotlinLogging.logger {}
    private val threadPool = Executors.newSingleThreadExecutor()
    private val digest = MessageDigest.getInstance("SHA-256")

    fun recordAsync(films: List<FilmRequest>) {
        CompletableFuture
                .runAsync(Runnable { record(films) }, threadPool)
                .exceptionally { runWithoutFallback { logger.error("Cannot record $it") } }
    }

    fun record(films: List<FilmRequest>) {
        val serializedRequest = journalSerializer.serialize(films)
        val key = digest.digest(serializedRequest)

        redisPool.resource.use {
            it.set(key, serializedRequest)
        }
        logger.info("Record (key={}, size={})", bytesToHex(key), serializedRequest.size)
    }

    fun replay(): Sequence<List<FilmRequest>> {
        val keys = findAllKeys()
        if (keys.isEmpty()) {
            logger.info { "Nothing to replay. Empty journal" }
            return emptySequence()
        }

        logger.info("Replaying ${keys.size} requests")
        redisPool.resource.use {
            return it.mget(*keys)
                    .asSequence()
                    .map(journalSerializer::deserialize)

        }
    }

    private fun findAllKeys(): Array<ByteArray> {
        redisPool.resource.use {
            val allKeys = "*".toByteArray()
            return it.keys(allKeys).toTypedArray()

        }
    }

    private fun bytesToHex(hash: ByteArray): String {
        return hash.asSequence()
                .map { it.toInt() }
                .map { Integer.toHexString(0xFF and it) }
                .map { if (it.length == 1) "0$it" else it }
                .reduce { first, second -> first + second }
    }
}

