package pl.ciruk.filmbag.journal

import org.springframework.boot.context.properties.ConfigurationProperties

/** The Redis server that holds the journal. */
@ConfigurationProperties("filmbag.journal")
data class JournalProperties(
    val host: String = "localhost",
    val port: Int = 6379,
    val maxConnections: Int = 8
)
