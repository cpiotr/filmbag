# FilmBag: improvement plan

Source: team review of 2026-11-17 (architecture, performance, usability) of `master` at `214eeae` plus the
uncommitted `docker-compose.yml` change. ANALYSIS.md is the earlier baseline and is out of date in places; see
[ANALYSIS.md corrections](#analysismd-corrections). This file is the one to keep current.

**How to use this file:**
- Work top to bottom. Each step lists its files, the change, how to verify it, and the evidence behind it.
- Tick the box when a step is merged, and add a dated note under it if the outcome differed from the plan.
- Steps marked **Decision** need the owner's answer before starting.

**Measured numbers:** they come from a throwaway harness (Testcontainers MariaDB 11.4 / Redis 8.4, Hibernate
statistics, and the boot jar in `eclipse-temurin:25-jre-alpine`). The harness is not in the repo. To re-measure,
count statements with `spring.jpa.properties.hibernate.generate_statistics=true` or a `StatementInspector`.

## Decisions needed first

Decided 2026-11-18:

- **D1:** yes, move to Spring MVC.
- **D3:** snapshot from the DB after the switch, then delete the old entries.
- **D4:** verify on the Linux host before committing the compose change.
- **D5:** leave as is.
- **D2:** no external clients, so breaking changes are fine.

- [x] **D1. Jersey → Spring MVC?**
  - **Why:** Jersey 4 has no Jackson 3 support, so the app depends on Boot 4's deprecated `spring-boot-jackson2`.
    The [Boot 4.0 migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
    says that module "will be removed in a future release".
  - **Cost:** the API is four resources, about 150 lines.
  - **Gain:** Problem Details, `@WebMvcTest`, Actuator at `/actuator`, no `AsyncResponse` plumbing.
  - **Why decide first:** decide before step 4, so error handling isn't built twice.
- [x] **D2. API contract changes.**
  - **Decided:** no external clients, so make the breaking changes outright, with no compatibility shims:
    - `GET /films` returns `{"items": [...], "next": "<url of next page or null>"}` instead of a bare array.
    - `PUT /films` becomes `POST /films` (a batch upsert; RFC 9110 PUT means replace).
    - Import, replay and snapshot return **200** with an object body, since they finish before responding:
      - import: `{"lastImportedOffset", "nextOffset"}`;
      - replay: `{"replayed", "failed", "failedKeys"}`;
      - snapshot: `{"journalEntries"}`.
    - Errors use `application/problem+json`, as in step 4.
  - **Rejected:** true async import (202 plus a status resource), because imports take under a second.
    Reconsider if they get long.
  - **Docs:** update the README's curl example and add an endpoint table (step 4).
- [x] **D3. Journal format migration.**
  - **Choice:** convert the existing protostuff entries to the new format once (read old, write new), or snapshot
    from the DB after the switch and delete the old entries.
  - **Decided: snapshot from the DB.**
    - The DB is the source of truth, and old entries may include rolled-back requests (step 3), so they are not
      worth carrying over.
    - No protostuff code has to survive the switch.
    - Before deleting the old entries, take a `redis-cli --rdb` dump as a fallback.
- [x] **D4. `host.docker.internal` on the production Linux host.**
  - **Status:** uncommitted change in `docker-compose.yml`. It works on Docker Desktop.
  - **Risk:** the old value `192.168.122.1` was libvirt's `virbr0` address, so `host-gateway` may not reach
    what-to-watch there.
  - **Next step:** run one import on the Linux host before committing.
- [x] **D5. BCrypt on every authenticated request (about 52 ms, 85% of read latency).**
  - **Cause:** Spring Security upgrades Boot's `{noop}` password to BCrypt after the first login.
  - **Status:** fine at personal load. It's a security trade-off, so leave it unless latency matters.

## Steps

### 1. [x] Make the compose deployment safe (Blocker)
> **Done 2026-11-18.**
> - **Port:** the database port is no longer published. Root over TCP gets "Access denied"; root through
>   `compose exec` still works via `unix_socket`, which needs container access anyway.
> - **Volumes and Redis:** named volumes; Redis runs with `--appendonly yes`.
> - **Accounts:** the app connects as `filmbag`; root gets a random password.
> - **README:** new backup command, plus "Moving an existing install".
> - **Verified in a sandbox:**
>   - An install started from `master`'s compose was moved by following the README; the film and the journal
>     entry survived.
>   - Data survives `down`/`up`.
>   - A fresh install applies all 14 migrations as `filmbag`.
> - **Found while testing:** Redis started with `--appendonly yes` ignores an existing `dump.rdb` and starts
>   empty, so the README converts the journal first.
> - **Defaults:** `application.properties` still defaults to `root`; local defaults are step 7.
> - **Not run:** the move on the real install.

- **Files:** `docker-compose.yml`, `README.md`, `application.properties:2-3`
- **Problems:**
  - MariaDB root has no password (`MYSQL_ALLOW_EMPTY_PASSWORD=true`) and port 3306 is published on 0.0.0.0.
  - `db` and `journal` keep their data on anonymous volumes, so `docker compose down && up` starts both empty.
  - The README backup uses `mysqldump`, which isn't in `mariadb:11.4`.
- **Change:**
  - **Existing install first:** copy the current anonymous volumes into the new named ones (`docker inspect
    filmbag-db-1` shows the volume), and take a `mariadb-dump` backup before anything else.
  - **Volumes:** named `db-data:/var/lib/mysql` and `journal-data:/data`, plus a top-level `volumes:` key. Run
    Redis with `command: ["redis-server", "--appendonly", "yes"]`.
  - **Port:** remove the `ports:` entry on `db`, or bind it to `127.0.0.1:3306:3306`.
  - **Credentials:**
    - `MARIADB_USER=filmbag`, `MARIADB_PASSWORD=${DB_PASSWORD:?}`, `MARIADB_RANDOM_ROOT_PASSWORD=1`.
    - Pass the same values to the app as `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.
    - Check that `database/create-db.sql` still works for a non-root user.
  - **README:** use `docker compose exec -T db mariadb-dump --single-transaction db > backup.sql`, and put the
    backup step before "Running".
- **Verify:** `down`/`up` keeps films and journal; `mariadb -h <host> -uroot` from outside is refused.
- **Evidence:**
  - [Docker port publishing](https://docs.docker.com/engine/network/port-publishing/): "Publishing container
    ports is insecure by default".
  - [compose down](https://docs.docker.com/reference/cli/docker/compose/down/): anonymous volumes "are not
    automatically mounted by a subsequent `up`".
  - [MariaDB mysqldump](https://mariadb.com/kb/en/mysqldump/): the symlink was removed from the image from 11.0.

### 2. [x] Enforce film identity and replace the startup job (Major)
> **Done 2026-11-18.**
> - **In-batch duplicates:** `storeAll` merges them; the later copy's score wins and the scores of both are kept.
> - **V015:** a Kotlin migration bean that fills missing hashes with `identityHashOf`, which now takes genre
>   names.
> - **V016:** merges films sharing a hash into the newest, moves their scores, drops equal scores, and adds
>   `NOT NULL` + `UNIQUE u_film_identity_hash`.
> - **Startup job:** removed.
> - **Tests:** 54 pass, including:
>   - `FilmIdentityMigrationTest`: seeded duplicates, including one with the hash already set;
>   - one PUT naming a film twice.
> - **Deferred to step 3:** the retry on a unique violation. A retry has to run outside the transaction, and
>   transaction boundaries move there. Until then a request racing another to insert the same new film fails
>   with 500 instead of storing a duplicate.

- **Files:** `film/FilmService.kt:21-45,86-130`, new migrations, `RequestProcessorIntegrationTest.kt`
- **Problems:**
  - **In-batch duplicates (reproduced):** `PUT [F, F]` stores 2 rows. `storeAll` only checks the DB, and the
    `identity_hash` index from V011 is not unique.
  - **Race:** PUT and import on different threads can both insert the same film.
  - **Startup job:** `recalculateIdentityAndRemoveDuplicates` groups only films whose hash changed, so it never
    removes these duplicates. It also reads the whole table on every boot (+0.54 s per 10k films) and its delete
    path is untested.
- **Change:**
  - **Dedupe within the batch** before partitioning: `films.associateBy { it.identityHash }.values`. Merge their
    scores rather than dropping them.
  - **V015 Java migration** (`BaseJavaMigration`, registered as a bean): fill a null `identity_hash` with the
    same SHA-256 as `identityHashOf`, then merge duplicates the way V013 merges genres. Merging means keeping the
    newest film, moving scores and genres, and deleting the rest.
  - **V016 SQL:** `identity_hash` `NOT NULL`; replace `i_film_identity_hash` with
    `UNIQUE u_film_identity_hash`.
  - **Remove the startup job:** delete the `@PostConstruct` method, `selectUpdatableAndDeletable`,
    `copyIfHashChanged` and the `TransactionTemplate` dependency.
  - **Race handling:** on `DataIntegrityViolationException` for that index, retry the batch once; the retry takes
    the update path.
- **Verify:**
  - New tests: one PUT with the same film twice; the migration on a seeded DB with duplicates.
  - Boot time no longer grows with table size.
- **Evidence:**
  - [Boot data initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html): "Flyway will
    be auto-configured with any beans that implement `JavaMigration`".
  - The repo's V013 is the precedent.

### 3. [x] Make the journal trustworthy (Major)
> **Done 2026-11-18.**
> - **Format:** JSON entries `{"films": [...]}` in the Redis stream `filmbag:journal`, written with the journal's
>   own Jackson 3 mapper. Unknown properties are ignored, and offsets are kept.
> - **Recording:** PUT and import record only after `storeAll` has committed; unit tests check that nothing is
>   recorded when storing fails. The write resource has no `@Transactional` any more.
> - **Replay:** returns 200 with `{"replayed", "failed": [entry ids]}`.
> - **Retry:** `RequestProcessor` owns the transaction (`TransactionTemplate`) and retries once on a unique
>   violation, as reported by Hibernate's `ConstraintViolationException.kind == UNIQUE`. Spring translates it to
>   a plain `DataIntegrityViolationException`, not `DuplicateKeyException`. `RequestProcessorRetryTest` reproduces
>   the race.
> - **Dependencies:** protostuff removed; added `tools.jackson.module:jackson-module-kotlin` (BOM-managed), which
>   step 10 needs too.
> - **README:** documents the journal and the one-time rebuild for D3 (RDB copy, `FLUSHDB`, snapshot).
> - **Departures from the plan:**
>   - No separate `JournalFilm` type and no version field. JSON by property name already survives fields being
>     added, and the type rename in step 8 doesn't change property names. Add a version only when the format
>     really changes.
>   - The read-only `@Transactional` stays on `findAll` and `snapshot`: entities are mapped to DTOs in the
>     resource, which needs the open session. These go with steps 5d and 10.
> - **Tests:** 58 pass.

- **Files:** `request/Journal.kt`, `request/JournalSerializer.kt`, `boundary/FilmResource.kt:54,66-67`,
  `request/DataLoader.kt:97-100`, `boundary/JournalResource.kt`
- **Problems:**
  - **Recorded before commit:** PUT and import journal before `storeAll` commits, so a rolled-back request is
    replayed later.
  - **Fragile format:** protostuff's runtime schema of the API DTO depends on field declaration order, and it
    serializes `ZonedDateTime` through `sun.misc.Unsafe`, which JDK 26+ throws on.
  - **Silent loss:** `deserialize` turns any error into `emptyList()`.
  - **Keys:** an `AtomicLong` in the process, and a SCAN of the whole keyspace where any 8-byte key counts as an
    entry, so `deleteAll` can delete other apps' keys.
- **Change:**
  - **After commit:** journal after `storeAll` returns, or use `@TransactionalEventListener`.
  - **No resource transactions:** remove `@Transactional` from the Jersey resources. `RequestProcessor` already
    has a class-level one.
  - **New format:** a dedicated type, `JournalEntry(version: Int, films: List<JournalFilm>)`, serialized as JSON
    with Jackson, with `created` as ISO-8601. It decouples the journal from the API DTO.
  - **Storage:** a Redis Stream (`XADD filmbag:journal * entry <json>`). Replay pages with `XRANGE … COUNT 100`;
    delete with `DEL filmbag:journal`.
  - **Old entries:** handle them as decided in D3.
  - **Remove protostuff** dependencies afterwards.
  - **Failures:** an unreadable entry is reported (key plus error), never dropped.
  - **Retry (from step 2):** retry `storeAll` once on a violation of `u_film_identity_hash` or `u_genre_name`.
    The retry must run outside the failed transaction; the second attempt finds the stored row and updates it.
- **Verify:**
  - A PUT that fails with 400 or 500 leaves no journal entry.
  - Adding a field to the API DTO doesn't break replay of old entries.
  - Replay reports corrupted entries.
- **Evidence:**
  - [protostuff-runtime](https://protostuff.github.io/docs/protostuff-runtime/): "the field's number is ordered
    according to their declaration".
  - [JEP 498](https://openjdk.org/jeps/498): "JDK 26 or later will throw an exception".
  - [Spring transaction-bound events](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html).
  - [Redis Streams](https://redis.io/docs/latest/develop/data-types/streams/).

### 4. [ ] Return client errors as 4xx with useful bodies (Major; after D1)
- **Files:** `boundary/DataModel.kt:21-26`, `boundary/FilmResource.kt`, `boundary/ImportResource.kt`,
  `boundary/JournalResource.kt`, `request/RequestProcessor.kt:19-22`
- **Problems:**
  - **PUT:** only nulls are validated. Values longer than their columns (link and poster > 512; title and genre >
    255; plot is trimmed) fail the whole PUT with 500; `RequestProcessorIntegrationTest.kt:241` asserts this.
  - **Import:** the same bad film aborts every import at that page.
  - **Query parameters:** a malformed `?yearFrom=199x` returns 404 with an empty body.
  - **Replay:** returns 202 even when batches fail.
  - **Error bodies:** three text/plain formats.
- **Change:**
  - **Validation:** a `problems()` check covering lengths and ranges next to `missingRequiredFields`. PUT returns
    400 naming the index and field; import and replay skip and log the bad film.
  - **Malformed parameters:** `ExceptionMapper<ParamException>` returning 400 (or MVC binding, if D1 goes that
    way).
  - **Error format:** `application/problem+json` for all errors (an `ExceptionMapper`, or
    `spring.mvc.problemdetails.enabled=true` after D1).
  - **Replay result:** return `{replayed, failed, failedKeys}`, and log the key and count rather than the whole
    film list.
  - **Contract:** apply D2 (response bodies, `POST /films`, 200 instead of 202).
  - **README:** add an endpoint table (method, path, parameters, body, responses). Document that pages are
    0-based with a maximum page size of 100.
  - **Order:** do this together with step 10, so the new controllers get the new contract directly.
- **Verify:** update the 500 test to expect 400; add tests for a malformed query parameter and for replay with a
  bad entry.
- **Evidence:**
  - [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110) §15.5.1 and §15.3.3 (202 means "processing has not been
    completed").
  - [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457).
  - Jakarta REST 3.1 §3.2 (`@QueryParam` failure gives 404).

### 5. [x] Cheap performance fixes (Major/Minor, measured)
> **Done 2026-11-18.** All four sub-steps are in.
>
> **Measured** with the reviewer's harness, adapted to the new schema and journal: 20.5k films; statements are
> exact.
>
> | Operation | Before | After |
> |---|---|---|
> | Import of 500 new films | 4,596 statements, 1,249 ms | 102 statements, 540 ms |
> | Unchanged re-import | 544 statements, 206 ms | 43 statements, 123 ms |
> | GET page | 4 statements | 3 statements (no count) |
> | Replay | 23,953 statements, 5,035 ms | 969 statements, 1,346 ms |
> | Snapshot | 2,137 ms, 1 transaction holding 103k entities | 963 ms, one short transaction per page |
>
> - **5a, 5b:** done as planned.
> - **5d:**
>   - GET uses a `Slice` sorted by `(created, id)`.
>   - The snapshot pages by keyset (`findAllInPages`).
>   - `FilmService` loads scores and genres before returning, so no resource holds a transaction any more.
>   - Gotcha: `FetchableFluentQuery.slice(pageable)` ignores `sortBy(...)`, so the sort must be in the
>     `PageRequest`. `scroll` does honour `sortBy`.
> - **5c:** compose uses `MaxRAMPercentage=75` with `mem_limit: 768m`. Under that limit the JVM picks **Serial
>   GC**, not G1: ergonomics choose Serial below about 1.8 GB. GC pauses are about 1 ms.
> - **Load test on the real image:**
>   - Snapshot and replay of 20k films, with replay inserting all 20k as new: 1.8 s each.
>   - cgroup `memory.peak` was 390 MiB of 768 MiB; no OOM.
> - **Small leftover:** slices and keyset windows read `size + 1` rows, so pages of 100 trigger two genre batch
>   loads. That is harmless; it goes away with `default_batch_fetch_size` above 100, or after step 8 replaces
>   `@Fetch(JOIN)`.
> - **Tests:** 62 pass.

- [ ] **5a. Merge genres once per batch** (`RequestProcessor.kt:24-32`)
  - **Change:** `val genresByName = genreService.merge(validRequests.flatMap { it.genres }).associateBy { it.name }`,
    passed into `convertToFilm`, which looks genres up in that map instead of querying.
  - **Measured:**
    - Replay of 20.5k films: 23,953 statements and 4.7 s → 1,953 statements and 1.19 s.
    - Re-import of 500 unchanged films: 544 → 44 statements.
    - All 51 tests pass.
- [ ] **5b. JDBC batching** (`application.properties`)
  - **Change:** `hibernate.jdbc.batch_size=50`, `hibernate.order_inserts=true`, `hibernate.order_updates=true`.
  - **Measured:**
    - Import of 500 new films: 4,596 → 602 statements, 1,145 → 775 ms.
    - Combined with 5a: 102 statements, 480 ms.
    - All tests pass, and V010's `INCREMENT BY 50` already matches the allocation size.
- [ ] **5c. Compose JVM flags** (`docker-compose.yml:33-40`)
  - **Change:** drop `-Xms1G -Xmx1G -XX:+UseZGC`, keep the Dockerfile's `MaxRAMPercentage=75` (G1), and add a
    memory limit (`mem_limit: 768m`).
  - **Measured:** 1.23 GiB → 292 MiB of container memory, with the same startup.
  - **Evidence:** the
    [JDK 25 ZGC guide](https://docs.oracle.com/en/java/javase/25/gctuning/z-garbage-collector1.html) says
    uncommit is disabled when `-Xms` equals `-Xmx`.
- [ ] **5d. Snapshot** (`JournalResource.kt:36-44`, `FilmService.kt:58-62`)
  - **Problem:** `find` returns `Page.content`, so every GET and every snapshot page also runs a discarded
    `count(*)`.
  - **Change:** use `repository.findBy(spec) { it.sortBy(...).slice(pageable) }`. Page the snapshot by keyset on
    `(created, id)`, with a short transaction per page instead of one over the whole table (103k managed entities
    at 20.5k films).
  - **Measured:** today 822 statements and about 2 s for 20.5k films.
  - **Also fixes:** offset paging sorted on non-unique `created` can skip or repeat films.

### 6. [x] Test setup in Boot's current style (Major)
> **Done 2026-11-18.**
> - **Shared setup:** `@IntegrationTest` (meta-annotation) combines `@SpringBootTest(RANDOM_PORT)`,
>   `TestcontainersConfiguration` (`@ServiceConnection` MariaDB plus a Redis container, wired through
>   `DynamicPropertyRegistrar`) and the `test` profile. It also imports `TestData`, which deletes all data, after
>   waiting for background journal writes.
> - **Removed:**
>   - the `@Primary` mocks of `DataLoader` and `RedisClient`, now unnecessary with a real Redis;
>   - `@DirtiesContext`, `CleanDatabaseConfiguration` and the per-class containers;
>   - `FilmBagApplicationTest`, whose `getBean != null` could not fail.
> - **Config:** `application-test.properties` only overrides; the main properties now load in tests. The app class
>   is a plain `@SpringBootApplication`. Added `spring-boot-testcontainers`.
> - **Local runs:** `./gradlew bootTestRun` (`TestFilmBagApplication`) runs the app against containers; verified
>   healthy.
> - **Result:** 61 tests, 2 Spring contexts (shared, plus the retry test's spy) instead of one per test method;
>   the build takes 23 s instead of 47 s.
> - **Moved to step 10:** end-to-end tests of `/import` and `/journal/*` over HTTP, because step 10 rewrites
>   those endpoints and their contract. Snapshot and replay are already covered at the bean level.

- **Files:** all of `src/test`, `FilmBagApplication.kt:6-11`, `src/test/resources/application.properties`
- **Problems:**
  - **Context restarts:** `@DirtiesContext(AFTER_EACH_TEST_METHOD)` in four classes, five hand-rolled containers,
    and copy-pasted `@DynamicPropertySource`.
  - **Scanning tricks:** test-only `CleanDatabaseConfiguration` lives in the production `config` package. A test
    class named `TestConfiguration` shadows Boot's annotation.
  - **Shadowed config:** the test `application.properties` shadows the main one, so production config is never
    loaded in tests.
- **Change:**
  - **Shared containers:** `@TestConfiguration(proxyBeanMethods = false) class TestcontainersConfiguration` with
    `@Bean @ServiceConnection fun mariaDb() = MariaDBContainer("mariadb:11.4")` and a Redis container bean, the
    pattern start.spring.io generates for Boot 4.1.2. Tests `@Import` it.
  - **Mocks:** `@MockitoBean` instead of `@Primary` mocks.
  - **Reset:** truncate tables in `@BeforeEach` instead of `@DirtiesContext`.
  - **Config:** rename the test properties to `application-test.properties` with `@ActiveProfiles("test")`, and
    return to a plain `@SpringBootApplication`.
  - **Local runs:** add `TestFilmBagApplication` (`SpringApplication.from(::main).with(...)`) so the app runs
    locally against containers.
  - **Fix test smells:**
    - typos: `filService`, `redisProperties` (it only sets the datasource);
    - `@AfterEach fun setUp()` runs after each test;
    - `getBean != null` can't fail;
    - the class name `JournalTest` doesn't match its file.
  - **New tests:** end-to-end tests for `/import` and `/journal/*`.
- **Verify:** `./gradlew build` time drops (baseline 51 tests in about 50 s); all tests green.
- **Evidence:**
  - [Boot Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html).
  - [Boot testing](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html): a
    top-level `@TestConfiguration` "should not be picked up by scanning".

### 7. [x] Configuration and operations (Minor)
> **Done 2026-11-18.**
> - **Typed settings:** `FilmProviderProperties` (`filmbag.provider.url`, `films-per-import`, checked at
>   startup) and `JournalProperties` (`filmbag.journal.host`, `port`, `max-connections`). They live beside their
>   users in `request`, with `ClientConfiguration` holding the Redis and OkHttp clients.
> - **Removed:** `Connections.kt`, `configurations.kt` and the startup logging of settings. `request` no longer
>   depends on `config`.
> - **Local defaults:** `localhost` and user `filmbag`; no private IPs.
> - **Compose:**
>   - new variable names;
>   - the provider URL comes from `${FILM_PROVIDER_URL:-…}`, with `.env` gitignored;
>   - `stop_grace_period: 40s`.
> - **README:** a table of the variables.
> - **Gotcha:** environment variable names drop dashes, so the variable is `FILMBAG_PROVIDER_FILMSPERIMPORT`.
>   `FilmProviderPropertiesTest` checks that it binds.
> - **Jedis pool:** the 100 ms idle eviction override is gone.
> - **Breaking for deployments:** `REDIS_HOST` and `EXTERNAL_PROVIDER_FILMREQUEST_*` are now `FILMBAG_JOURNAL_HOST`
>   and `FILMBAG_PROVIDER_*`.
> - **Not done:** `spring-boot-configuration-processor`. For Kotlin it needs kapt, which is in maintenance mode
>   and slows the build, and its only gain is IDE completion.
> - **Verified:** smoke test on the compose stack (PUT, journal entry, replay). 64 tests pass.

- **Typed properties:** replace the six `@Value`s with `@Validated @ConfigurationProperties` data classes
  (`filmbag.provider.url: URI`, `filmbag.provider.films-per-import`, `filmbag.redis.host/port`) and add
  `spring-boot-configuration-processor`. Delete `logConfiguration` and `configurations.kt`.
  - Evidence: [Boot external config](https://docs.spring.io/spring-boot/reference/features/external-config.html),
    which recommends `@ConfigurationProperties` and notes `@Value` has no metadata.
- **Local defaults:** default to `localhost` instead of `172.19.0.x` (`application.properties:1,19`).
- **Provider URL:** `${FILM_PROVIDER_URL:-http://host.docker.internal:8080/resources/suggestions}` in compose,
  with site values in an untracked `.env`. This depends on D4.
- **Shutdown:** `stop_grace_period: 40s` on `filmbag`, because compose's 10 s default cuts off the 30 s
  graceful-shutdown wait.
- **Jedis pool:** drop `minEvictableIdleDuration = 100ms` (`Connections.kt:32`). It drops idle Redis connections
  every 30 s.
- **Open-in-view:** `spring.jpa.open-in-view=false` does nothing without Spring MVC. Keep it if D1 goes to MVC.

### 8. [ ] Names and package structure (Major/Minor)
- **Package cycles:** `boundary` ↔ `request` and `request` → `config` → `boundary`.
  - Move `FilmRequest` and `ScoreRequest` out of `boundary`, and `Jersey.kt` into `boundary`.
  - Evidence: [Spring Modulith verification](https://docs.spring.io/spring-modulith/reference/verification.html)
    ("No cycles").
- **Renames:**
  - `FilmRequest` → `FilmData` (it is also the response, journal record and provider payload).
  - `DataLoader` → `ProviderImporter`, `RequestProcessor` → `FilmImporter`, `DataWrapper` → gone (step 3).
  - `Connections` → `ClientConfiguration`.
  - Parameters named `it` → `films` (`JournalResource.kt:48`) and `film` (`FilmService.kt:123`).
  - Jersey resources: `@Service` → `@Component`.
- **Simplify `FilmService.storeAll`:** replace the mutating `copyScoreFrom` lambda inside `filter` with a plain
  loop over `updateScoresFrom(film)`. Drop the `saveAll` of already-managed entities; dirty checking flushes them.
- **JPA entities:**
  - plain classes instead of `data class`;
  - `allOpen` for `@Entity`/`@MappedSuperclass`/`@Embeddable`;
  - `Score.film` non-null with no cascade to the parent;
  - an entity graph on `find` instead of `@Fetch(JOIN)`.

### 9. [x] Build (Minor)
> **Done 2026-11-18, before step 8, because step 8's entity changes rely on `allOpen`.**
> - **Plugins:** the plugins DSL, with versions in `settings.gradle` `pluginManagement` taken from
>   `gradle.properties`.
> - **Build file:** a top-level `kotlin { compilerOptions }` block and an `allOpen` block for entities.
> - **Toolchain:** the foojay toolchain resolver (1.0.0) downloads JDK 25 when it is missing.
> - **Logging:** `io.github.oshai:kotlin-logging-jvm:8.0.7` replaces microutils 1.7.6; it only accepts lambda
>   messages.
> - **Tests:** the explicit JUnit API and engine are gone; `junit-platform-launcher` is declared, as the generator
>   does.
> - **Removed:** the configuration-time `println`.
> - **CI:** builds the Docker image, without pushing it; verified locally. 64 tests pass.

- **Plugins DSL:** `plugins { kotlin("jvm"); kotlin("plugin.spring"); kotlin("plugin.jpa");
  id("org.springframework.boot") }`, a top-level `kotlin { compilerOptions { … } }`, and the `allOpen {}` block.
- **Logging library:** `io.github.microutils:kotlin-logging:1.7.6` → `io.github.oshai:kotlin-logging-jvm`; the
  artifact moved.
- **Cleanup:** drop the explicit `junit-jupiter-api`/`-engine` (the starter brings them) and the configuration-time
  `println`.
- **Toolchain:** add the `org.gradle.toolchains.foojay-resolver-convention` plugin in `settings.gradle`, or list
  JDK 25 + Docker as prerequisites in the README.
- **CI:** build the Docker image (no push), so the README's compose path is tested.

### 10. [ ] Jersey → Spring MVC (if D1 = yes)
- **Change:** `@RestController`s with `ResponseStatusException` and `spring.mvc.problemdetails.enabled=true`.
- **Removals:** drop `spring-boot-starter-jersey`, `spring-boot-jackson2` and `Jersey.kt`. Move `DataLoader` to
  Jackson 3, which it can do even without D1.
- **Paths:** keep the `/resources` prefix with `spring.mvc.servlet.path=/resources`, or move Actuator to
  `/actuator` and update the security matcher, Dockerfile `HEALTHCHECK` and README.
- **Async import:** replace `AsyncResponse` and `ConnectionCallback`. It is unclear whether that callback ever
  fires, since the response is only written at the end.

### 11. [ ] Faster startup (Minor, measured)
- **Change:** extract with `--layers` but without `--launcher`, train a JDK 25 AOT cache in the build stage
  (`-XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh`), and run
  `java -XX:AOTCache=app.aot -jar filmbag.jar`.
- **Measured:** 3.8 s → 1.4 s; the cache is 132 MB.
- **Evidence:** [Boot AOT cache](https://docs.spring.io/spring-boot/reference/packaging/aot-cache.html).

## Open questions (not yet steps)

- **Time-zone storage:** `@TimeZoneStorage(NATIVE)` on `DATETIME(6)` (`film/DataModel.kt:19`). Hibernate
  documents NATIVE as an error unless the dialect supports time zones natively, and MariaDB's doesn't. The stored
  value may depend on the driver or JVM time zone. Consider `Instant` + `hibernate.jdbc.time_zone=UTC`.
- **Not verified:**
  - peak memory during snapshot or replay;
  - the concurrent PUT/import race (traced in code, not reproduced);
  - CI `permissions:`.

## ANALYSIS.md corrections

| ANALYSIS.md says | Actual state (2026-11-17) |
|---|---|
| B5: CI publishes to GHCR | Release job removed in `214eeae` |
| 32-bit hash fixed | Collisions fixed, but uniqueness not enforced (step 2) |
| `@PostConstruct` fixed | Still runs every boot and misses duplicates (step 2) |
| N+1 fixed | Reads: verified 82 → 4 statements. Writes: still one genre query per film (5a) |
| Package cycle fixed | `boundary` ↔ `request` remains (step 8) |
| Only snapshot transaction open | All resources hold transactions; writes journal before commit (step 3) |
| protostuff: replace before a JDK removes `Unsafe` | JDK 26 throws by default (step 3) |
| Dependencies fixed | microutils kotlin-logging 1.7.6 remains (step 9) |
| `scanBasePackages` keeps test config out | `CleanDatabaseConfiguration` is deliberately in the scan (step 6) |
| Tests don't run migrations / version drift fixed | Main `application.properties` never loads in tests (step 6) |
| Hardcoded addresses | Still `172.19.0.x` defaults (step 7) |

**Verified as correct:** V014 index used; SCAN, not KEYS; V010 sequences match the allocation size; the
`default_batch_fetch_size` gain.

## Keep

- V010 and V013 migrations: sequences above existing ids; dedupe before the unique index.
- `ddl-auto=validate` against the real migrations on the production MariaDB version.
- SHA-256 identity with a unit separator and sorted genre names.
- Per-import cancel flag with `compareAndSet` (clean 409).
- `DataLoaderTest` uses a real `HttpServer` instead of mocking OkHttp.
