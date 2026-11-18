package pl.ciruk.filmbag.provider

import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class FilmProviderConfiguration {
    @Bean
    fun httpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .readTimeout(Duration.ofSeconds(30))
            .connectTimeout(Duration.ofSeconds(1))
            .build()
    }
}
