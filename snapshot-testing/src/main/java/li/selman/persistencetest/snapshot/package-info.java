/**
 * Deterministic, structured snapshots of captured queries.
 *
 * <p>Start with {@link li.selman.persistencetest.snapshot.QuerySnapshots} to build a
 * {@link li.selman.persistencetest.snapshot.QuerySnapshot} from captured queries, and
 * {@link li.selman.persistencetest.snapshot.QuerySnapshotSerializer} to hand it to
 * <a href="https://github.com/codedabble-dev/java-snapshot-testing">java-snapshot-testing</a>'s
 * {@code Expect}.
 */
@NullMarked
package li.selman.persistencetest.snapshot;

import org.jspecify.annotations.NullMarked;
