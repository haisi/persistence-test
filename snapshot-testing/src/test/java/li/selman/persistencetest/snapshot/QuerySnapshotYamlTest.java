package li.selman.persistencetest.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.Test;

class QuerySnapshotYamlTest {

    @Test
    void rendersEmptySnapshotAsEmptyList() {
        assertThat(QuerySnapshotYaml.render(new QuerySnapshot(List.of()))).isEqualTo("queries: []");
    }

    @Test
    void rendersEntryWithTablesAndBlockLiteralSql() {
        var snapshot = new QuerySnapshot(List.of(new QuerySnapshotEntry(
                StatementType.SELECT, List.of("customer", "orders"), "select * from customer", 1)));

        assertThat(QuerySnapshotYaml.render(snapshot)).isEqualTo("""
                        queries:
                          - type: SELECT
                            tables:
                              - customer
                              - orders
                            normalizedSql: |
                              select * from customer
                            count: 1""");
    }

    @Test
    void rendersEmptyTablesAsEmptyList() {
        var snapshot = new QuerySnapshot(
                List.of(new QuerySnapshotEntry(StatementType.OTHER, List.of(), "set search_path = x", 1)));

        assertThat(QuerySnapshotYaml.render(snapshot)).contains("tables: []");
    }

    @Test
    void isDeterministicAcrossRepeatedCalls() {
        var snapshot = new QuerySnapshot(List.of(
                new QuerySnapshotEntry(StatementType.SELECT, List.of("customer"), "select * from customer", 2),
                new QuerySnapshotEntry(StatementType.UPDATE, List.of("customer"), "update customer set x", 1)));

        assertThat(QuerySnapshotYaml.render(snapshot)).isEqualTo(QuerySnapshotYaml.render(snapshot));
    }
}
