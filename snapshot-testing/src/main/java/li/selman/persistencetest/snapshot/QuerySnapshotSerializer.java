package li.selman.persistencetest.snapshot;

import au.com.origin.snapshots.Snapshot;
import au.com.origin.snapshots.SnapshotSerializerContext;
import au.com.origin.snapshots.serializers.SnapshotSerializer;

/**
 * Integrates with <a href="https://github.com/codedabble-dev/java-snapshot-testing">java-snapshot-testing</a>:
 * renders a {@link QuerySnapshot} as YAML and hands it storage/diffing/approval-unaware to {@code Expect}.
 *
 * <pre>{@code
 * expect.serializer(new QuerySnapshotSerializer())
 *         .toMatchSnapshot(QuerySnapshots.current());
 * }</pre>
 */
public final class QuerySnapshotSerializer implements SnapshotSerializer {

    @Override
    public Snapshot apply(Object snapshot, SnapshotSerializerContext context) {
        if (!(snapshot instanceof QuerySnapshot querySnapshot)) {
            throw new IllegalArgumentException("QuerySnapshotSerializer only supports QuerySnapshot instances, got "
                    + (snapshot == null ? "null" : snapshot.getClass()));
        }
        return context.toSnapshot(QuerySnapshotYaml.render(querySnapshot));
    }

    @Override
    public String getOutputFormat() {
        return "yml";
    }
}
