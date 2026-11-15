package pl.ciruk.filmbag.config

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Starts every test context from an empty, freshly migrated schema.
 * Tests reset the context with `@DirtiesContext`, so each test method gets a clean database.
 */
@Configuration
class CleanDatabaseConfiguration {
    @Bean
    fun cleanMigrateStrategy() = FlywayMigrationStrategy { flyway ->
        flyway.clean()
        flyway.migrate()
    }
}
