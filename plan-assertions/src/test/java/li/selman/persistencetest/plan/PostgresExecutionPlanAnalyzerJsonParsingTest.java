package li.selman.persistencetest.plan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit-tests {@link PostgresExecutionPlanAnalyzer#parseExplainJson} directly against hand-crafted
 * {@code EXPLAIN (FORMAT JSON)} output, independent of a real database connection - see
 * {@code PostgresExecutionPlanAnalyzerIntegrationTest} for the real-Postgres round trip.
 */
class PostgresExecutionPlanAnalyzerJsonParsingTest {

    @Test
    void parsesALeafNode() {
        String json = """
                [
                  {
                    "Plan": {
                      "Node Type": "Seq Scan",
                      "Relation Name": "customer",
                      "Plan Rows": 1000,
                      "Actual Rows": 950
                    },
                    "Planning Time": 0.1,
                    "Execution Time": 5.2
                  }
                ]
                """;

        ExecutionPlan plan = PostgresExecutionPlanAnalyzer.parseExplainJson(json);

        assertThat(plan.root().nodeType()).isEqualTo("Seq Scan");
        assertThat(plan.root().relationName()).isEqualTo("customer");
        assertThat(plan.root().indexName()).isNull();
        assertThat(plan.root().planRows()).isEqualTo(1000);
        assertThat(plan.root().actualRows()).isEqualTo(950);
        assertThat(plan.root().children()).isEmpty();
    }

    @Test
    void parsesNestedPlansIntoChildren() {
        String json = """
                [
                  {
                    "Plan": {
                      "Node Type": "Nested Loop",
                      "Plan Rows": 5,
                      "Actual Rows": 5,
                      "Plans": [
                        {
                          "Node Type": "Seq Scan",
                          "Relation Name": "orders",
                          "Plan Rows": 100,
                          "Actual Rows": 100
                        },
                        {
                          "Node Type": "Index Scan",
                          "Relation Name": "customer",
                          "Index Name": "idx_customer_email",
                          "Plan Rows": 1,
                          "Actual Rows": 1
                        }
                      ]
                    }
                  }
                ]
                """;

        ExecutionPlan plan = PostgresExecutionPlanAnalyzer.parseExplainJson(json);

        assertThat(plan.root().nodeType()).isEqualTo("Nested Loop");
        assertThat(plan.root().children()).extracting(PlanNode::nodeType).containsExactly("Seq Scan", "Index Scan");
        assertThat(plan.usesIndex("idx_customer_email")).isTrue();
        assertThat(plan.usesAnyIndexOn("customer")).isTrue();
        assertThat(plan.avoidsSequentialScan()).isFalse();
    }

    @Test
    void missingOptionalFieldsDefaultSensibly() {
        String json = """
                [
                  {
                    "Plan": {
                      "Node Type": "Result",
                      "Plan Rows": 1,
                      "Actual Rows": 1
                    }
                  }
                ]
                """;

        ExecutionPlan plan = PostgresExecutionPlanAnalyzer.parseExplainJson(json);

        assertThat(plan.root().relationName()).isNull();
        assertThat(plan.root().indexName()).isNull();
        assertThat(plan.root().children()).isEmpty();
    }
}
