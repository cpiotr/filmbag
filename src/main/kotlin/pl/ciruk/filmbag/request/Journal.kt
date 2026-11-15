package pl.ciruk.filmbag.request

import mu.KotlinLogging
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest
import redis.clients.jedis.RedisClient
import redis.clients.jedis.params.ScanParams
import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@Service
class Journal(
    private val redisClient: RedisClient,
    private val journalSerializer: JournalSerializer,
    private val taskExecutor: TaskExecutor
) {
    private val logger = KotlinLogging.logger {}
    private val keyGenerator = AtomicLong(TimeUnit.MILLISECONDS.toNanos(Instant.now().toEpochMilli()))

    fun recordAsync(films: List<FilmRequest>) {
        // The key is taken now, so replay keeps the order of requests even if writes finish out of order
        val key = keyGenerator.incrementAndGet()
        CompletableFuture
            .runAsync({ record(key, journalSerializer.serialize(films)) }, taskExecutor)
            .exceptionally {
                logger.error(it) { "Cannot record request with ${films.size} films" }
                null
            }
    }

    fun record(films: List<FilmRequest>) {
        val serializedRequest = journalSerializer.serialize(films)
        val key = keyGenerator.incrementAndGet()

        record(key, serializedRequest)
    }

    fun deleteAll() {
        val keys = findAllKeysSerialized()
        keys.forEach { key -> redisClient.del(key) }
        logger.info { "Deleted ${keys.size} entries" }
    }

    private fun record(key: Long, serializedRequest: ByteArray) {
        redisClient.set(key.toBytes(), serializedRequest)
        logger.info("Record (key={}, size={})", key, serializedRequest.size)
    }

    fun replay(): Sequence<List<FilmRequest>> {
        val keys = findAllKeysSerialized()
        if (keys.isEmpty()) {
            logger.info { "Nothing to replay. Empty journal" }
            return emptySequence()
        }

        logger.info("Replaying ${keys.size} requests")
        return redisClient.mget(*keys)
            .asSequence()
            .map(journalSerializer::deserialize)
    }

    // SCAN walks the keyspace in steps instead of blocking Redis like KEYS, but may return a key twice
    private fun findAllKeys(): List<Long> {
        val keys = mutableSetOf<Long>()
        var cursor = ScanParams.SCAN_POINTER_START_BINARY
        do {
            val page = redisClient.scan(cursor, ScanParams().count(1000))
            page.result
                .filter { bytes -> bytes.size == Long.SIZE_BYTES }
                .mapTo(keys) { bytes -> bytes.toLong() }
            cursor = page.cursorAsBytes
        } while (!page.isCompleteIteration)
        return keys.sorted()
    }

    private fun findAllKeysSerialized(): Array<ByteArray> {
        return findAllKeys()
            .map { long -> long.toBytes() }
            .toTypedArray()
    }
}

private fun Long.toBytes(): ByteArray = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(this).array()

private fun ByteArray.toLong(): Long = ByteBuffer.wrap(this).long
