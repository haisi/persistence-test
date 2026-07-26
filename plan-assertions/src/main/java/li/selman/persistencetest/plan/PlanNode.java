package li.selman.persistencetest.plan;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * One node of an execution plan tree.
 *
 * @param nodeType the planner's name for this operation (e.g. {@code "Seq Scan"}, {@code "Index Scan"},
 *     {@code "Nested Loop"}).
 * @param relationName the table/index this node scans, or {@code null} for nodes that don't scan one
 *     directly (joins, aggregates, ...).
 * @param indexName the index this node uses, or {@code null} if it doesn't use one.
 * @param planRows the planner's row-count estimate for this node.
 * @param actualRows rows actually produced, as measured by {@code ANALYZE}.
 * @param children child plan nodes (e.g. the two sides of a join).
 */
public record PlanNode(
        String nodeType,
        @Nullable String relationName,
        @Nullable String indexName,
        long planRows,
        long actualRows,
        List<PlanNode> children) {

    @SuppressWarnings("Var") // reassigning a compact constructor's implicit parameter for a defensive copy
    // is the standard record idiom; it can't be annotated @Var since the underlying field is final.
    public PlanNode {
        children = List.copyOf(children);
    }
}
