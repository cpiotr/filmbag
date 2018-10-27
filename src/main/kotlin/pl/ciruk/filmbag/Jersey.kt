package pl.ciruk.filmbag

import org.glassfish.jersey.server.ResourceConfig
import org.springframework.context.annotation.Configuration
import javax.ws.rs.ApplicationPath

@Configuration
@ApplicationPath("/resources")
class Jersey : ResourceConfig(FilmResource::class.java) {
}
