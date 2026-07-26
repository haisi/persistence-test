package li.selman.persistencetest.analysis;

import java.util.List;
import li.selman.persistencetest.core.CapturedQuery;

/**
 * A group of two or more captured queries with the same normalized SQL, regardless of whether their bind
 * parameters differ. The raw signal behind N+1 detection: the same query shape executed repeatedly, once
 * per row of some outer result set, is the textbook symptom.
 *
 * @param normalizedSql the shared normalized SQL.
 * @param occurrences every query in the group, in execution order; always has at least 2 elements.
 */
public record RepeatedQueryShape(String normalizedSql, List<CapturedQuery> occurrences) {

    @SuppressWarnings("Var") // reassigning a compact constructor's implicit parameter for a defensive copy
    // is the standard record idiom; it can't be annotated @Var since the underlying field is final.
    public RepeatedQueryShape {
        occurrences = List.copyOf(occurrences);
    }

    /** Number of times this query shape was executed. */
    public int occurrenceCount() {
        return occurrences.size();
    }
}
