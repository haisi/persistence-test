package li.selman.persistencetest.plan;

import java.sql.Connection;
import li.selman.persistencetest.core.CapturedQuery;

/** Database-specific SPI for obtaining an {@link ExecutionPlan} for a query. */
public interface ExecutionPlanAnalyzer {

    /**
     * Explains {@code query} on {@code connection}.
     *
     * <p>Implementations that need to actually execute the statement to get real (not just estimated) row
     * counts (as {@link PostgresExecutionPlanAnalyzer} does) must never leave side effects behind, even for
     * data-modifying statements - see {@link PostgresExecutionPlanAnalyzer}'s Javadoc for how it guarantees
     * that.
     *
     * @throws ExecutionPlanException if the plan can't be obtained.
     */
    ExecutionPlan explain(Connection connection, CapturedQuery query);
}
