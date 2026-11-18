-- Films sharing an identity hash are one film: keep the newest, move the scores of the others to it,
-- then forbid duplicates. Genres need no moving, as they are part of the hash.
CREATE TEMPORARY TABLE film_duplicate AS
SELECT film.id, kept.id AS kept_id
FROM film
JOIN (
    SELECT id, identity_hash
    FROM (
        SELECT id, identity_hash, ROW_NUMBER() OVER (PARTITION BY identity_hash ORDER BY created DESC, id DESC) AS position
        FROM film
    ) AS ranked
    WHERE position = 1
) AS kept ON kept.identity_hash = film.identity_hash
WHERE film.id <> kept.id;

UPDATE score
JOIN film_duplicate ON film_duplicate.id = score.film_id
SET score.film_id = film_duplicate.kept_id;

-- The application treats scores with equal type, grade, quantity and url as one score
DELETE duplicate
FROM score AS duplicate
JOIN score AS kept
    ON kept.film_id = duplicate.film_id
    AND kept.type <=> duplicate.type
    AND kept.grade = duplicate.grade
    AND kept.quantity = duplicate.quantity
    AND kept.url <=> duplicate.url
    AND kept.id < duplicate.id;

DELETE film_genres
FROM film_genres
JOIN film_duplicate ON film_duplicate.id = film_genres.film_id;

DELETE film
FROM film
JOIN film_duplicate ON film_duplicate.id = film.id;

DROP TEMPORARY TABLE film_duplicate;

ALTER TABLE film MODIFY identity_hash CHAR(64) NOT NULL;
DROP INDEX i_film_identity_hash ON film;
CREATE UNIQUE INDEX u_film_identity_hash ON film (identity_hash);
