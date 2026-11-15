-- Every film query sorts by creation time
CREATE INDEX i_film_created ON film (created);
