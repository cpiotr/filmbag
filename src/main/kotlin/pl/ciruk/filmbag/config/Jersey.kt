package pl.ciruk.filmbag.config

import org.glassfish.jersey.server.ResourceConfig
import org.springframework.context.annotation.Configuration
import pl.ciruk.filmbag.boundary.FilmResource
import javax.ws.rs.ApplicationPath

@Configuration
@ApplicationPath("/resources")
class Jersey : ResourceConfig(FilmResource::class.java)
