package pl.ciruk.filmbag.request

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.boundary.FilmRequest

@Service
class JournalSerializer {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val kryo = Kryo()

    fun serialize(requests: List<FilmRequest>): ByteArray {
        logger.info("Films: {}", requests.map { it.title })
        val output = Output(requests.size * 1024)
        kryo.writeObject(output, requests)
        return output.toBytes()
    }

    fun deserialize(bytes: ByteArray): List<FilmRequest> {
        val typeToken: List<FilmRequest> = mutableListOf()
        return kryo.readObject(Input(bytes), typeToken.javaClass)
    }
}