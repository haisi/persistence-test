package li.selman.persistencetest.core;

import org.jspecify.annotations.Nullable;

/**
 * A single bind parameter of a captured statement.
 *
 * @param position 1-based JDBC parameter index, matching {@link java.sql.PreparedStatement} conventions.
 * @param value the bound value as passed to the driver; {@code null} for a bound SQL {@code NULL}.
 * @param typeName the driver/Java type name of the value (e.g. {@code "java.lang.String"}), when known.
 */
public record BindParameter(
        int position, @Nullable Object value, @Nullable String typeName) {

    public BindParameter {
        if (position < 1) {
            throw new IllegalArgumentException("position must be 1-based (>= 1), got " + position);
        }
    }

    /**
     * Creates a parameter with its type name derived from {@code value}'s runtime class, or {@code null} if
     * {@code value} is {@code null}.
     */
    public static BindParameter of(int position, @Nullable Object value) {
        String typeName = value == null ? null : value.getClass().getName();
        return new BindParameter(position, value, typeName);
    }
}
