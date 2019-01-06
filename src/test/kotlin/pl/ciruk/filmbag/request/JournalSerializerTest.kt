package pl.ciruk.filmbag.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import pl.ciruk.filmbag.testFilmRequest

class JournalSerializerTest {
    private var journalSerializer = JournalSerializer()

    @Test
    fun `should serialize and deserialize the same list of requests`() {
        val filmRequests = generateSequence { testFilmRequest() }
                .take(10)
                .toList()

        val bytes = journalSerializer.serialize(filmRequests)
        val deserializedRequests = journalSerializer.deserialize(bytes)

        assertThat(deserializedRequests).isEqualTo(filmRequests)
    }
}
