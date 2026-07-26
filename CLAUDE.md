# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A JUnit 5 library for Spring Boot integration tests that detects persistence regressions — N+1 queries,
query-count drift, missing indexes, unexpected ORM behavior changes — by capturing SQL at the JDBC layer
instead of parsing Hibernate logs. Targets Java 25, Spring Boot 4, Spring Data JPA, Hibernate 7, JdbcTemplate,
and PostgreSQL. `prompt.md` has the full original design brief if you need the rationale behind a design
decision that isn't obvious from the code.

## Commands

```shell
./mvnw verify                       # full build: format check, Checkstyle, Error Prone/NullAway, tests, JaCoCo gate
./mvnw verify -Dquick                # skip all quality gates - just compile + test, for fast local iteration
./mvnw spotless:apply                # auto-format code and every pom.xml (run before committing)
./mvnw test -pl query-assertions     # run one module's tests only
./mvnw test -pl query-assertions -Dtest=QueriesAssertTest   # run a single test class
```

`plan-assertions`' integration tests need Docker (Testcontainers PostgreSQL) — expect them to fail or be
skipped without it.

Quality gates (Spotless, Checkstyle, Error Prone/NullAway, javadoc/sources jars, surefire report, JaCoCo) all
live in the `qa` Maven profile, active by default, disabled via `-Dquick`. JaCoCo enforces 85% line / 75%
branch coverage at `verify`, deliberately below 100% because several modules have real defensive branches
(JDBC proxy edge cases, SPI dispatch, parser fallbacks) that don't have a meaningful test for every branch.

Every module has an `ArchitectureTest` that enforces every package has a `package-info.java` annotated
`@NullMarked` (via `null-markeder`/ArchUnit). If it fails, it auto-generates the missing file via
`PackageInfoGenerator.main(...)` — just re-run the build.

## Architecture

Reactor with 8 modules under a strict, one-directional dependency chain — a module only depends on what's
upstream of it, never a sibling or downstream module:

```text
persistence-test-core        domain model + SQL normalization, no framework dependencies
├── query-capture             JDBC-layer capture via datasource-proxy
│   ├── query-analysis        statistics, duplicate/N+1 detection
│   │   └── query-assertions  the assertThatQueries() AssertJ DSL
│   ├── hibernate-support      entity-aware assertions (optional Hibernate dependency)
│   ├── snapshot-testing       deterministic query snapshots via java-snapshot-testing
│   └── spring-boot-autoconfigure   automatic DataSource wrapping + test wiring
└── plan-assertions            PostgreSQL EXPLAIN-backed index/scan assertions
```

- **`persistence-test-core`**: `CapturedQuery`/`BindParameter`/`StatementType` immutable records. `SqlNormalizer`
  is an SPI (`JSqlParserSqlNormalizer` is the default impl, built on JSqlParser) that turns raw SQL into a
  `NormalizedQuery` stable across whitespace/comments/casing/quoting but sensitive to real semantic
  differences. This module has zero dependency on Hibernate, Spring, or a JDBC driver.
- **`query-capture`**: wraps a `DataSource` with datasource-proxy so capture is transparent to whatever sits
  above JDBC (Spring Data JPA, Hibernate, JdbcTemplate, plain JDBC alike — nothing is special-cased).
  `QueryCaptureContext` is a thread-local accumulator (see its Javadoc for what it does *not* guarantee across
  threads/executors). `QueryCaptureExtension` is the JUnit 5 integration point.
- **`query-analysis`**: pure analyzers over `List<CapturedQuery>`, no JUnit/AssertJ dependency, so they're
  reusable outside a test assertion context.
- **`query-assertions`**: the public `assertThatQueries()` DSL, reading the ambient `QueryCaptureContext` by
  default.
- **`hibernate-support`**: the only module depending on Hibernate. Resolves entity-to-table via a live
  Hibernate `MappingMetamodel` (not re-derived from `@Table`, so custom naming strategies still resolve). A
  separate entry point (`HibernateAssertions`) rather than an extension of `query-assertions`'s `QueriesAssert`
  — Java can't retroactively add methods to another module's fluent-assertion type.
- **`snapshot-testing`**: structured snapshots (never raw SQL, never volatile data). `SnapshotNormalizer` masks
  non-deterministic literals; `QuerySnapshotSerializer` integrates with java-snapshot-testing for
  storage/diffing/approval. Repeated identical shapes collapse into one entry with a count.
- **`plan-assertions`**: `EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)`-backed assertions — derives stable facts,
  never compares raw plan text. `ANALYZE` executes the statement (including side-effecting DML), so
  `PostgresExecutionPlanAnalyzer` always runs inside a savepoint it rolls back afterward.
- **`spring-boot-autoconfigure`**: a `BeanPostProcessor` (registered via `@AutoConfiguration`) wraps the
  application's `DataSource` bean(s) automatically; a `TestExecutionListener` (registered via
  `META-INF/spring.factories`, not just `AutoConfiguration.imports`) resets `QueryCaptureContext` before every
  test method. Disable with `persistence-test.enabled=false`.

### Cross-cutting conventions

- Everything is designed around SPIs at the extension points: `SqlNormalizer`, `ExecutionPlanAnalyzer`,
  `QuerySnapshotTransformer`, database adapters. Add new database/framework support by implementing the SPI,
  not by modifying core modules.
- Root package for every module is `li.selman.persistencetest.<module-suffix>`; NullAway/ArchUnit
  (`root.package` pom property) is scoped to it per-module.
- Never compare raw SQL text or raw execution-plan text in assertions/snapshots — always derive normalized or
  structured facts first. Never make assertions based solely on execution time.
- `QueryFilters` centralizes ignoring noise (Flyway/Liquibase/Postgres-catalog queries) — use it rather than
  hand-rolled predicates when writing new tests/examples.

## Release automation

Release tooling mirrors [null-markeder](https://github.com/haisi/null-markeder) /
[jackson-jspecify](https://github.com/haisi/jackson-jspecify), adapted for this project's 8 modules (see those
repos for the single/two-module baseline this was extended from):

- `.github/workflows/release.yml` — triggered by a `v*` tag push, stages all 8 modules and hands off to
  JReleaser (`jreleaser.yml`) for signing and Maven Central deploy, then pings javadoc.io for every module.
- `.github/workflows/pages.yml` — publishes `docs/` plus **per-module** coverage/test reports
  (`/coverage/<module>/`, `/tests/<module>/`) since no single module represents the whole project.
- `./bumpPomVersion.sh` then `./release.sh` cuts a release; `./setPomVersions.sh X.Y.Z` sets the version
  directly; `./dryrun-release.sh` exercises the JReleaser sign/deploy path locally.
- CI (`ci.yml`) reports coverage to Coveralls aggregated across all 8 modules' JaCoCo XML reports.
