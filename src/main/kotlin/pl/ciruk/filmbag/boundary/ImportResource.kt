package pl.ciruk.filmbag.boundary

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.function.runWithoutFallback
import pl.ciruk.filmbag.request.DataLoader
import java.lang.invoke.MethodHandles
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.Suspended
import javax.ws.rs.core.Response

@Service
@Path("/import")
class ImportResource(private val dataLoader: DataLoader) {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    @GET
    @Path("/{offset}")
    fun import(
            @Suspended asyncResponse: AsyncResponse,
            @PathParam("offset") @DefaultValue("0") offset: Int?) {
        logger.info("Importing from offset: $offset")

        dataLoader.importAsync(offset ?: 0)
                .thenAccept { asyncResponse.resume(Response.accepted(it).build()) }
                .exceptionally {
                    runWithoutFallback {
                        logger.error("Cannot import data at offset $offset", it)
                        val response = Response.serverError().entity(it.message).build()
                        asyncResponse.resume(response)
                    }
                }
    }
}
