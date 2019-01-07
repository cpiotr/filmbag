package pl.ciruk.filmbag.request

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest

@Service
class JournalSerializer {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val kryo = Kryo()

    fun serialize(requests: List<FilmRequest>): ByteArray {
        logger.debug("Serializing: ${requests.size} requests")
        val output = Output(requests.size * 1024)
        kryo.writeObject(output, requests)
        return output.toBytes()
    }

    fun deserialize(bytes: ByteArray): List<FilmRequest> {
        val typeToken: List<FilmRequest> = mutableListOf()
        val requests = kryo.readObject(Input(bytes), typeToken.javaClass)
        logger.debug("Deserialized ${requests.size} requests")
        return requests
    }
}
