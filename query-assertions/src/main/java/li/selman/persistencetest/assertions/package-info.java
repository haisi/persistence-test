/**
 * Fluent AssertJ-style DSL over captured queries.
 *
 * <p>Start with {@code import static li.selman.persistencetest.assertions.QueryAssertions.assertThatQueries;}
 * - with no arguments it reads from {@code QueryCaptureContext.current()}, so it works directly against
 * whatever was captured during the current test without any explicit wiring:
 *
 * <pre>{@code
 * assertThatQueries()
 *     .selects(2)
 *     .containsNoDelete()
 *     .hasNoNPlusOne();
 * }</pre>
 */
@NullMarked
package li.selman.persistencetest.assertions;

import org.jspecify.annotations.NullMarked;
