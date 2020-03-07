package pl.ciruk.filmbag.film

import java.time.ZonedDateTime
import java.util.*
import javax.persistence.*
import javax.persistence.CascadeType.*
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
        var score: Double,

        @OneToMany(cascade = [ALL], mappedBy = "film", orphanRemoval = true)
        val scores: MutableSet<Score> = mutableSetOf(),

        @ManyToMany
        @JoinTable(name = "film_genres",
                joinColumns = [JoinColumn(name = "film_id")],
                inverseJoinColumns = [JoinColumn(name = "genres_id")])
        val genres: Set<Genre> = mutableSetOf(),

        val hash: Int = Objects.hash(title, year, link)
) {
    fun addScore(grade: Double, quantity: Long, type: ScoreType, url: String?): Boolean {
        val newScore = Score(grade = grade, quantity = quantity, url = url, type = type, film = this)
        for (score in scores) {
            if (score.hasSameProperties(newScore)) {
                return false
            }
        }
        return scores.add(newScore)
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

    override fun toString(): String {
        return "Film(id=$id, title='$title', year=$year)"
    }
}

@Entity
data class Score(
        @Id @GeneratedValue(strategy = SEQUENCE) val id: Long? = null,
        @Enumerated(EnumType.STRING) val type: ScoreType,
        val grade: Double,
        val quantity: Long,
        val url: String? = null,
        @ManyToOne(cascade = [PERSIST, MERGE]) @JoinColumn(name = "film_id") val film: Film? = null) {
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

    fun hasSameProperties(other: Score) = Objects.equals(grade, other.grade)
            && Objects.equals(quantity, other.quantity)
            && Objects.equals(type, other.type)
            && Objects.equals(url, other.url)
}

@Entity
data class Genre(
        @Id @GeneratedValue(strategy = SEQUENCE) val id: Long? = null,
        val name: String)

enum class ScoreType {
    AMATEUR,
    CRITIC,
    UNKNOWN,
    ;
}
