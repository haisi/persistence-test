package li.selman.persistencetest.core;

/**
 * The kind of SQL statement a {@link CapturedQuery} represents.
 *
 * <p>Deliberately coarse: it distinguishes the categories that assertions and analyzers actually branch on
 * (is this a write? should it ever run twice in a loop?) rather than every statement subtype a dialect
 * supports.
 */
public enum StatementType {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    /** {@code CREATE}/{@code ALTER}/{@code DROP}/{@code TRUNCATE} and other schema-changing statements. */
    DDL,
    /** Recognized by the driver/parser but not one of the categories above (e.g. {@code SET}, {@code CALL}). */
    OTHER
}
