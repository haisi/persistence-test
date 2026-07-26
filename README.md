# persistence-test

[![CI](https://github.com/haisi/persistence-test/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/haisi/persistence-test/actions/workflows/ci.yml)

A JUnit 5 library for Spring Boot integration tests that detects persistence regressions - N+1 queries,
query-count drift, missing indexes, unexpected ORM behavior changes - by capturing SQL at the JDBC layer
instead of parsing Hibernate logs.

Targets Java 25, Spring Boot 4, Spring Data JPA, Hibernate 7, JdbcTemplate, and PostgreSQL.

## Status: foundation milestone

This repository currently implements the two modules everything else depends on. It is not yet usable
end-to-end from a Spring Boot test - there is no auto-configuration or AssertJ-style DSL yet. See
[Roadmap](#roadmap).

### `persistence-test-core`

Database-agnostic domain model and SQL normalization. No dependency on Hibernate, Spring, or a JDBC driver -
only [JSqlParser](https://github.com/JSqlParser/JSqlParser).

- `CapturedQuery` / `BindParameter` / `StatementType` - immutable records describing one SQL execution.
- `SqlNormalizer` (SPI) / `JSqlParserSqlNormalizer` (default impl) - turns raw SQL into a `NormalizedQuery`
  (statement type, referenced tables, and a normalized SQL rendering) that's stable across whitespace,
  comments, keyword casing, and identifier-quoting differences, while still distinguishing real semantic
  differences (joins, predicates, columns, grouping, limits). See the Javadoc on
  `JSqlParserSqlNormalizer` for the one documented gap: alias names are not yet canonicalized.

### `query-capture`

Captures every SQL statement executed through a `DataSource`, by wrapping it with
[datasource-proxy](https://github.com/ttddyy/datasource-proxy) - so capture works transparently for Spring
Data JPA, Hibernate, JdbcTemplate, and plain JDBC alike, without special-casing any of them.

- `QueryCapture.wrap(dataSource)` - wraps a `DataSource` so every statement executed through it is recorded.
- `QueryCaptureContext.current()` - thread-local accumulator of `CapturedQuery` instances; see its Javadoc
  for what it does (and does not) guarantee under concurrency and cross-thread handoff.
- `QueryCaptureExtension` (JUnit 5) - resets capture state before each test and can inject
  `QueryCaptureContext` as a test method parameter.

```java
@ExtendWith(QueryCaptureExtension.class)
class OrderRepositoryTest {

    @Test
    void findsOrdersByCustomer(QueryCaptureContext queries) {
        orderRepository.findByCustomerId(customerId);

        assertThat(queries.capturedQueries()).hasSize(1);
    }
}
```

Wire `QueryCapture.wrap(...)` around wherever your test `DataSource` bean is created (e.g. a
`@TestConfiguration` bean post-processor) until `spring-boot-autoconfigure` exists to do it automatically.

## Building

```shell
./mvnw verify
```

Runs Spotless, Checkstyle, Error Prone/NullAway, and a JaCoCo coverage gate (85% line / 75% branch -
deliberately below the 100% used by [java-lib-archetype](https://github.com/haisi/java-lib-archetype), this
project's archetype, since real defensive branches here - JDBC proxy edge cases, SPI dispatch, parser
fallbacks - don't have a meaningful test for every branch). Add `-Dquick` to skip all of that and just
compile and test.

## Roadmap

Not yet built, in dependency order:

1. **`query-analysis`** - duplicate/repeated query detection, N+1 pattern detection, query-count and
   statement statistics, execution timelines, on top of `CapturedQuery`/`NormalizedQuery`.
2. **`query-assertions`** - the AssertJ-style DSL (`assertThatQueries().selects(2).hasNoNPlusOne()...`),
   plus query filtering (ignore Flyway/Liquibase/metadata queries).
3. **`snapshot-testing`** - deterministic YAML/JSON snapshot generation from `NormalizedQuery`, delegating
   storage/diffing/approval to
   [java-snapshot-testing](https://github.com/codedabble-dev/java-snapshot-testing) via a custom
   `SnapshotSerializer`, plus the transformation pipeline (mask UUIDs/timestamps/generated IDs) and the
   `QuerySnapshotTransformer` SPI.
4. **`plan-assertions`** - PostgreSQL `EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)`-backed assertions
   (`usesIndex()`, `avoidsSequentialScan()`, ...), run inside a rolled-back transaction since `ANALYZE`
   executes the statement (including any `INSERT`/`UPDATE`/`DELETE`).
5. **`hibernate-support`** - optional entity-aware assertions (`containsSelect(Customer.class)`); the core
   modules above never depend on Hibernate.
6. **`spring-boot-autoconfigure`** - automatic `DataSource` wrapping and JUnit 5 wiring for
   `@SpringBootTest`, so none of the manual `QueryCapture.wrap(...)` plumbing above is needed.

Known limitations tracked for follow-up rather than fixed now (see the relevant Javadoc for each):

- `JSqlParserSqlNormalizer` does not canonicalize alias names.
- `QueryCaptureListener` only captures the first batch item's bind parameters for batched statements, and
  uses datasource-proxy's deprecated `getQueryArgsList()` rather than the lower-level
  `getParametersList()`/`ParameterSetOperation` API.
- `QueryCaptureContext` cannot see queries executed on a different thread than the test thread (e.g. from
  `@Async` code or an executor) without further work to propagate capture across the handoff.

## License

[Apache License, Version 2.0](LICENSE).
