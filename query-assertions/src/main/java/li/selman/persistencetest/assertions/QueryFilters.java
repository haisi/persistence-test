package li.selman.persistencetest.assertions;

import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import li.selman.persistencetest.core.CapturedQuery;

/**
 * Common predicates for {@link QueriesAssert#ignore(Predicate)}, so tests don't have to hand-write
 * table-name matching for well-known migration/metadata noise.
 *
 * <p>These are heuristics over {@link CapturedQuery#tables()} and {@link CapturedQuery#normalizedSql()},
 * not an exhaustive or dialect-complete list - write your own predicate for anything not covered here.
 */
public final class QueryFilters {

    private QueryFilters() {}

    /** Matches queries that reference any of the given tables (case-insensitive). */
    public static Predicate<CapturedQuery> accessesAnyTable(String... tables) {
        Set<String> lower =
                Stream.of(tables).map(table -> table.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        return query -> query.tables().stream().anyMatch(lower::contains);
    }

    /** Flyway's schema history table. */
    public static Predicate<CapturedQuery> isFlywayMetadata() {
        return accessesAnyTable("flyway_schema_history");
    }

    /** Liquibase's changelog and changelog-lock tables. */
    public static Predicate<CapturedQuery> isLiquibaseMetadata() {
        return accessesAnyTable("databasechangelog", "databasechangeloglock");
    }

    /** PostgreSQL system catalog / information schema queries, typically issued by JDBC driver metadata calls. */
    public static Predicate<CapturedQuery> isPostgresCatalogQuery() {
        return query -> query.tables().stream()
                .anyMatch(table -> table.startsWith("pg_") || table.startsWith("information_schema."));
    }

    /** A bare connection-validation ping (e.g. {@code select 1}), issued by connection pools and Hibernate. */
    public static Predicate<CapturedQuery> isConnectionValidationQuery() {
        return query -> query.tables().isEmpty()
                && query.normalizedSql().toLowerCase(Locale.ROOT).matches("select 1;?");
    }
}
