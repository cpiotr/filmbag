package pl.ciruk.filmbag.request

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest
import pl.ciruk.filmbag.function.runWithoutFallback
import java.lang.invoke.MethodHandles
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class Journal(
        private val redisTemplate: RedisTemplate<ByteArray, ByteArray>,
        private val journalSerializer: JournalSerializer) {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
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

        redisTemplate.opsForValue()[key] = serializedRequest
        logger.info("Record (key={}, size={})", bytesToHex(key), serializedRequest.size)
    }

    fun replay(): Sequence<List<FilmRequest>> {
        val keys = findAllKeys()
        logger.info("Replaying ${keys.size} requests")
        return redisTemplate.opsForValue()
                .multiGet(keys)
                .orEmpty()
                .asSequence()
                .map(journalSerializer::deserialize)
    }

    private fun findAllKeys() = redisTemplate.keys("*".toByteArray())

    private fun bytesToHex(hash: ByteArray): String {
        return hash.asSequence()
                .map { it.toInt() }
                .map { Integer.toHexString(0xFF and it) }
                .map { if (it.length == 1) "0$it" else it }
                .reduce { first, second -> first + second }
    }
}

