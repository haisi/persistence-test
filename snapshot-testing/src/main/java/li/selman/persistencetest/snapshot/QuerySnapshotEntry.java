package li.selman.persistencetest.snapshot;

import java.util.List;
import li.selman.persistencetest.core.StatementType;

/**
 * One distinct query shape within a {@link QuerySnapshot}.
 *
 * @param statementType the kind of statement.
 * @param tables tables referenced by the statement.
 * @param normalizedSql the statement's SQL at whatever {@link SnapshotLevel} the snapshot was built with.
 * @param count how many times this exact shape (statement type, tables, and SQL all equal) was executed.
 *     Grouping repeats into a count, rather than listing every occurrence, is what keeps the snapshot
 *     stable when only the number of repetitions changes (e.g. an N+1 fix that reduces 5 identical queries
 *     to 3 shows up as a one-line count change, not a diff over 3-5 near-duplicate entries).
 */
public record QuerySnapshotEntry(StatementType statementType, List<String> tables, String normalizedSql, int count) {

    @SuppressWarnings("Var") // reassigning a compact constructor's implicit parameter for a defensive copy
    // is the standard record idiom; it can't be annotated @Var since the underlying field is final.
    public QuerySnapshotEntry {
        tables = List.copyOf(tables);
        if (count < 1) {
            throw new IllegalArgumentException("count must be >= 1, got " + count);
        }
    }
}
