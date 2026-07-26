# Persistence Test Library

Design and implement a production-quality Java 25 library called **`persistence-test`** for use in **Spring Boot 4** integration tests. The library should help detect persistence regressions, N+1 problems, missing indexes, unexpected queries, and ORM behavior changes while remaining easy to use across multiple projects.

I want you to use https://github.com/haisi/java-lib-archetype as the archetype --> however the maven structure modular.

## Overall Goals

The library must work with:

- Java 25
- Spring Boot 4.x
- Spring Data JPA
- Hibernate 7.x
- JdbcTemplate
- PostgreSQL (first-class support)
- JUnit 5
- AssertJ

The design should be modular, extensible, and database-agnostic wherever possible.

---

# Architecture

Split the library into the following modules:

```text
persistence-test
├── query-capture
├── query-analysis
├── query-assertions
├── snapshot-testing
├── plan-assertions
├── spring-boot-autoconfigure
└── hibernate-support
```

Each module should have a clearly defined responsibility and minimal dependencies on the others.

---

# Query Capture

Capture every SQL statement executed during a test by intercepting the **DataSource/JDBC layer**, **not** by parsing Hibernate SQL logs.

The capture mechanism must therefore work transparently for:

- Spring Data JPA
- Hibernate
- JdbcTemplate
- plain JDBC
- future JDBC-based frameworks

Each captured statement should contain:

- execution order
- timestamp
- SQL
- normalized SQL
- bind parameters
- execution duration
- statement type (SELECT / INSERT / UPDATE / DELETE / DDL / etc.)
- affected rows (if available)
- exception (if any)
- thread
- connection identifier

The capture lifecycle should integrate automatically with JUnit 5.

---

# SQL Normalization

Implement a robust SQL normalization pipeline.

Normalization should remove irrelevant differences such as:

- whitespace
- formatting
- SQL comments
- alias names
- identifier quoting differences
- case differences

while preserving semantic differences such as:

- joins
- predicates
- selected columns
- grouping
- ordering
- limits
- tables

The normalized representation should be deterministic across Hibernate formatting changes whenever possible.

---

# Query Analysis

Implement reusable analyzers capable of detecting:

- duplicate queries
- repeated identical queries
- N+1 query patterns
- query counts
- statement statistics
- accessed tables
- execution timeline
- repeated query shapes executed with different bind parameters

The analyzers should be reusable independently of assertions.

---

# Fluent Assertions

Provide an AssertJ-style DSL.

Example API:

```java
assertThatQueries()
    .selects(2)
    .updates(1)
    .containsSelect(Customer.class)
    .containsNoDelete();
```

```java
assertThatQueries()
    .hasNoNPlusOne();
```

```java
assertThatQueries()
    .containsTable("customer");
```

```java
assertThatQueries()
    .lastSelect()
    .usesIndex();
```

Failure messages should be extremely detailed and actionable.

---

# Snapshot Testing (First-Class Feature)

Snapshot testing should be one of the core capabilities of the library.

Do **not** snapshot raw SQL strings.

Instead, snapshot a structured execution model that remains stable across harmless SQL formatting changes.

Example:

```yaml
queries:
  - type: SELECT
    tables:
      - customer
      - orders
    normalizedSql: |
      select ...
    count: 1

  - type: UPDATE
    tables:
      - customer
```

The snapshot model should be deterministic and stable.

Do **not** store volatile information by default.

---

# Snapshot Levels

Support multiple snapshot modes.

## SQL

Near-raw normalized SQL.

Useful for debugging.

## Semantic (Default)

Store semantic information including:

- statement type
- tables
- joins
- predicates
- grouping
- ordering
- selected columns

Ignore:

- formatting
- aliases
- identifier quoting
- whitespace
- comments

This should be the recommended default.

## Intent

Provide an even higher-level representation describing **what the query does**, not exactly how Hibernate generated it.

---

# Snapshot Integration

Do **not** reinvent snapshot storage.

Integrate directly with:

https://github.com/codedabble-dev/java-snapshot-testing

The library should only generate deterministic YAML or JSON representations and delegate snapshot persistence, diffing, and approval workflow to `java-snapshot-testing`.

---

# Snapshot Transformation Pipeline

One of the primary extension points should be a transformation pipeline that makes snapshots stable.

Provide built-in transformations such as:

- mask UUIDs
- mask timestamps
- mask generated IDs
- mask tenant identifiers
- ignore schemas
- ignore comments
- ignore optimizer hints
- ignore execution duration

Example API:

```java
SnapshotNormalizer.builder()
    .replaceUuid("<uuid>")
    .replaceTimestamp("<timestamp>")
    .replaceGeneratedIds("<id>")
    .ignoreAliases()
    .ignoreComments()
    .build();
```

Also expose an SPI:

```java
public interface QuerySnapshotTransformer {

    QuerySnapshot transform(QuerySnapshot snapshot);

}
```

This should allow applications to implement project-specific normalization rules without modifying the library.

---

# Query Filtering

Allow queries to be ignored before analysis or snapshot generation.

Examples:

- Flyway
- Liquibase
- Hibernate metadata queries
- PostgreSQL startup queries
- custom predicates

Example:

```java
.ignore(query -> query.isMetadata())
.ignoreTables("flyway_schema_history")
```

---

# Execution Plan Assertions

Provide a database SPI.

Implement PostgreSQL first.

Use:

```sql
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)
```

Never compare raw execution plans.

Instead derive stable facts.

Example assertions:

```java
usesIndex()

usesIndex("idx_customer_email")

avoidsSequentialScan()

estimatedRowsLessThan(10)

usesAnyIndexOn("customer")
```

Database-specific implementations should be replaceable.

---

# Hibernate Support

Provide an optional Hibernate module.

Capabilities may include:

- mapping entities to tables
- mapping associations
- entity-aware assertions

Example:

```java
assertThatQueries()
    .containsSelect(Customer.class);
```

The core library must **not** depend on Hibernate.

---

# Spring Boot Integration

Provide auto-configuration for Spring Boot 4.

The library should work with:

```java
@SpringBootTest
```

without requiring manual setup.

Enable automatic query capture for each test.

Ensure thread safety and proper cleanup between tests.

---

# Diagnostics

Failure messages should be one of the strongest features.

Examples:

- grouped duplicate queries
- repeated query shapes
- parameter samples
- normalized SQL
- execution order
- query timeline
- detected N+1 candidates
- summary statistics

Diagnostics should immediately explain **why** a test failed.

---

# Extensibility

Design all major components around SPIs.

Examples:

- database adapters
- execution-plan analyzers
- snapshot transformers
- SQL normalizers
- query analyzers

The library should be easy to extend without modifying core modules.

---

# Non-Goals

Do **not**:

- parse Hibernate SQL logs
- depend on Hibernate in the core module
- compare raw textual execution plans
- rely on exact SQL formatting
- make assertions based solely on execution time
- create brittle assertions that break across harmless Hibernate formatting changes

---

# Quality Expectations

Produce a clean, well-documented, production-quality library with:

- immutable domain models
- comprehensive unit tests
- integration tests against PostgreSQL using Testcontainers
- clear module boundaries
- excellent Javadoc
- fluent, discoverable APIs
- high-quality failure messages
- extension points for future databases and frameworks

Favor long-term maintainability, stable assertions, and excellent developer ergonomics over clever implementation details.

---

# Nice-to-Have Features

If time permits, consider implementing:

- Snapshot diff visualizations that clearly explain *what* changed between snapshots.
- Query execution timeline visualization.
- Repository/service profiling summaries (query count, duration, N+1 detection, duplicate queries).
- Automatic grouping of repeated query shapes.
- Detection of identical queries executed repeatedly with identical parameters.
- Optional assertions on returned row counts.
- Future adapters for MySQL, MariaDB, Oracle, SQL Server, and H2.
- Optional support for jOOQ and MyBatis, since they also execute through JDBC.