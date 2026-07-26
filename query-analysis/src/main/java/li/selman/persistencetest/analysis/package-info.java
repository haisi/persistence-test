/**
 * Reusable analyzers over a list of {@link li.selman.persistencetest.core.CapturedQuery}: statistics,
 * duplicate-query detection, repeated-query-shape/N+1 detection, and accessed tables.
 *
 * <p>Pure functions over an already-captured list - no dependency on JUnit, AssertJ, or
 * {@code QueryCaptureContext} - so they're usable independently of {@code query-assertions}. Start with
 * {@link li.selman.persistencetest.analysis.QueryAnalyzer}.
 */
@NullMarked
package li.selman.persistencetest.analysis;

import org.jspecify.annotations.NullMarked;
