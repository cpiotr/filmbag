package pl.ciruk.filmbag.config

import com.zaxxer.hikari.HikariDataSource
import org.mariadb.jdbc.MariaDbDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource


@Configuration
class Database {
    @ConditionalOnMissingClass("javax.sql.DataSource")
    @Bean
    fun dataSource(
            @Value("\${spring.datasource.url}") url: String,
            @Value("\${db.username}") username: String,
            @Value("\${db.password}") password: String): DataSource {
        val ds = HikariDataSource()
        ds.maximumPoolSize = 100
        ds.dataSourceClassName = MariaDbDataSource::class.java.name
        ds.addDataSourceProperty("url", url)
        ds.addDataSourceProperty("user", username)
        ds.addDataSourceProperty("password", password)
        return ds
    }
}
