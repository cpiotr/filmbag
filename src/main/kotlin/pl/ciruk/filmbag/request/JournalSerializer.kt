package pl.ciruk.filmbag.request

import io.protostuff.LinkedBuffer
import io.protostuff.ProtostuffIOUtil
import io.protostuff.runtime.RuntimeSchema
import mu.KotlinLogging
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest


@Service
class JournalSerializer {
    private val logger = KotlinLogging.logger {}
    private val buffer = LinkedBuffer.allocate(1024)
    private val schema = RuntimeSchema.getSchema(DataWrapper::class.java)

    fun serialize(requests: List<FilmRequest>): ByteArray {
        logger.debug { "Serializing: ${requests.size} requests" }

        return try {
            ProtostuffIOUtil.toByteArray(DataWrapper(requests), schema, buffer)
        } finally {
            buffer.clear()
        }
    }

    fun deserialize(bytes: ByteArray): List<FilmRequest> {
        return try {
            val dataWrapper = schema.newMessage()
            ProtostuffIOUtil.mergeFrom(bytes, dataWrapper, schema)
            val requests = dataWrapper.requests ?: emptyList()
            logger.debug { "Deserialized ${requests.size} requests" }
            requests
        } catch (e: Exception) {
            logger.error(e) { "Could not deserialize entry" }
            emptyList()
        }
    }
}

class DataWrapper(var requests: List<FilmRequest>? = null)
