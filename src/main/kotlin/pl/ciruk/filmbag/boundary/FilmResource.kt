package pl.ciruk.filmbag.boundary

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.film.Film
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.request.Journal
import pl.ciruk.filmbag.request.RequestProcessor
import java.lang.invoke.MethodHandles
import java.math.BigDecimal
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Service
@Transactional
@Path("/films")
class FilmReadResource(private val filmService: FilmService) {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun findAll(
            @DefaultValue(missingInt.toString()) @QueryParam("yearFrom") yearFrom: Int,
            @DefaultValue(missingInt.toString()) @QueryParam("yearTo") yearTo: Int,
            @DefaultValue(missingDecimal.toString()) @QueryParam("scoreFrom") scoreFrom: BigDecimal,
            @DefaultValue(missingDecimal.toString()) @QueryParam("scoreTo") scoreTo: BigDecimal,
            @DefaultValue(firstPage.toString()) @QueryParam("page") page: Int,
            @DefaultValue(defaultPageSize.toString()) @QueryParam("pageSize") pageSize: Int): List<FilmRequest> {
        logger.info("Find page $page of size $pageSize")

        val yearRange = createYearRange(yearFrom, yearTo)
        val scoreRange = createScoreRange(scoreFrom, scoreTo)

        return filmService.find(yearRange, scoreRange, page, pageSize)
                .sortedByDescending { it.created }
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
@Transactional
@Path("/films")
class FilmWriteResource(
        private val requestProcessor: RequestProcessor,
        private val journal: Journal) {
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    fun storeIfAbsent(filmRequests: List<FilmRequest>): Response {
        journal.recordAsync(filmRequests)
        requestProcessor.storeAll(filmRequests)
        return Response.accepted().build()
    }
}