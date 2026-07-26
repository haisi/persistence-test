package li.selman.persistencetest.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Stable, derived facts about a query's execution plan - never the raw plan text, which changes across
 * harmless Postgres version/statistics differences.
 *
 * @param root the plan's root node.
 */
public record ExecutionPlan(PlanNode root) {

    /** Whether any node in the plan uses an index scan of any kind (index, index-only, or bitmap). */
    public boolean usesIndex() {
        return anyNode(node -> node.nodeType().contains("Index"));
    }

    /** Whether any node in the plan uses the index named {@code indexName}. */
    public boolean usesIndex(String indexName) {
        return anyNode(node -> indexName.equals(node.indexName()));
    }

    /** Whether no node in the plan is a sequential scan. */
    public boolean avoidsSequentialScan() {
        return !anyNode(node -> "Seq Scan".equals(node.nodeType()));
    }

    /** Whether the root node's planner row estimate is less than {@code rows}. */
    public boolean estimatedRowsLessThan(long rows) {
        return root.planRows() < rows;
    }

    /** Whether any node uses an index scan against {@code table} (case-insensitive). */
    public boolean usesAnyIndexOn(String table) {
        return anyNode(node -> node.nodeType().contains("Index") && table.equalsIgnoreCase(node.relationName()));
    }

    /** Every node in the plan, in depth-first order, root first. */
    public List<PlanNode> allNodes() {
        List<PlanNode> nodes = new ArrayList<>();
        collect(root, nodes);
        return List.copyOf(nodes);
    }

    private boolean anyNode(Predicate<PlanNode> predicate) {
        return allNodes().stream().anyMatch(predicate);
    }

    private static void collect(PlanNode node, List<PlanNode> into) {
        into.add(node);
        for (PlanNode child : node.children()) {
            collect(child, into);
        }
    }
}
