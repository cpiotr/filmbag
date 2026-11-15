-- Genre names are matched exactly by the application, so compare them exactly here too
ALTER TABLE genre MODIFY name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

-- Merge genres that share a name into the oldest one, then forbid duplicates
CREATE TEMPORARY TABLE genre_kept AS SELECT name, MIN(id) AS id FROM genre GROUP BY name;

INSERT INTO film_genres (film_id, genres_id)
SELECT DISTINCT film_genres.film_id, genre_kept.id
FROM film_genres
JOIN genre ON genre.id = film_genres.genres_id
JOIN genre_kept ON genre_kept.name = genre.name
WHERE genre.id <> genre_kept.id
  AND NOT EXISTS (
    SELECT 1 FROM film_genres AS existing
    WHERE existing.film_id = film_genres.film_id AND existing.genres_id = genre_kept.id
  );

DELETE film_genres
FROM film_genres
JOIN genre ON genre.id = film_genres.genres_id
JOIN genre_kept ON genre_kept.name = genre.name
WHERE genre.id <> genre_kept.id;

DELETE genre
FROM genre
JOIN genre_kept ON genre_kept.name = genre.name
WHERE genre.id <> genre_kept.id;

DROP TEMPORARY TABLE genre_kept;

CREATE UNIQUE INDEX u_genre_name ON genre (name);
