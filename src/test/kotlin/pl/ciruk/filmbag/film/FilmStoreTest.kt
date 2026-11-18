package pl.ciruk.filmbag.film

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class FilmStoreTest {
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
    fun `should reject limit shorter than the ellipsis`() {
        assertThatThrownBy { trimToLimit("Text", 2) }
                .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `should not trim null`() {
        val trimmed = trimToLimit(null, 100)

        assertThat(trimmed).isNull()
    }
}
