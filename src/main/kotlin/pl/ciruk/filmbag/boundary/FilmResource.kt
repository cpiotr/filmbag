package pl.ciruk.filmbag.boundary

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.request.Journal
import pl.ciruk.filmbag.request.RequestProcessor
import java.math.BigDecimal
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

private val logger = KotlinLogging.logger {}

@Service
@Path("/films")
class FilmReadResource(private val filmService: FilmService) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional(readOnly = true)
    fun findAll(
        @DefaultValue(missingInt.toString()) @QueryParam("yearFrom") yearFrom: Int,
        @DefaultValue(missingInt.toString()) @QueryParam("yearTo") yearTo: Int,
        @DefaultValue(missingDecimal.toString()) @QueryParam("scoreFrom") scoreFrom: BigDecimal,
        @DefaultValue(missingDecimal.toString()) @QueryParam("scoreTo") scoreTo: BigDecimal,
        @DefaultValue(firstPage.toString()) @QueryParam("page") page: Int,
        @DefaultValue(defaultPageSize.toString()) @QueryParam("pageSize") pageSize: Int
    ): List<FilmRequest> {
        val yearRange = createYearRange(yearFrom, yearTo)
        val scoreRange = createScoreRange(scoreFrom, scoreTo)

        return filmService.find(yearRange, scoreRange, page, pageSize)
            .map { it.convertToRequest() }
    }

    private fun createScoreRange(scoreFrom: BigDecimal, scoreTo: BigDecimal): Range<BigDecimal> {
        return when (Pair(scoreFrom, scoreTo)) {
            Pair(missingDecimal.toBigDecimal(), missingDecimal.toBigDecimal()) -> EmptyRange()
            Pair(scoreFrom, missingDecimal.toBigDecimal()) -> LeftClosedRange(scoreFrom)
            Pair(missingDecimal.toBigDecimal(), scoreTo) -> RightClosedRange(scoreTo)
            else -> ClosedRange(scoreFrom, scoreTo)
        }
    }

    private fun createYearRange(yearFrom: Int, yearTo: Int): Range<Int> {
        return when (Pair(yearFrom, yearTo)) {
            Pair(missingInt, missingInt) -> EmptyRange()
            Pair(yearFrom, missingInt) -> LeftClosedRange(yearFrom)
            Pair(missingInt, yearTo) -> RightClosedRange(yearTo)
            else -> ClosedRange(yearFrom, yearTo)
        }
    }

    companion object {
        const val missingInt = -1
        const val missingDecimal = -1.0
        const val firstPage = 0
        const val defaultPageSize = 10
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
    @Transactional
    fun storeIfAbsent(filmRequests: List<FilmRequest>) {
        logger.info { "Store ${filmRequests.size} films, if missing" }
        journal.recordAsync(filmRequests)
        requestProcessor.storeAll(filmRequests)
    }
}
