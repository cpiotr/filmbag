package pl.ciruk.filmbag.provider

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertySources
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource
import java.net.URI

class FilmProviderPropertiesTest {
    @Test
    fun `should bind from environment variables`() {
        val environment = mapOf(
            "FILMBAG_PROVIDER_URL" to "http://provider/films",
            "FILMBAG_PROVIDER_FILMSPERIMPORT" to "20"
        )
        val sources = MutablePropertySources().apply {
            addFirst(SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, environment))
        }

        val properties = Binder(ConfigurationPropertySources.from(sources))
            .bindOrCreate("filmbag.provider", FilmProviderProperties::class.java)

        assertThat(properties).isEqualTo(FilmProviderProperties(URI("http://provider/films"), 20))
    }

    @Test
    fun `should reject a provider URL that is not HTTP`() {
        val source = MapConfigurationPropertySource(mapOf("filmbag.provider.url" to "provider/films"))

        assertThatThrownBy { Binder(source).bindOrCreate("filmbag.provider", FilmProviderProperties::class.java) }
            .hasRootCauseMessage("filmbag.provider.url must be an HTTP URL: provider/films")
    }

    @Test
    fun `should reject an import of no films`() {
        assertThatThrownBy { FilmProviderProperties(URI("http://provider"), filmsPerImport = 0) }
            .hasMessage("filmbag.provider.films-per-import must be positive: 0")
    }
}
