package pl.ciruk.filmbag.boundary

import org.glassfish.jersey.server.ResourceConfig
import org.springframework.context.annotation.Configuration
import jakarta.ws.rs.ApplicationPath

@Configuration
@ApplicationPath("/resources")
class Jersey : ResourceConfig(
        FilmReadResource::class.java,
        FilmWriteResource::class.java,
        JournalResource::class.java,
        ImportResource::class.java
)
