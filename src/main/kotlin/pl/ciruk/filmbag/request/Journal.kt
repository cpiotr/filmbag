package pl.ciruk.filmbag.request

import mu.KotlinLogging
import okhttp3.internal.and
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.function.runWithoutFallback
import redis.clients.jedis.JedisPool
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class Journal(
    private val redisPool: JedisPool,
    private val journalSerializer: JournalSerializer
) {
    private val logger = KotlinLogging.logger {}
    private val threadPool = Executors.newSingleThreadExecutor()
    private val digest = MessageDigest.getInstance("SHA-256")

    fun recordAsync(films: List<FilmRequest>) {
        CompletableFuture
            .runAsync(Runnable { record(films) }, threadPool)
            .exceptionally { runWithoutFallback { logger.error(it) { "Cannot record request with ${films.size} films" } } }
    }

    fun record(films: List<FilmRequest>) {
        val serializedRequest = journalSerializer.serialize(films)
        val key = Instant.now().toEpochMilli()

        redisPool.resource.use {
            it.set(key.toBytes(), serializedRequest)
        }
        logger.info("Record (key={}, size={})", key, serializedRequest.size)
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
            return it.keys(allKeys)
                .map { bytes -> bytes.toLong() }
                .sorted()
                .map { long -> long.toBytes() }
                .toTypedArray()
        }
    }

    fun Long.toBytes(): ByteArray {
        var l = this
        val result = ByteArray(Long.SIZE_BYTES)
        for (i in Long.SIZE_BYTES - 1 downTo 0) {
            result[i] = (l and 0xFF).toByte()
            l = l shr Byte.SIZE_BITS
        }
        return result
    }

    fun ByteArray.toLong(): Long {
        var result: Long = 0
        for (i in 0 until Long.SIZE_BYTES) {
            result = result shl Byte.SIZE_BITS
            val byte = this[i]
            result = result or ((byte and 0xFF).toLong())
        }
        return result
    }
}

