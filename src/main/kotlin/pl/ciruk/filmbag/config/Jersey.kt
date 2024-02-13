package pl.ciruk.filmbag.config

import org.glassfish.jersey.server.ResourceConfig
import org.springframework.context.annotation.Configuration
import pl.ciruk.filmbag.boundary.FilmReadResource
import pl.ciruk.filmbag.boundary.FilmWriteResource
import pl.ciruk.filmbag.boundary.ImportResource
import pl.ciruk.filmbag.boundary.JournalResource
import jakarta.ws.rs.ApplicationPath

@Configuration
@ApplicationPath("/resources")
class Jersey : ResourceConfig(
        FilmReadResource::class.java,
        FilmWriteResource::class.java,
        JournalResource::class.java,
        ImportResource::class.java
)
