package pl.ciruk.filmbag.boundary

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pl.ciruk.filmbag.provider.ProviderImporter
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/resources/import")
class ImportController(private val providerImporter: ProviderImporter) {
    /** Imports films from the provider, starting at page [offset], and answers once the import has finished. */
    @PostMapping("/{offset}")
    fun import(@PathVariable offset: Int): CompletableFuture<ImportResult> {
        if (offset < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "offset must be at least 0")
        }
        return providerImporter.importAsync(offset)
            .thenApply { lastPage -> ImportResult(lastImportedOffset = lastPage, nextOffset = lastPage + 1) }
    }

    /** Stops the running import after its current page. */
    @PostMapping("/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun cancel() {
        providerImporter.cancelImport()
    }
}

/** The next import continues where this one ended when it starts at [nextOffset]. */
data class ImportResult(val lastImportedOffset: Int, val nextOffset: Int)
