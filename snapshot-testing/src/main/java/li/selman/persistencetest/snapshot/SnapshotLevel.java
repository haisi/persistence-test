package li.selman.persistencetest.snapshot;

/**
 * How much of a captured query's SQL text ends up in a {@link QuerySnapshot}.
 *
 * <p>Only two levels are implemented. A third, higher-level "Intent" representation - describing
 * <em>what</em> a query does rather than the SQL Hibernate happened to generate for it - is intentionally
 * not attempted here: generalized query-to-intent inference is open-ended enough that a real implementation
 * would need its own design discussion, not a quick addition to this enum.
 */
public enum SnapshotLevel {
    /**
     * {@link li.selman.persistencetest.core.CapturedQuery#sql()}, whitespace-collapsed only - close to
     * what the driver actually saw. Useful for debugging, but sensitive to incidental Hibernate formatting
     * changes.
     */
    SQL,

    /**
     * {@link li.selman.persistencetest.core.CapturedQuery#normalizedSql()} - stable across whitespace,
     * comments, keyword casing, and identifier quoting differences. The recommended default.
     */
    SEMANTIC
}
