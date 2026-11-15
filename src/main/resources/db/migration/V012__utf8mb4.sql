-- MariaDB's utf8 is utf8mb3, which rejects 4-byte characters such as emoji
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE film CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE genre CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE film_genres CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE score CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
