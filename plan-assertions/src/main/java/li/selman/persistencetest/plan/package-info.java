/**
 * Execution-plan assertions, backed by a per-database {@link li.selman.persistencetest.plan.ExecutionPlanAnalyzer}
 * SPI. {@link li.selman.persistencetest.plan.PostgresExecutionPlanAnalyzer} is the PostgreSQL
 * implementation, using {@code EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)}.
 *
 * <p>Never compares raw plan text: {@link li.selman.persistencetest.plan.ExecutionPlan} derives stable
 * facts (does this use an index, does it avoid a sequential scan, ...) from the plan tree instead, since
 * raw plan output is exactly the kind of thing that changes on harmless Postgres version/statistics
 * differences.
 */
@NullMarked
package li.selman.persistencetest.plan;

import org.jspecify.annotations.NullMarked;
