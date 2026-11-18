package pl.ciruk.filmbag.boundary

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.ciruk.filmbag.film.FilmService
import pl.ciruk.filmbag.film.FilmStore
import pl.ciruk.filmbag.film.toFilmData
import pl.ciruk.filmbag.journal.Journal
import pl.ciruk.filmbag.journal.ReplayResult

@RestController
@RequestMapping("/resources/journal")
class JournalController(
    private val filmStore: FilmStore,
    private val journal: Journal,
    private val filmService: FilmService
) {
    /** Stores the films of every journal entry again, oldest first. */
    @PostMapping("/replay")
    fun replay(): ReplayResult {
        return journal.replay(filmStore::storeAll)
    }

    /** Records every stored film in the journal. */
    @PostMapping("/snapshot")
    fun snapshot(): SnapshotResult {
        var entries = 0
        for (films in filmService.findAllInPages(SNAPSHOT_PAGE_SIZE)) {
            journal.record(films.map { it.toFilmData() })
            entries++
        }
        return SnapshotResult(entries)
    }

    companion object {
        private const val SNAPSHOT_PAGE_SIZE = 100
    }
}

data class SnapshotResult(val journalEntries: Int)
