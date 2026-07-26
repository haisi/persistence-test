package li.selman.persistencetest.plan;

import org.assertj.core.api.AbstractAssert;

/** Assertions over an {@link ExecutionPlan}. Obtain one via {@link PlanAssertions#assertThatPlanOf}. */
public final class ExecutionPlanAssert extends AbstractAssert<ExecutionPlanAssert, ExecutionPlan> {

    ExecutionPlanAssert(ExecutionPlan actual) {
        super(actual, ExecutionPlanAssert.class);
    }

    public ExecutionPlanAssert usesIndex() {
        isNotNull();
        if (!actual.usesIndex()) {
            failWithMessage("Expected the plan to use an index, but it didn't:%n%s", actual.root());
        }
        return this;
    }

    public ExecutionPlanAssert usesIndex(String indexName) {
        isNotNull();
        if (!actual.usesIndex(indexName)) {
            failWithMessage("Expected the plan to use index '%s', but it didn't:%n%s", indexName, actual.root());
        }
        return this;
    }

    public ExecutionPlanAssert avoidsSequentialScan() {
        isNotNull();
        if (!actual.avoidsSequentialScan()) {
            failWithMessage("Expected the plan to avoid a sequential scan, but it used one:%n%s", actual.root());
        }
        return this;
    }

    public ExecutionPlanAssert estimatedRowsLessThan(long rows) {
        isNotNull();
        if (!actual.estimatedRowsLessThan(rows)) {
            failWithMessage(
                    "Expected the plan's estimated row count to be less than %d, but it was %d:%n%s",
                    rows, actual.root().planRows(), actual.root());
        }
        return this;
    }

    public ExecutionPlanAssert usesAnyIndexOn(String table) {
        isNotNull();
        if (!actual.usesAnyIndexOn(table)) {
            failWithMessage(
                    "Expected the plan to use an index on table '%s', but it didn't:%n%s", table, actual.root());
        }
        return this;
    }
}
