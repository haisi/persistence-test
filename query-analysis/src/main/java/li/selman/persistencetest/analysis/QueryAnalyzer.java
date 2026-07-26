package li.selman.persistencetest.analysis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import li.selman.persistencetest.core.BindParameter;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;

/**
 * Reusable analyzers over a list of {@link CapturedQuery}.
 *
 * <p>Every method here is a pure function of its input list - none of them read {@code QueryCaptureContext}
 * or any other ambient state, so they're just as usable outside a test assertion (e.g. in a profiling
 * report) as inside one.
 */
public final class QueryAnalyzer {

    /**
     * Default minimum number of times a query shape must repeat to be considered an N+1 candidate by
     * {@link #nPlusOneCandidatesOf(List)}.
     */
    public static final int DEFAULT_N_PLUS_ONE_THRESHOLD = 2;

    private QueryAnalyzer() {}

    /** Aggregate counts and timing over {@code queries}. */
    public static QueryStatistics statisticsOf(List<CapturedQuery> queries) {
        return QueryStatistics.of(queries);
    }

    /** Union of every query's {@link CapturedQuery#tables()}. */
    public static Set<String> accessedTablesOf(List<CapturedQuery> queries) {
        return statisticsOf(queries).accessedTables();
    }

    /** {@code queries} sorted by {@link CapturedQuery#sequence()} - the order they were executed in. */
    public static List<CapturedQuery> timelineOf(List<CapturedQuery> queries) {
        return queries.stream()
                .sorted(Comparator.comparingLong(CapturedQuery::sequence))
                .toList();
    }

    /**
     * Groups of queries with identical normalized SQL <em>and</em> identical bind parameter values,
     * executed more than once - genuinely redundant queries. See {@link #repeatedShapesOf(List)} for the
     * same-SQL-different-parameters case.
     */
    public static List<DuplicateQueryGroup> duplicatesOf(List<CapturedQuery> queries) {
        Map<QuerySignature, List<CapturedQuery>> grouped = new LinkedHashMap<>();
        for (CapturedQuery query : queries) {
            grouped.computeIfAbsent(
                            new QuerySignature(query.normalizedSql(), query.parameters()), key -> new ArrayList<>())
                    .add(query);
        }
        return grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> new DuplicateQueryGroup(entry.getKey().normalizedSql(), entry.getValue()))
                .toList();
    }

    /**
     * Groups of queries with identical normalized SQL, executed more than once, regardless of whether their
     * bind parameters differ. A superset of {@link #duplicatesOf(List)}: every duplicate group is also a
     * repeated shape, but a repeated shape whose occurrences have differing parameters is not a duplicate.
     */
    public static List<RepeatedQueryShape> repeatedShapesOf(List<CapturedQuery> queries) {
        Map<String, List<CapturedQuery>> grouped = new LinkedHashMap<>();
        for (CapturedQuery query : queries) {
            grouped.computeIfAbsent(query.normalizedSql(), key -> new ArrayList<>())
                    .add(query);
        }
        return grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> new RepeatedQueryShape(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * {@link #repeatedShapesOf(List)}, filtered to {@code SELECT} shapes repeated at least
     * {@value #DEFAULT_N_PLUS_ONE_THRESHOLD} times - the textbook N+1 symptom of one query per row of an
     * outer result set. This is a heuristic based purely on repetition count, since captured queries don't
     * track how many rows an outer {@code SELECT} returned; a repeated shape that happens to be legitimate
     * (e.g. a batch job intentionally querying per item) will still be flagged.
     */
    public static List<RepeatedQueryShape> nPlusOneCandidatesOf(List<CapturedQuery> queries) {
        return nPlusOneCandidatesOf(queries, DEFAULT_N_PLUS_ONE_THRESHOLD);
    }

    /** Like {@link #nPlusOneCandidatesOf(List)}, with a caller-supplied minimum occurrence count. */
    public static List<RepeatedQueryShape> nPlusOneCandidatesOf(List<CapturedQuery> queries, int threshold) {
        if (threshold < 2) {
            throw new IllegalArgumentException("threshold must be >= 2, got " + threshold);
        }
        return repeatedShapesOf(queries).stream()
                .filter(shape -> shape.occurrences().getFirst().statementType() == StatementType.SELECT)
                .filter(shape -> shape.occurrenceCount() >= threshold)
                .toList();
    }

    private record QuerySignature(String normalizedSql, List<BindParameter> parameters) {}
}
