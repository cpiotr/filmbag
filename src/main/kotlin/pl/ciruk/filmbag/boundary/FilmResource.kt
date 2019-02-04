package pl.ciruk.filmbag.boundary

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.ciruk.filmbag.film.Film
import pl.ciruk.filmbag.film.FilmService
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Service
@Transactional
@Path("/films")
class FilmResource(private val filmService: FilmService) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun findAll(
            @DefaultValue(missingInt.toString()) @QueryParam("yearFrom") yearFrom: Int,
            @DefaultValue(missingInt.toString()) @QueryParam("yearTo") yearTo: Int,
            @DefaultValue(missingDouble.toString()) @QueryParam("scoreFrom") scoreFrom: Double,
            @DefaultValue(missingDouble.toString()) @QueryParam("scoreTo") scoreTo: Double
    ): List<FilmRequest> {
        val yearRange: Range<Int> = when (Pair(yearFrom, yearTo)) {
            Pair(missingInt, missingInt) -> EmptyRange()
            Pair(yearFrom, missingInt) -> LeftClosedRange(yearFrom)
            Pair(missingInt, yearTo) -> RightClosedRange(yearTo)
            else -> ClosedRange(yearFrom, yearTo)
        }

        return filmService.find(yearRange, EmptyRange())
                .map { it.convertToRequest() }
    }

    companion object {
        const val missingInt = -1
        const val missingDouble = -1.0
    }
}
