package li.selman.persistencetest.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.Test;

class SnapshotNormalizerTest {

    @Test
    void replacesUuidLiterals() {
        var snapshot = snapshotWithSql("select * from customer where id = '4f9d1c1a-2b3d-4e5f-8a9b-0c1d2e3f4a5b'");

        var normalizer = SnapshotNormalizer.builder().replaceUuid("<uuid>").build();

        assertThat(normalizer.transform(snapshot).queries().get(0).normalizedSql())
                .isEqualTo("select * from customer where id = '<uuid>'");
    }

    @Test
    void replacesTimestampLiterals() {
        var snapshot = snapshotWithSql("select * from customer where created_at = '2026-07-26 12:34:56.789'");

        var normalizer =
                SnapshotNormalizer.builder().replaceTimestamp("<timestamp>").build();

        assertThat(normalizer.transform(snapshot).queries().get(0).normalizedSql())
                .isEqualTo("select * from customer where created_at = '<timestamp>'");
    }

    @Test
    void replacesGeneratedIdLiterals() {
        var snapshot = snapshotWithSql("select * from customer where id = 42");

        var normalizer =
                SnapshotNormalizer.builder().replaceGeneratedIds("<id>").build();

        assertThat(normalizer.transform(snapshot).queries().get(0).normalizedSql())
                .isEqualTo("select * from customer where id = <id>");
    }

    @Test
    void appliesUuidAndTimestampBeforeGeneratedIdsRegardlessOfBuilderCallOrder() {
        var snapshot = snapshotWithSql("select * from customer where created_at = '2026-07-26 12:34:56'");

        // Called out of "logical" order on purpose - replaceGeneratedIds first - to prove the fixed
        // internal ordering, not builder call order, is what determines execution order.
        var normalizer = SnapshotNormalizer.builder()
                .replaceGeneratedIds("<id>")
                .replaceTimestamp("<timestamp>")
                .build();

        assertThat(normalizer.transform(snapshot).queries().get(0).normalizedSql())
                .isEqualTo("select * from customer where created_at = '<timestamp>'");
    }

    @Test
    void ignoreSchemasStripsSchemaPrefixFromTables() {
        var entry = new QuerySnapshotEntry(
                StatementType.SELECT, List.of("public.customer", "orders"), "select * from public.customer", 1);
        var snapshot = new QuerySnapshot(List.of(entry));

        var normalizer = SnapshotNormalizer.builder().ignoreSchemas().build();

        assertThat(normalizer.transform(snapshot).queries().get(0).tables()).containsExactly("customer", "orders");
    }

    @Test
    void ignoreAliasesAndIgnoreCommentsAreNoOps() {
        var snapshot = snapshotWithSql("select * from customer");

        var normalizer =
                SnapshotNormalizer.builder().ignoreAliases().ignoreComments().build();

        assertThat(normalizer.transform(snapshot)).isEqualTo(snapshot);
    }

    @Test
    void withNoRulesConfiguredLeavesTheSnapshotUnchanged() {
        var snapshot = snapshotWithSql("select * from customer where id = 42");

        assertThat(SnapshotNormalizer.builder().build().transform(snapshot)).isEqualTo(snapshot);
    }

    private static QuerySnapshot snapshotWithSql(String sql) {
        return new QuerySnapshot(List.of(new QuerySnapshotEntry(StatementType.SELECT, List.of("customer"), sql, 1)));
    }
}
