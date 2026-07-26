package li.selman.persistencetest.core.normalize;

/** Thrown when a {@link SqlNormalizer} cannot parse the given SQL text. */
public class SqlNormalizationException extends RuntimeException {

    public SqlNormalizationException(String sql, Throwable cause) {
        super("Failed to normalize SQL: " + sql, cause);
    }
}
