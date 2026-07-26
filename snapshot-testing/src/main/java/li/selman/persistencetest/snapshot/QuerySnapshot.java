package li.selman.persistencetest.snapshot;

import java.util.List;

/**
 * A deterministic, structured representation of the queries executed during a test - never raw SQL
 * strings and never volatile data (timestamps, durations, connection/thread identifiers): only what's
 * needed to notice a persistence regression on review.
 *
 * @param queries one entry per distinct query shape, in first-occurrence order.
 */
public record QuerySnapshot(List<QuerySnapshotEntry> queries) {

    @SuppressWarnings("Var") // reassigning a compact constructor's implicit parameter for a defensive copy
    // is the standard record idiom; it can't be annotated @Var since the underlying field is final.
    public QuerySnapshot {
        queries = List.copyOf(queries);
    }
}
