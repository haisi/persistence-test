# persistence-test

[![CI](https://github.com/haisi/persistence-test/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/haisi/persistence-test/actions/workflows/ci.yml)
[![Coverage Status](https://coveralls.io/repos/github/haisi/persistence-test/badge.svg?branch=main)](https://coveralls.io/github/haisi/persistence-test?branch=main)
[![License](https://img.shields.io/github/license/haisi/persistence-test)](LICENSE)

[**Website**](https://haisi.github.io/persistence-test/)

A JUnit 5 library for Spring Boot integration tests that detects persistence regressions - N+1 queries,
query-count drift, missing indexes, unexpected ORM behavior changes - by capturing SQL at the JDBC layer
instead of parsing Hibernate logs.

Targets Java 25, Spring Boot 4, Spring Data JPA, Hibernate 7, JdbcTemplate, and PostgreSQL.

## Quickstart (Spring Boot)

```xml
<dependency>
    <groupId>li.selman</groupId>
    <artifactId>spring-boot-autoconfigure</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>li.selman</groupId>
    <artifactId>query-assertions</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

```java
import static li.selman.persistencetest.assertions.QueryAssertions.assertThatQueries;

@SpringBootTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void findsOrdersByCustomerWithoutNPlusOne() {
        orderRepository.findByCustomerId(customerId);

        assertThatQueries().selects(1).containsNoDelete().hasNoNPlusOne();
    }
}
```

No manual `DataSource` wrapping and no `@ExtendWith` needed: the `DataSource` bean is wrapped automatically,
and capture state resets before every test method.

Without Spring Boot, wire `QueryCapture.wrap(...)` around your `DataSource` and add
`@ExtendWith(QueryCaptureExtension.class)` yourself - see `query-capture` below.

## Modules

```text
persistence-test-core        domain model + SQL normalization, no framework dependencies
├── query-capture             JDBC-layer capture via datasource-proxy
│   ├── query-analysis        statistics, duplicate/N+1 detection
│   │   └── query-assertions  the assertThatQueries() AssertJ DSL
│   ├── hibernate-support     entity-aware assertions (optional Hibernate dependency)
│   ├── snapshot-testing      deterministic query snapshots via java-snapshot-testing
│   └── spring-boot-autoconfigure   automatic DataSource wrapping + test wiring
└── plan-assertions           PostgreSQL EXPLAIN-backed index/scan assertions
```

| Module | Maven Central | Javadoc |
|---|---|---|
| `persistence-test-core` | [![Maven Central](https://img.shields.io/maven-central/v/li.selman/persistence-test-core.svg)](https://central.sonatype.com/artifact/li.selman/persistence-test-core) | [![Javadoc](https://javadoc.io/badge2/li.selman/persistence-test-core/javadoc.svg)](https://javadoc.io/doc/li.selman/persistence-test-core) |
| `query-capture` | [![Maven Central](https://img.shields.io/maven-central/v/li.selman/query-capture.svg)](https://central.sonatype.com/artifact/li.selman/query-capture) | [![Javadoc](https://javadoc.io/badge2/li.selman/query-capture/javadoc.svg)](https://javadoc.io/doc/li.selman/query-capture) |
| `query-analysis` | [![Maven Central](https://img.shields.io/maven-central/v/li.selman/query-analysis.svg)](https://central.sonatype.com/artifact/li.selman/query-analysis) | [![Javadoc](https://javadoc.io/badge2/li.selman/query-analysis/javadoc.svg)](https://javadoc.io/doc/li.selman/query-analysis) |
| `query-assertions` | [![Maven Central](https://img.shields.io/maven-central/v/li.selman/query-assertions.svg)](https://central.sonatype.com/artifact/li.selman/query-assertions) | [![Javadoc](https://javadoc.io/badge2/li.selman/query-assertions/javadoc.svg)](https://javadoc.io/doc/li.selman/query-assertions) |
| `hibernate-support` | [![Maven Central](https://img.shields.io/maven-central/v/li.selman/hibernate-support.svg)](https://central.sonatype.com/artifact/li.selman/hibernate-support) | [![Javadoc](https://javadoc.io/badge2/li.selman/hibernate-support/javadoc.svg)](https://javadoc.io/doc/li.selman/hibernate-support) |
| `snapshot-testing` | [![Maven Central](https://img.shields.io/maven-central/v/li.selman/snapshot-testing.svg)](https://central.sonatype.com/artifact/li.selman/snapshot-testing) | [![Javadoc](https://javadoc.io/badge2/li.selman/snapshot-testing/javadoc.svg)](https://javadoc.io/doc/li.selman/snapshot-testing) |
| `spring-boot-autoconfigure` | [![Maven Central](https://img.shields.io/maven-central/v/li.selman/spring-boot-autoconfigure.svg)](https://central.sonatype.com/artifact/li.selman/spring-boot-autoconfigure) | [![Javadoc](https://javadoc.io/badge2/li.selman/spring-boot-autoconfigure/javadoc.svg)](https://javadoc.io/doc/li.selman/spring-boot-autoconfigure) |
| `plan-assertions` | [![Maven Central](https://img.shields.io/maven-central/v/li.selman/plan-assertions.svg)](https://central.sonatype.com/artifact/li.selman/plan-assertions) | [![Javadoc](https://javadoc.io/badge2/li.selman/plan-assertions/javadoc.svg)](https://javadoc.io/doc/li.selman/plan-assertions) |

### `persistence-test-core`

Database-agnostic domain model and SQL normalization. No dependency on Hibernate, Spring, or a JDBC driver -
only [JSqlParser](https://github.com/JSqlParser/JSqlParser).

- `CapturedQuery` / `BindParameter` / `StatementType` - immutable records describing one SQL execution.
- `SqlNormalizer` (SPI) / `JSqlParserSqlNormalizer` (default impl) - turns raw SQL into a `NormalizedQuery`
  (statement type, referenced tables, and a normalized SQL rendering) that's stable across whitespace,
  comments, keyword casing, and identifier-quoting differences, while still distinguishing real semantic
  differences (joins, predicates, columns, grouping, limits).

### `query-capture`

Captures every SQL statement executed through a `DataSource`, by wrapping it with
[datasource-proxy](https://github.com/ttddyy/datasource-proxy) - so capture works transparently for Spring
Data JPA, Hibernate, JdbcTemplate, and plain JDBC alike, without special-casing any of them.

- `QueryCapture.wrap(dataSource)` - wraps a `DataSource` so every statement executed through it is recorded.
- `QueryCaptureContext.current()` - thread-local accumulator of `CapturedQuery` instances; see its Javadoc
  for what it does (and does not) guarantee under concurrency and cross-thread handoff.
- `QueryCaptureExtension` (JUnit 5) - resets capture state before each test and can inject
  `QueryCaptureContext` as a test method parameter.

### `query-analysis`

Pure analyzers over `List<CapturedQuery>` - no JUnit/AssertJ dependency, reusable outside a test assertion
(e.g. in a profiling report):

- `QueryStatistics` - counts per statement type, total/average duration, accessed tables.
- `duplicatesOf` - queries with identical SQL *and* identical bind parameters, executed more than once.
- `repeatedShapesOf` / `nPlusOneCandidatesOf` - the same SQL shape executed repeatedly with different
  parameters; a heuristic on repetition count, since captured queries don't track how many rows an outer
  `SELECT` returned.

### `query-assertions`

The `assertThatQueries()` AssertJ DSL, reading from the ambient `QueryCaptureContext` by default:

```java
assertThatQueries()
    .ignore(QueryFilters.isFlywayMetadata())
    .selects(2)
    .updates(1)
    .containsTable("customer")
    .containsNoDelete()
    .hasNoNPlusOne();

assertThatQueries().lastSelect().hasTable("customer").hasParameterCount(1);
```

Failure messages include the full execution timeline and summary statistics, not just the mismatched count.
`QueryFilters` has common predicates for Flyway/Liquibase/Postgres-catalog noise.

### `hibernate-support`

Entity-to-table resolution via a live Hibernate `MappingMetamodel` (not re-derived from `@Table`
annotations, so custom naming strategies still resolve correctly), plus entity-aware assertions:

```java
HibernateAssertions.assertThatQueries(new HibernateEntityTableResolver(entityManagerFactory))
    .containsSelect(Customer.class)
    .containsNoDelete(Customer.class);
```

A separate entry point from `query-assertions`, not an extension of `QueriesAssert` - Java has no mechanism
to retroactively add methods to another module's fluent-assertion type. The only module here that depends
on Hibernate; everything else works with plain JDBC.

### `snapshot-testing`

Deterministic, structured query snapshots - never raw SQL strings, never volatile data (timestamps,
durations, connection/thread ids):

```yaml
queries:
  - type: SELECT
    tables:
      - customer
    normalizedSql: |
      select * from customer where id = ?
    count: 2
  - type: UPDATE
    tables:
      - customer
    normalizedSql: |
      update customer set name = ?
    count: 1
```

Repeated identical shapes collapse into one entry with a count, so an N+1 fix that reduces occurrences
shows up as a one-line count change on review, not a diff over several near-duplicate entries.
`QuerySnapshots.of(queries)` builds the snapshot (default: `SnapshotLevel.SEMANTIC`), `SnapshotNormalizer`
masks non-deterministic literals (UUIDs, timestamps, generated IDs), and `QuerySnapshotSerializer`
integrates with [java-snapshot-testing](https://github.com/codedabble-dev/java-snapshot-testing) for
storage/diffing/approval:

```java
expect.serializer(new QuerySnapshotSerializer()).toMatchSnapshot(QuerySnapshots.current());
```

### `plan-assertions`

PostgreSQL execution-plan assertions via `EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)`, deriving stable facts
rather than ever comparing raw plan text:

```java
CapturedQuery lastSelect = assertThatQueries().lastSelect().capturedQuery();
PlanAssertions.assertThatPlanOf(connection, lastSelect)
    .usesIndex()
    .usesAnyIndexOn("customer")
    .avoidsSequentialScan();
```

`ANALYZE` executes the statement, including any side-effecting DML - `PostgresExecutionPlanAnalyzer` always
runs inside a savepoint it rolls back to afterward, verified against a real PostgreSQL instance (via
Testcontainers) including that a `DELETE` never actually persists.

### `spring-boot-autoconfigure`

Wraps the application's `DataSource` bean(s) with `QueryCapture` automatically (a `BeanPostProcessor`
registered via `@AutoConfiguration`), and registers a `TestExecutionListener` (via `META-INF/spring.factories`)
that resets `QueryCaptureContext` before every test method - so `@SpringBootTest` works with no manual
wiring. Disable with `persistence-test.enabled=false`.

## Building

```shell
./mvnw verify
```

Runs Spotless, Checkstyle, Error Prone/NullAway, and a JaCoCo coverage gate (85% line / 75% branch -
deliberately below the 100% used by [java-lib-archetype](https://github.com/haisi/java-lib-archetype), this
project's archetype, since real defensive branches here - JDBC proxy edge cases, SPI dispatch, parser
fallbacks, connection-failure handling - don't have a meaningful test for every branch). Add `-Dquick` to
skip all of that and just compile and test. `plan-assertions`' integration test needs Docker (Testcontainers
PostgreSQL).

## Known limitations

Tracked as follow-up rather than fixed now (see the relevant Javadoc for each):

- `JSqlParserSqlNormalizer` does not canonicalize alias names - two queries identical except for alias
  spelling normalize differently today. `SnapshotNormalizer`'s `ignoreAliases()`/`ignoreComments()` are
  documented no-ops for the same reason (comments are already gone by construction).
- `QueryCaptureListener` only captures the first batch item's bind parameters for batched statements, and
  uses datasource-proxy's deprecated `getQueryArgsList()` rather than the lower-level
  `getParametersList()`/`ParameterSetOperation` API.
- `QueryCaptureContext` cannot see queries executed on a different thread than the test thread (e.g. from
  `@Async` code or an executor) without further work to propagate capture across the handoff.
- No "Intent" snapshot level (describing *what* a query does, above the SQL/Semantic levels) - generalized
  query-to-intent inference is open-ended enough to need its own design discussion.
- Adapters for other databases (MySQL, MariaDB, Oracle, SQL Server) and other JDBC-based frameworks (jOOQ,
  MyBatis) are not implemented; `ExecutionPlanAnalyzer` and `SqlNormalizer` are SPIs specifically so those
  can be added without modifying this library.

## Releasing

Releases are published to Maven Central via [JReleaser](https://jreleaser.org). Pushing a tag matching `v*`
(e.g. `v0.1.0`) triggers `.github/workflows/release.yml`, which stages every module's build artifacts and
hands them to JReleaser to sign and deploy to the [Central Portal](https://central.sonatype.com).

```shell
./bumpPomVersion.sh
git push
./release.sh
```

## Contributing

Bug reports, feature requests and pull requests are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md). This
project follows a [Code of Conduct](CODE_OF_CONDUCT.md); by participating you agree to abide by it.

## License

[Apache License, Version 2.0](LICENSE).

See `jreleaser.yml` for the deployment configuration.
