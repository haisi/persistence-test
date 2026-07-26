/**
 * Database-agnostic domain model for a single captured SQL execution.
 *
 * <p>Types in this package have no dependency on JDBC, Hibernate, or Spring: they describe what happened
 * ({@link li.selman.persistencetest.core.CapturedQuery}), not how it was intercepted. That job belongs to
 * {@code query-capture}.
 */
@NullMarked
package li.selman.persistencetest.core;

import org.jspecify.annotations.NullMarked;
