package pl.ciruk.filmbag.boundary

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * Every request except health checks needs HTTP Basic credentials, configured with
 * `spring.security.user.name` and `spring.security.user.password`. CSRF protection is off
 * because clients are services without browser sessions.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class Security {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                // Jersey serves Actuator endpoints under its application path
                authorize("/resources/actuator/health/**", permitAll)
                authorize(anyRequest, authenticated)
            }
            httpBasic { }
            csrf { disable() }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
        }
        return http.build()
    }
}
