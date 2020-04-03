package pl.ciruk.filmbag.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RequestProcessorTest {
    @Test
    fun `should trim text to size`() {
        val text = "Text longer than six"
        val limit = 6

        val trimmed = trimToLimit(text, limit)

        assertThat(trimmed)
                .hasSize(limit)
                .endsWith("...")
                .startsWith("Tex")
    }

    @Test
    fun `should not trim null`() {
        val trimmed = trimToLimit(null, 100)

        assertThat(trimmed).isNull()
    }
}
