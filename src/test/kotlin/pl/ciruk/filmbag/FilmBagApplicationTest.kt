package pl.ciruk.filmbag

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import pl.ciruk.filmbag.request.DataLoader

@ExtendWith(SpringExtension::class)
@SpringBootTest
class FilmBagApplicationTest(@Autowired val context: ApplicationContext) {
    @Test
    fun shouldLoadContext() {
        val dataLoader = context.getBean(DataLoader::class.java)
        assertThat(dataLoader).isNotNull
    }
}
