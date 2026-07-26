package li.selman.persistencetest.plan;

import java.sql.Connection;
import li.selman.persistencetest.core.CapturedQuery;

/**
 * Entry point for execution-plan assertions.
 *
 * <pre>{@code
 * CapturedQuery lastSelect = assertThatQueries().lastSelect().capturedQuery(); // from query-assertions
 * assertThatPlanOf(connection, lastSelect).usesIndex();
 * }</pre>
 *
 * <p>Deliberately a separate entry point from {@code query-assertions}' {@code lastSelect()} rather than a
 * method chained directly onto it: obtaining a plan needs a live {@link Connection} that
 * {@code query-assertions} has no reason to know about, and Java has no mechanism to retroactively add
 * methods to another module's fluent-assertion type anyway.
 */
public final class PlanAssertions {

    private PlanAssertions() {}

    /** Uses {@link PostgresExecutionPlanAnalyzer}. */
    public static ExecutionPlanAssert assertThatPlanOf(Connection connection, CapturedQuery query) {
        return assertThatPlanOf(connection, query, new PostgresExecutionPlanAnalyzer());
    }

    /** Like {@link #assertThatPlanOf(Connection, CapturedQuery)}, with a caller-supplied analyzer. */
    public static ExecutionPlanAssert assertThatPlanOf(
            Connection connection, CapturedQuery query, ExecutionPlanAnalyzer analyzer) {
        return new ExecutionPlanAssert(analyzer.explain(connection, query));
    }
}
