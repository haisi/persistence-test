package li.selman.persistencetest.assertions;

import java.util.List;
import java.util.function.Predicate;
import li.selman.persistencetest.analysis.DuplicateQueryGroup;
import li.selman.persistencetest.analysis.QueryAnalyzer;
import li.selman.persistencetest.analysis.RepeatedQueryShape;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;
import org.assertj.core.api.AbstractAssert;

/**
 * Fluent assertions over a list of {@link CapturedQuery}. Obtain one via
 * {@link QueryAssertions#assertThatQueries()}.
 */
public final class QueriesAssert extends AbstractAssert<QueriesAssert, List<CapturedQuery>> {

    QueriesAssert(List<CapturedQuery> actual) {
        super(actual, QueriesAssert.class);
    }

    // --- filtering -----------------------------------------------------------------------------------

    /**
     * Returns a new assertion over only the queries for which {@code predicate} is {@code false} -
     * everything matching {@code predicate} is excluded from every assertion made afterward. See
     * {@link QueryFilters} for common predicates (Flyway/Liquibase/catalog noise).
     */
    public QueriesAssert ignore(Predicate<CapturedQuery> predicate) {
        isNotNull();
        return new QueriesAssert(
                actual.stream().filter(query -> !predicate.test(query)).toList());
    }

    /** Shorthand for {@code ignore(QueryFilters.accessesAnyTable(tables))}. */
    public QueriesAssert ignoreTables(String... tables) {
        return ignore(QueryFilters.accessesAnyTable(tables));
    }

    // --- counts ----------------------------------------------------------------------------------------

    public QueriesAssert hasTotalCount(int expected) {
        isNotNull();
        if (actual.size() != expected) {
            failWithMessage(
                    "%s",
                    Diagnostics.withContext(
                            "Expected %d captured queries but found %d.".formatted(expected, actual.size()), actual));
        }
        return this;
    }

    public QueriesAssert selects(int expected) {
        return hasCountOfType(StatementType.SELECT, expected);
    }

    public QueriesAssert inserts(int expected) {
        return hasCountOfType(StatementType.INSERT, expected);
    }

    public QueriesAssert updates(int expected) {
        return hasCountOfType(StatementType.UPDATE, expected);
    }

    public QueriesAssert deletes(int expected) {
        return hasCountOfType(StatementType.DELETE, expected);
    }

    private QueriesAssert hasCountOfType(StatementType type, int expected) {
        isNotNull();
        int actualCount = countOfType(type);
        if (actualCount != expected) {
            failWithMessage(
                    "%s",
                    Diagnostics.withContext(
                            "Expected %d %s quer%s but found %d."
                                    .formatted(expected, type, expected == 1 ? "y" : "ies", actualCount),
                            actual));
        }
        return this;
    }

    private int countOfType(StatementType type) {
        return (int)
                actual.stream().filter(query -> query.statementType() == type).count();
    }

    // --- presence / absence -----------------------------------------------------------------------------

    public QueriesAssert containsNoSelect() {
        return containsNoneOfType(StatementType.SELECT);
    }

    public QueriesAssert containsNoInsert() {
        return containsNoneOfType(StatementType.INSERT);
    }

    public QueriesAssert containsNoUpdate() {
        return containsNoneOfType(StatementType.UPDATE);
    }

    public QueriesAssert containsNoDelete() {
        return containsNoneOfType(StatementType.DELETE);
    }

    private QueriesAssert containsNoneOfType(StatementType type) {
        isNotNull();
        List<CapturedQuery> matches =
                actual.stream().filter(query -> query.statementType() == type).toList();
        if (!matches.isEmpty()) {
            failWithMessage(
                    "%s",
                    Diagnostics.withContext(
                            "Expected no %s queries but found %d.".formatted(type, matches.size()), actual));
        }
        return this;
    }

    /** Whether any captured query's {@link CapturedQuery#tables()} contains {@code table} (case-insensitive). */
    public QueriesAssert containsTable(String table) {
        isNotNull();
        String lower = Diagnostics.lowercase(table);
        boolean found = actual.stream().anyMatch(query -> query.tables().contains(lower));
        if (!found) {
            failWithMessage(
                    "%s",
                    Diagnostics.withContext(
                            "Expected queries to access table '%s' but none did. Accessed tables: %s"
                                    .formatted(table, QueryAnalyzer.accessedTablesOf(actual)),
                            actual));
        }
        return this;
    }

    // --- analysis-based ----------------------------------------------------------------------------------

    /** No {@code SELECT} shape repeated {@value QueryAnalyzer#DEFAULT_N_PLUS_ONE_THRESHOLD}+ times. */
    public QueriesAssert hasNoNPlusOne() {
        return hasNoNPlusOne(QueryAnalyzer.DEFAULT_N_PLUS_ONE_THRESHOLD);
    }

    /** Like {@link #hasNoNPlusOne()}, with a caller-supplied minimum occurrence count. */
    public QueriesAssert hasNoNPlusOne(int threshold) {
        isNotNull();
        List<RepeatedQueryShape> candidates = QueryAnalyzer.nPlusOneCandidatesOf(actual, threshold);
        if (!candidates.isEmpty()) {
            failWithMessage(
                    "%s",
                    Diagnostics.withContext(
                            "Detected %d likely N+1 pattern(s) - the same SELECT shape executed %d+ times:%n%n%s"
                                    .formatted(
                                            candidates.size(),
                                            threshold,
                                            Diagnostics.describeRepeatedShapes(candidates)),
                            actual));
        }
        return this;
    }

    /** No query executed more than once with identical SQL and identical bind parameters. */
    public QueriesAssert hasNoDuplicates() {
        isNotNull();
        List<DuplicateQueryGroup> duplicates = QueryAnalyzer.duplicatesOf(actual);
        if (!duplicates.isEmpty()) {
            failWithMessage(
                    "%s",
                    Diagnostics.withContext(
                            "Detected %d duplicate quer%s (identical SQL and parameters executed more than once):%n%n%s"
                                    .formatted(
                                            duplicates.size(),
                                            duplicates.size() == 1 ? "y" : "ies",
                                            duplicates.stream()
                                                    .map(group -> "  executed %d times: %s"
                                                            .formatted(group.occurrenceCount(), group.normalizedSql()))
                                                    .collect(java.util.stream.Collectors.joining(
                                                            System.lineSeparator()))),
                            actual));
        }
        return this;
    }

    // --- single-query views -------------------------------------------------------------------------------

    public SingleCapturedQueryAssert firstSelect() {
        return singleOfType(StatementType.SELECT, true);
    }

    public SingleCapturedQueryAssert lastSelect() {
        return singleOfType(StatementType.SELECT, false);
    }

    public SingleCapturedQueryAssert first() {
        return single(true);
    }

    public SingleCapturedQueryAssert last() {
        return single(false);
    }

    private SingleCapturedQueryAssert singleOfType(StatementType type, boolean first) {
        isNotNull();
        List<CapturedQuery> matches = QueryAnalyzer.timelineOf(
                actual.stream().filter(query -> query.statementType() == type).toList());
        if (matches.isEmpty()) {
            failWithMessage(
                    "%s",
                    Diagnostics.withContext("Expected at least one %s query but found none.".formatted(type), actual));
        }
        return new SingleCapturedQueryAssert(first ? matches.getFirst() : matches.getLast());
    }

    private SingleCapturedQueryAssert single(boolean first) {
        isNotNull();
        if (actual.isEmpty()) {
            failWithMessage(
                    "%s", Diagnostics.withContext("Expected at least one captured query but found none.", actual));
        }
        List<CapturedQuery> timeline = QueryAnalyzer.timelineOf(actual);
        return new SingleCapturedQueryAssert(first ? timeline.getFirst() : timeline.getLast());
    }
}
