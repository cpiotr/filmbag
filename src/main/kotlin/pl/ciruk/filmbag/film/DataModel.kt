package pl.ciruk.filmbag.film

import java.util.*
import javax.persistence.*
import javax.persistence.CascadeType.ALL

@Entity
data class Film(
        @Id @GeneratedValue val id: Long? = null,
        val title: String,
        val year: Int,
        val plot: String? = null,
        val link: String,
        val poster: String? = null,
        val score: Double,
        @OneToMany(cascade = [ALL]) @JoinColumn(name = "film_id") val scores: Set<Score> = setOf(),
        @ManyToMany val genres: Set<Genre> = setOf(),
        val hash: Int = Objects.hash(title, year, genres.map { it.name }))

@Entity
data class Score(
        @Id @GeneratedValue val id: Long? = null,
        val grade: Double,
        val quantity: Long)

@Entity
data class Genre(
        @Id @GeneratedValue val id: Long? = null,
        val name: String)
