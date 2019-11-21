package pl.ciruk.filmbag.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager


@Configuration
@EnableWebSecurity
class Security : WebSecurityConfigurerAdapter(false) {
    override fun configure(http: HttpSecurity?) {
        http
                ?.authorizeRequests()
                ?.antMatchers("/resources/*")?.authenticated()
                ?.and()
                ?.csrf()?.ignoringAntMatchers("/resources/*")
                ?.and()
                ?.httpBasic()
    }

    @Bean
    override fun userDetailsService(): UserDetailsService {
        val users = User.withDefaultPasswordEncoder()
        val manager = InMemoryUserDetailsManager()
        val user = users.username("user").password("password").roles("USER").build()
        manager.createUser(user)
        return manager
    }
}
