package li.selman.persistencetest.core.normalize;

/**
 * SPI for turning raw SQL text into a {@link NormalizedQuery}.
 *
 * <p>Implement this to plug in project- or dialect-specific normalization (e.g. a normalizer that
 * understands a proprietary SQL dialect the default {@link JSqlParserSqlNormalizer} can't parse) without
 * modifying this library.
 */
@FunctionalInterface
public interface SqlNormalizer {

    /**
     * Normalizes a single SQL statement.
     *
     * @param sql the raw SQL text as passed to the JDBC driver.
     * @return the statement's normalized, semantic representation.
     * @throws SqlNormalizationException if {@code sql} cannot be parsed.
     */
    NormalizedQuery normalize(String sql);
}
