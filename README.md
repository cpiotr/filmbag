# filmbag
Bag of films

## Building
```
./gradlew clean build
```

## Running
The API uses HTTP Basic authentication. Set the password (and optionally the user, `filmbag` by default) before starting:
```
export FILMBAG_PASSWORD=...
docker-compose build
docker-compose up
curl -u "filmbag:$FILMBAG_PASSWORD" http://localhost:18080/resources/films
```

The application applies database migrations itself at startup. Health checks need no credentials:
`http://localhost:18080/resources/actuator/health` (also `/liveness` and `/readiness`).

The image is configured with Spring environment variables (for example `SPRING_DATASOURCE_URL`, `REDIS_HOST`,
`EXTERNAL_PROVIDER_FILMREQUEST_URL`) and JVM options in `JDK_JAVA_OPTIONS`; see `docker-compose.yml`.

## Releasing
Push a tag such as `v1.2.3`. After the build passes, CI publishes the image as `ghcr.io/cpiotr/filmbag:1.2.3`.

## Upgrading the database
`docker-compose.yml` runs MariaDB 11.4 with `MARIADB_AUTO_UPGRADE=1`, so a data directory created by an older
MariaDB (such as 10.4) is upgraded on first start. Back up the database before the first start on a new version:
```
docker-compose exec db mysqldump -uroot --single-transaction db > filmbag-backup.sql
```
