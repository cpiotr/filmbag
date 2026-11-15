-- Hibernate 6+ uses one sequence per entity instead of the shared hibernate_sequence table.
-- Each sequence starts past the existing ids with room for one allocation block of 50.

SET @start = (SELECT COALESCE(MAX(id), 0) + 51 FROM film);
SET @ddl = CONCAT('CREATE SEQUENCE film_seq START WITH ', @start, ' INCREMENT BY 50');
PREPARE create_sequence FROM @ddl;
EXECUTE create_sequence;
DEALLOCATE PREPARE create_sequence;

SET @start = (SELECT COALESCE(MAX(id), 0) + 51 FROM genre);
SET @ddl = CONCAT('CREATE SEQUENCE genre_seq START WITH ', @start, ' INCREMENT BY 50');
PREPARE create_sequence FROM @ddl;
EXECUTE create_sequence;
DEALLOCATE PREPARE create_sequence;

SET @start = (SELECT COALESCE(MAX(id), 0) + 51 FROM score);
SET @ddl = CONCAT('CREATE SEQUENCE score_seq START WITH ', @start, ' INCREMENT BY 50');
PREPARE create_sequence FROM @ddl;
EXECUTE create_sequence;
DEALLOCATE PREPARE create_sequence;
