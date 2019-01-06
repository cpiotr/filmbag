package pl.ciruk.filmbag.request

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest
import java.lang.invoke.MethodHandles
import java.security.MessageDigest

@Service
class Journal(
        private val redisTemplate: RedisTemplate<ByteArray, ByteArray>,
        private val journalSerializer: JournalSerializer) {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    private val digest = MessageDigest.getInstance("SHA-256")

    fun record(films: List<FilmRequest>) {
        val serializedRequest = journalSerializer.serialize(films)
        val key = digest.digest(serializedRequest)

        logger.info("Record (key={}, size={})", bytesToHex(key), serializedRequest.size)
        redisTemplate.opsForValue()[key] = serializedRequest
    }

    fun replay() {
        val keys = findAllKeys()
        logger.info("Replaying ${keys.size} requests")
        redisTemplate.opsForValue()
                .multiGet(keys)
                .orEmpty()
                .map(journalSerializer::deserialize)
                .forEach { logger.info("Got ${it.size} film requests") }
    }

    private fun findAllKeys() = redisTemplate.connectionFactory
            ?.connection
            ?.keys("*".toByteArray())
            .orEmpty()

    private fun bytesToHex(hash: ByteArray): String {
        return hash.asSequence()
                .map { it.toInt() }
                .map { Integer.toHexString(0xFF and it) }
                .map { if (it.length == 1) "0$it" else it }
                .reduce { first, second -> first + second }
    }
}

