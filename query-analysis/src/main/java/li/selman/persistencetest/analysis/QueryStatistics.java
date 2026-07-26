package li.selman.persistencetest.analysis;

import com.google.errorprone.annotations.Var;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;

/**
 * Aggregate counts and timing over a list of captured queries.
 *
 * @param totalCount total number of captured queries.
 * @param countsByType number of queries per {@link StatementType}; types with zero occurrences are absent.
 * @param totalDuration sum of every query's {@link CapturedQuery#duration()}; queries with no measured
 *     duration (execution never completed) contribute zero.
 * @param accessedTables union of every query's {@link CapturedQuery#tables()}, lower-cased and sorted.
 */
public record QueryStatistics(
        int totalCount, Map<StatementType, Integer> countsByType, Duration totalDuration, Set<String> accessedTables) {

    @SuppressWarnings("Var") // reassigning a compact constructor's implicit parameter for a defensive copy
    // is the standard record idiom; it can't be annotated @Var since the underlying field is final.
    public QueryStatistics {
        countsByType = Map.copyOf(countsByType);
        accessedTables = Set.copyOf(accessedTables);
    }

    /** Number of captured queries of the given type; zero if none. */
    public int countOf(StatementType type) {
        return countsByType.getOrDefault(type, 0);
    }

    /** {@link #totalDuration()} divided evenly across {@link #totalCount()}, or zero if there were none. */
    public Duration averageDuration() {
        return totalCount == 0 ? Duration.ZERO : totalDuration.dividedBy(totalCount);
    }

    /** Computes statistics over {@code queries}. */
    public static QueryStatistics of(List<CapturedQuery> queries) {
        Map<StatementType, Integer> counts = new EnumMap<>(StatementType.class);
        Set<String> tables = new TreeSet<>();
        @Var Duration total = Duration.ZERO;
        for (CapturedQuery query : queries) {
            counts.merge(query.statementType(), 1, Integer::sum);
            tables.addAll(query.tables());
            if (query.duration() != null) {
                total = total.plus(query.duration());
            }
        }
        return new QueryStatistics(queries.size(), counts, total, tables);
    }
}
