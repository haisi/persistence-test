package li.selman.persistencetest.analysis;

import java.util.List;
import li.selman.persistencetest.core.CapturedQuery;

/**
 * A group of two or more captured queries with the same normalized SQL <em>and</em> the same bind
 * parameter values - genuinely redundant queries, as opposed to a {@link RepeatedQueryShape} where the
 * parameters differ.
 *
 * @param normalizedSql the shared normalized SQL.
 * @param occurrences every query in the group, in execution order; always has at least 2 elements.
 */
public record DuplicateQueryGroup(String normalizedSql, List<CapturedQuery> occurrences) {

    @SuppressWarnings("Var") // reassigning a compact constructor's implicit parameter for a defensive copy
    // is the standard record idiom; it can't be annotated @Var since the underlying field is final.
    public DuplicateQueryGroup {
        occurrences = List.copyOf(occurrences);
    }

    /** Number of times this exact query (SQL and parameters) was executed. */
    public int occurrenceCount() {
        return occurrences.size();
    }
}
