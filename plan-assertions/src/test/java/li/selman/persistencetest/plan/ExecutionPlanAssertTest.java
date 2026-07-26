package li.selman.persistencetest.plan;

import static li.selman.persistencetest.plan.PlanAssertions.assertThatPlanOf;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.Test;

class ExecutionPlanAssertTest {

    // A fake analyzer that ignores its arguments entirely, so these two never need to be real - a dynamic
    // proxy Connection (never invoked) and a minimal real CapturedQuery, avoiding both a Mockito dependency
    // and passing null for non-@Nullable parameters.
    private static final Connection UNUSED_CONNECTION = (Connection) Proxy.newProxyInstance(
            ExecutionPlanAssertTest.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
                throw new UnsupportedOperationException("not used by the fixed analyzer in this test");
            });

    @SuppressWarnings({"NullAway", "NullArgumentForNonNullParameter"})
    private static final CapturedQuery UNUSED_QUERY = new CapturedQuery(
            0,
            Instant.EPOCH,
            "select 1",
            "select 1",
            StatementType.SELECT,
            List.of(),
            List.of(),
            Duration.ZERO,
            null,
            null,
            "main",
            "conn-1");

    @Test
    void usesIndexFailsWithPlanDetailsWhenPlanIsASeqScan() {
        var analyzer = fixedAnalyzer(new PlanNode("Seq Scan", "customer", null, 100, 100, List.of()));

        assertThatThrownBy(() -> assertThatPlanOf(UNUSED_CONNECTION, UNUSED_QUERY, analyzer)
                        .usesIndex())
                .hasMessageContaining("Expected the plan to use an index, but it didn't")
                .hasMessageContaining("Seq Scan");
    }

    @Test
    void usesIndexPassesWhenPlanUsesAnyIndexVariant() {
        var analyzer = fixedAnalyzer(new PlanNode("Index Only Scan", "customer", "idx_email", 1, 1, List.of()));

        assertThatPlanOf(UNUSED_CONNECTION, UNUSED_QUERY, analyzer).usesIndex();
    }

    @Test
    void usesIndexNamedFailsWhenTheWrongIndexIsUsed() {
        var analyzer = fixedAnalyzer(new PlanNode("Index Scan", "customer", "idx_email", 1, 1, List.of()));

        assertThatThrownBy(() -> assertThatPlanOf(UNUSED_CONNECTION, UNUSED_QUERY, analyzer)
                        .usesIndex("idx_name"))
                .hasMessageContaining("Expected the plan to use index 'idx_name'");
    }

    @Test
    void avoidsSequentialScanFailsWhenPlanContainsASeqScan() {
        var analyzer = fixedAnalyzer(new PlanNode("Seq Scan", "customer", null, 100, 100, List.of()));

        assertThatThrownBy(() -> assertThatPlanOf(UNUSED_CONNECTION, UNUSED_QUERY, analyzer)
                        .avoidsSequentialScan())
                .hasMessageContaining("Expected the plan to avoid a sequential scan, but it used one");
    }

    @Test
    void estimatedRowsLessThanFailsWithActualEstimateInMessage() {
        var analyzer = fixedAnalyzer(new PlanNode("Seq Scan", "customer", null, 5000, 5000, List.of()));

        assertThatThrownBy(() -> assertThatPlanOf(UNUSED_CONNECTION, UNUSED_QUERY, analyzer)
                        .estimatedRowsLessThan(10))
                .hasMessageContaining("Expected the plan's estimated row count to be less than 10, but it was 5000");
    }

    @Test
    void usesAnyIndexOnFailsWhenTableIsScannedWithoutAnIndex() {
        var analyzer = fixedAnalyzer(new PlanNode("Seq Scan", "customer", null, 100, 100, List.of()));

        assertThatThrownBy(() -> assertThatPlanOf(UNUSED_CONNECTION, UNUSED_QUERY, analyzer)
                        .usesAnyIndexOn("customer"))
                .hasMessageContaining("Expected the plan to use an index on table 'customer', but it didn't");
    }

    private static ExecutionPlanAnalyzer fixedAnalyzer(PlanNode root) {
        return (Connection connection, CapturedQuery query) -> new ExecutionPlan(root);
    }
}
