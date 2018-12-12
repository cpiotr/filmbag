package pl.ciruk.filmbag.request

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import pl.ciruk.filmbag.boundary.FilmRequest
import java.lang.invoke.MethodHandles
import java.security.MessageDigest

@Component
class RequestRecorder(private val redisTemplate: RedisTemplate<ByteArray, ByteArray>) {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    private val digest = MessageDigest.getInstance("SHA-256")

    private val kryo = Kryo()

    fun record(films: List<FilmRequest>) {
        val serializedRequest = serialize(films)
        val key = digest.digest(serializedRequest)

        logger.info("Record (key={}, size={})", bytesToHex(key), serializedRequest.size)
        redisTemplate.opsForValue()[key] = serializedRequest
    }

    private fun serialize(films: List<FilmRequest>): ByteArray {
        val output = Output(films.size * 512)
        kryo.writeObject(output, films)
        return output.toBytes()
    }

    private fun bytesToHex(hash: ByteArray): String {
        return hash.asSequence()
                .map { it.toInt() }
                .map { Integer.toHexString(0xFF and it) }
                .map { if (it.length == 1) "0$it" else it }
                .reduce { first, second -> first + second }
    }
}