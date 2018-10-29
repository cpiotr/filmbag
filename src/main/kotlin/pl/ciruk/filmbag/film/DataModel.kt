package pl.ciruk.filmbag.film

import javax.persistence.*

@Entity
data class Film(
        @Id @GeneratedValue val id: Long? = null,
        val title: String,
        val year: Int,
        val plot: String? = null,
        val link: String,
        val poster: String? = null,
        val score: Double,
        @OneToMany(mappedBy = "film") val scores: Set<Score> = setOf(),
        @ManyToMany val genres: Set<Genre> = setOf())

@Entity
data class Score (
        @Id @GeneratedValue val id: Long? = null,
        val grade: Double,
        val quantity: Long,
        @ManyToOne @JoinColumn(name ="film_id") val film: Film? = null)

@Entity
data class Genre (
        @Id @GeneratedValue val id: Long? = null,
        val name: String)