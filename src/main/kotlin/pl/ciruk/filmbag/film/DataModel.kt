package pl.ciruk.filmbag.film

import java.time.ZonedDateTime
import java.util.*
import javax.persistence.*
import javax.persistence.CascadeType.ALL
import javax.persistence.GenerationType.SEQUENCE

@Entity
data class Film(
        @Id @GeneratedValue(strategy = SEQUENCE) val id: Long? = null,
        val created: ZonedDateTime = ZonedDateTime.now(),
        val title: String,
        val year: Int,
        val plot: String? = null,
        val link: String,
        val poster: String? = null,
        val score: Double,
        @OneToMany(cascade = [ALL], mappedBy = "film") val scores: MutableSet<Score> = mutableSetOf(),
        @ManyToMany val genres: Set<Genre> = setOf(),
        val hash: Int = Objects.hash(title, year, genres.map { it.name })) {
    fun addScore(grade: Double, quantity: Long, url: String?) {
        val newScore = Score(grade = grade, quantity = quantity, url = url, film = this)
        scores.add(newScore)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Film

        if (id != other.id) return false
        if (hash != other.hash) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + hash
        return result
    }
}

@Entity
data class Score(
        @Id @GeneratedValue(strategy = SEQUENCE) val id: Long? = null,
        val grade: Double,
        val quantity: Long,
        val url: String? = null,
        @ManyToOne @JoinColumn(name = "film_id") val film: Film? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Score

        if (id != other.id) return false
        if (grade != other.grade) return false
        if (quantity != other.quantity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + grade.hashCode()
        result = 31 * result + quantity.hashCode()
        return result
    }
}

@Entity
data class Genre(
        @Id @GeneratedValue(strategy = SEQUENCE) val id: Long? = null,
        val name: String)
