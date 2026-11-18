package pl.ciruk.filmbag

import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * A test of the whole application against MariaDB and Redis in containers. Tests with this annotation and no
 * mock beans of their own share one application context, so each one starts with [TestData.deleteAll].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration::class, TestData::class)
@ActiveProfiles("test")
annotation class IntegrationTest
