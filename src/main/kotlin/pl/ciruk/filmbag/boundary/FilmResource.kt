package pl.ciruk.filmbag.boundary

import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import mu.KotlinLogging
import org.springframework.stereotype.Service
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.film.rangeOf
import pl.ciruk.filmbag.request.Journal
import pl.ciruk.filmbag.request.RequestProcessor
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

@Service
@Path("/films")
class FilmReadResource(private val filmService: FilmService) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun findAll(
        @QueryParam("yearFrom") yearFrom: Int?,
        @QueryParam("yearTo") yearTo: Int?,
        @QueryParam("scoreFrom") scoreFrom: BigDecimal?,
        @QueryParam("scoreTo") scoreTo: BigDecimal?,
        @DefaultValue(firstPage.toString()) @QueryParam("page") page: Int,
        @DefaultValue(defaultPageSize.toString()) @QueryParam("pageSize") pageSize: Int
    ): List<FilmRequest> {
        if (page < 0 || pageSize !in 1..maxPageSize) {
            throw badRequest("page must be at least 0 and pageSize between 1 and $maxPageSize")
        }

        return filmService.find(rangeOf(yearFrom, yearTo), rangeOf(scoreFrom, scoreTo), page, pageSize)
            .map { it.convertToRequest() }
    }

    companion object {
        const val firstPage = 0
        const val defaultPageSize = 10
        const val maxPageSize = 100
    }
}

@Service
@Path("/films")
class FilmWriteResource(
    private val requestProcessor: RequestProcessor,
    private val journal: Journal
) {
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    fun storeOrUpdateScores(filmRequests: List<FilmRequest>) {
        val problems = filmRequests.mapIndexedNotNull { index, filmRequest ->
            filmRequest.missingRequiredFields()
                .takeIf { it.isNotEmpty() }
                ?.let { "film $index is missing ${it.joinToString()}" }
        }
        if (problems.isNotEmpty()) {
            throw badRequest(problems.joinToString("; "))
        }

        logger.info { "Store ${filmRequests.size} films, if missing" }
        requestProcessor.storeAll(filmRequests)
        journal.recordAsync(filmRequests)
    }
}

private fun badRequest(message: String) = BadRequestException(
    Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.TEXT_PLAIN)
        .entity(message)
        .build()
)
