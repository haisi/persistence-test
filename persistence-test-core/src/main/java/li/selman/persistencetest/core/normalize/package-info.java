/**
 * SQL normalization: turning raw driver-bound SQL text into a form that is stable across harmless Hibernate
 * formatting changes but still distinguishes real semantic differences.
 *
 * <p>See {@link li.selman.persistencetest.core.normalize.SqlNormalizer} for the extension point and
 * {@link li.selman.persistencetest.core.normalize.JSqlParserSqlNormalizer} for the built-in implementation
 * and its documented limitations.
 */
@NullMarked
package li.selman.persistencetest.core.normalize;

import org.jspecify.annotations.NullMarked;
