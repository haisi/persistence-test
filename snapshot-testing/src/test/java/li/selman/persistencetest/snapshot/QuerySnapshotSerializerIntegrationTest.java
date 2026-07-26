package li.selman.persistencetest.snapshot;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import java.util.List;
import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Exercises {@link QuerySnapshotSerializer} against the real java-snapshot-testing framework, rather than
 * just unit-testing {@link QuerySnapshotYaml#render}: this is what confirms the {@code Snapshot} object
 * {@code SnapshotSerializerContext.toSnapshot} produces actually round-trips through {@code Expect}'s real
 * storage/comparison path. The baseline file this test compares against is committed alongside the test
 * (see {@code __snapshots__}).
 */
@ExtendWith(SnapshotExtension.class)
class QuerySnapshotSerializerIntegrationTest {

    // Injected by SnapshotExtension via reflection before each test method runs, not by our own code -
    // NullAway can't see that initialization.
    @SuppressWarnings("NullAway")
    private Expect expect;

    @Test
    void matchesTheCommittedYamlSnapshot() {
        var fixtures = new CapturedQueryFixtures();
        var queries = List.of(
                fixtures.query(
                        StatementType.SELECT,
                        "SELECT * FROM customer WHERE id = 1",
                        "select * from customer where id = ?",
                        "customer"),
                fixtures.query(
                        StatementType.SELECT,
                        "SELECT * FROM customer WHERE id = 2",
                        "select * from customer where id = ?",
                        "customer"),
                fixtures.query(
                        StatementType.UPDATE,
                        "UPDATE customer SET name = 'x'",
                        "update customer set name = ?",
                        "customer"));

        QuerySnapshot snapshot = QuerySnapshots.of(queries);

        expect.serializer(new QuerySnapshotSerializer()).toMatchSnapshot(snapshot);
    }
}
