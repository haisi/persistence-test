package li.selman.persistencetest.plan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class ExecutionPlanTest {

    @Test
    void usesIndexIsTrueForAnyIndexScanVariant() {
        assertThat(planWithRoot(node("Index Scan", "customer", "idx_customer_email"))
                        .usesIndex())
                .isTrue();
        assertThat(planWithRoot(node("Index Only Scan", "customer", "idx_customer_email"))
                        .usesIndex())
                .isTrue();
        assertThat(planWithRoot(node("Bitmap Index Scan", null, "idx_customer_email"))
                        .usesIndex())
                .isTrue();
        assertThat(planWithRoot(node("Seq Scan", "customer", null)).usesIndex()).isFalse();
    }

    @Test
    void usesIndexNamedChecksTheSpecificIndex() {
        var plan = planWithRoot(node("Index Scan", "customer", "idx_customer_email"));

        assertThat(plan.usesIndex("idx_customer_email")).isTrue();
        assertThat(plan.usesIndex("idx_customer_name")).isFalse();
    }

    @Test
    void avoidsSequentialScanIsFalseWhenAnyNodeIsASeqScan() {
        var joinWithSeqScanChild =
                new PlanNode("Nested Loop", null, null, 10, 10, List.of(node("Seq Scan", "orders", null)));

        assertThat(new ExecutionPlan(joinWithSeqScanChild).avoidsSequentialScan())
                .isFalse();
        assertThat(planWithRoot(node("Index Scan", "customer", "idx")).avoidsSequentialScan())
                .isTrue();
    }

    @Test
    void estimatedRowsLessThanComparesTheRootNodesPlanRows() {
        var plan = new ExecutionPlan(new PlanNode("Seq Scan", "customer", null, 1000, 1000, List.of()));

        assertThat(plan.estimatedRowsLessThan(2000)).isTrue();
        assertThat(plan.estimatedRowsLessThan(500)).isFalse();
    }

    @Test
    void usesAnyIndexOnMatchesTableCaseInsensitively() {
        var plan = planWithRoot(node("Index Scan", "customer", "idx_customer_email"));

        assertThat(plan.usesAnyIndexOn("customer")).isTrue();
        assertThat(plan.usesAnyIndexOn("CUSTOMER")).isTrue();
        assertThat(plan.usesAnyIndexOn("orders")).isFalse();
    }

    @Test
    void allNodesReturnsTheWholeTreeDepthFirst() {
        var child1 = node("Seq Scan", "orders", null);
        var child2 = node("Index Scan", "customer", "idx_customer_email");
        var root = new PlanNode("Nested Loop", null, null, 20, 20, List.of(child1, child2));

        assertThat(new ExecutionPlan(root).allNodes())
                .extracting(PlanNode::nodeType)
                .containsExactly("Nested Loop", "Seq Scan", "Index Scan");
    }

    private static ExecutionPlan planWithRoot(PlanNode root) {
        return new ExecutionPlan(root);
    }

    private static PlanNode node(String nodeType, @Nullable String relationName, @Nullable String indexName) {
        return new PlanNode(nodeType, relationName, indexName, 1, 1, List.of());
    }
}
