package pl.ciruk.filmbag.provider

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

/** Where imports fetch films from: page `n` is `<url>/<n>`. An import stops after [filmsPerImport] films. */
@ConfigurationProperties("filmbag.provider")
data class FilmProviderProperties(
    val url: URI,
    val filmsPerImport: Int = 50
) {
    init {
        require(url.scheme == "http" || url.scheme == "https") { "filmbag.provider.url must be an HTTP URL: $url" }
        require(filmsPerImport > 0) { "filmbag.provider.films-per-import must be positive: $filmsPerImport" }
    }
}
