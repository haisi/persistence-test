package li.selman.persistencetest.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.Test;

class QuerySnapshotsTest {

    private final CapturedQueryFixtures fixtures = new CapturedQueryFixtures();

    @Test
    void groupsIdenticalShapesWithACount() {
        var queries = List.of(
                fixtures.query(
                        StatementType.SELECT,
                        "select * from order_item where order_id = 1",
                        "select * from order_item where order_id = ?",
                        "order_item"),
                fixtures.query(
                        StatementType.SELECT,
                        "select * from order_item where order_id = 2",
                        "select * from order_item where order_id = ?",
                        "order_item"));

        QuerySnapshot snapshot = QuerySnapshots.of(queries);

        assertThat(snapshot.queries()).hasSize(1);
        assertThat(snapshot.queries().get(0).count()).isEqualTo(2);
    }

    @Test
    void preservesFirstOccurrenceOrderOfDistinctShapes() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select * from orders", "select * from orders", "orders"),
                fixtures.query(StatementType.UPDATE, "update customer set x", "update customer set x", "customer"));

        QuerySnapshot snapshot = QuerySnapshots.of(queries);

        assertThat(snapshot.queries())
                .extracting(QuerySnapshotEntry::statementType)
                .containsExactly(StatementType.SELECT, StatementType.UPDATE);
    }

    @Test
    void semanticLevelUsesNormalizedSql() {
        var queries = List.of(fixtures.query(
                StatementType.SELECT, "SELECT   *  FROM customer", "select * from customer", "customer"));

        QuerySnapshot snapshot = QuerySnapshots.of(queries, SnapshotLevel.SEMANTIC);

        assertThat(snapshot.queries().get(0).normalizedSql()).isEqualTo("select * from customer");
    }

    @Test
    void sqlLevelUsesRawSqlWithOnlyWhitespaceCollapsed() {
        var queries = List.of(fixtures.query(
                StatementType.SELECT, "SELECT   *  FROM customer", "select * from customer", "customer"));

        QuerySnapshot snapshot = QuerySnapshots.of(queries, SnapshotLevel.SQL);

        assertThat(snapshot.queries().get(0).normalizedSql()).isEqualTo("SELECT * FROM customer");
    }

    @Test
    void emptyInputYieldsEmptySnapshot() {
        assertThat(QuerySnapshots.of(List.of()).queries()).isEmpty();
    }
}
