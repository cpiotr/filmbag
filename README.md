# filmbag
Bag of films

## Building
Needs JDK 25 and Docker, which the tests use to run MariaDB and Redis:
```
./gradlew clean build
```

To run the application locally against MariaDB and Redis in containers, on port 18080:
```
./gradlew bootTestRun
```
The password of user `user` is printed at startup (`Using generated security password`).

## Running
The API uses HTTP Basic authentication. Set its password (and optionally the user, `filmbag` by default) and the
database password before starting. Moving an install that was started with an older `docker-compose.yml`? Follow
[Moving an existing install](#moving-an-existing-install) first.
```
export FILMBAG_PASSWORD=...
export DB_PASSWORD=...
docker compose build
docker compose up
curl -u "filmbag:$FILMBAG_PASSWORD" http://localhost:18080/resources/films
```

The database and the journal keep their data in the named volumes `filmbag_db-data` and `filmbag_journal-data`,
which survive `docker compose down`. The database port is not published; use
`docker compose exec db sh -c 'mariadb -u"$MARIADB_USER" -p"$MARIADB_PASSWORD" db'` for a SQL shell.

The application applies database migrations itself at startup. Health checks need no credentials:
`http://localhost:18080/resources/actuator/health` (also `/liveness` and `/readiness`).

The image is configured with environment variables; see `docker-compose.yml` and `application.properties`:

| Variable | Meaning | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD` | Database connection | `jdbc:mariadb://localhost:3306/db`, `filmbag` |
| `SPRING_SECURITY_USER_NAME`, `_PASSWORD` | API credentials | |
| `FILMBAG_JOURNAL_HOST`, `FILMBAG_JOURNAL_PORT` | Redis holding the journal | `localhost`, `6379` |
| `FILMBAG_PROVIDER_URL` | Film provider for imports; page `n` is read from `<url>/<n>` | `http://localhost:8080/resources/suggestions` |
| `FILMBAG_PROVIDER_FILMSPERIMPORT` | An import stops after this many films | `50` |
| `JDK_JAVA_OPTIONS` | JVM options | `-XX:MaxRAMPercentage=75` |

With compose, set `FILM_PROVIDER_URL` in an `.env` file next to `docker-compose.yml` to change the provider.

## Backing up
```
docker compose exec -T db sh -c 'mariadb-dump -u"$MARIADB_USER" -p"$MARIADB_PASSWORD" --single-transaction db' > filmbag-backup.sql
```

`docker-compose.yml` runs MariaDB 11.4 with `MARIADB_AUTO_UPGRADE=1`, so a data directory created by an older
MariaDB (such as 10.4) is upgraded on first start. Take a backup before starting a new MariaDB version.

## Moving an existing install
Older versions of `docker-compose.yml` kept the data in anonymous volumes, connected as a passwordless `root` and
published the database port. Moving such an install to the current file, with the old version still running:

1. Back up the database, and note the volumes that hold the data:
   ```
   docker compose exec -T db mariadb-dump -uroot --single-transaction db > filmbag-backup.sql
   volume_of() { docker inspect -f "{{range .Mounts}}{{if eq .Destination \"$2\"}}{{.Name}}{{end}}{{end}}" "$1"; }
   old_db=$(volume_of filmbag-db-1 /var/lib/mysql)
   old_journal=$(volume_of filmbag-journal-1 /data)
   echo "$old_db $old_journal"
   ```
   On MariaDB 10.x use `mysqldump` instead of `mariadb-dump`. Check that both volume names are printed.
2. Stop it, update the files, and create the new volumes without starting anything:
   ```
   docker compose stop
   git pull
   export FILMBAG_PASSWORD=... DB_PASSWORD=...
   docker compose create
   ```
3. Copy the data into the new volumes:
   ```
   docker run --rm -v "$old_db":/from -v filmbag_db-data:/to alpine cp -a /from/. /to/
   docker run --rm -v "$old_journal":/from -v filmbag_journal-data:/to alpine cp -a /from/. /to/
   ```
4. Convert the journal to append-only files. Redis started with `--appendonly yes` ignores an existing
   `dump.rdb`, so load it once without that option and let Redis write the append-only files:
   ```
   docker run -d --name journal-convert -v filmbag_journal-data:/data redis:8.4-alpine
   sleep 2
   docker exec journal-convert redis-cli config set appendonly yes
   sleep 5   # until `redis-cli info persistence` shows aof_rewrite_in_progress:0
   docker exec journal-convert redis-cli shutdown
   docker rm journal-convert
   ```
5. Create the application's database user and replace the empty root passwords. The `MARIADB_*` settings only
   apply to an empty data directory, so do it by hand:
   ```
   docker compose up -d --wait db
   docker compose exec db sh -c '
     root_password=$(head -c 32 /dev/urandom | base64 | tr -dc A-Za-z0-9)
     mariadb -uroot <<SQL
   CREATE USER IF NOT EXISTS \`$MARIADB_USER\`@\`%\` IDENTIFIED BY "$MARIADB_PASSWORD";
   GRANT ALL PRIVILEGES ON db.* TO \`$MARIADB_USER\`@\`%\`;
   ALTER USER IF EXISTS root@localhost, root@\`%\` IDENTIFIED BY "$root_password";
   SQL'
   ```
   Root then has a random password nobody knows, as in a fresh install.
6. Start everything, and check that the films and the journal are there:
   ```
   docker compose up -d
   curl -u "filmbag:$FILMBAG_PASSWORD" http://localhost:18080/resources/films
   docker compose exec journal redis-cli dbsize
   ```
   Once this works, delete the old volumes with `docker volume rm "$old_db" "$old_journal"`.

## The journal
Every stored list of films is also recorded, after it is stored, as a JSON entry in the Redis stream
`filmbag:journal`. `POST /resources/journal/replay` stores all entries again, oldest first, and answers with the
number replayed and the ids of entries that failed. `POST /resources/journal/snapshot` records every film in the
database.

Versions before the stream kept entries as binary protostuff values under 8-byte keys, which current versions
don't read. After upgrading from such a version, rebuild the journal from the database once:
```
docker compose exec journal redis-cli save
docker compose exec journal cp /data/dump.rdb /data/journal-before-stream.rdb
docker compose exec journal redis-cli flushdb
curl -u "filmbag:$FILMBAG_PASSWORD" -X POST http://localhost:18080/resources/journal/snapshot
```
`journal-before-stream.rdb` stays in the `journal-data` volume in case the old entries are needed.
