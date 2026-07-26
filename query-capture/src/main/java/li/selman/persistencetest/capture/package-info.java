/**
 * Captures every SQL statement executed through a {@link javax.sql.DataSource} by wrapping it with
 * datasource-proxy, so capture works transparently for Spring Data JPA, Hibernate, JdbcTemplate, and plain
 * JDBC alike - none of them are parsed or special-cased.
 *
 * <p>Start with {@link li.selman.persistencetest.capture.QueryCapture#wrap(javax.sql.DataSource)}.
 *
 * <p>Captured queries accumulate in a thread-local {@link li.selman.persistencetest.capture.QueryCaptureContext};
 * see its Javadoc for what that does and does not guarantee under concurrency.
 */
@NullMarked
package li.selman.persistencetest.capture;

import org.jspecify.annotations.NullMarked;
