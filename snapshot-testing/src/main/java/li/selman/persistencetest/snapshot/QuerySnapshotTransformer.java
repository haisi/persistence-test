package li.selman.persistencetest.snapshot;

/**
 * SPI for project-specific snapshot normalization, applied after {@link QuerySnapshots} builds a
 * {@link QuerySnapshot} and before it's serialized.
 *
 * <p>Implement this for normalization {@link SnapshotNormalizer}'s built-in rules don't cover (e.g. masking
 * an application-specific encoded identifier format) without modifying this library.
 */
@FunctionalInterface
public interface QuerySnapshotTransformer {

    /** Returns a transformed snapshot; may return {@code snapshot} unchanged. */
    QuerySnapshot transform(QuerySnapshot snapshot);
}
