package li.selman.persistencetest.core;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A single SQL statement execution captured at the JDBC layer, with everything needed to analyze, assert
 * on, or snapshot it.
 *
 * <p>Immutable and framework-agnostic: nothing here knows about Hibernate, Spring, or a specific JDBC
 * driver. {@code query-capture} is responsible for producing instances of this type from a real
 * {@link java.sql.Connection}.
 *
 * @param sequence 0-based order in which this statement was submitted to the driver, relative to other
 *     statements captured in the same session. Used to reconstruct an execution timeline.
 * @param timestamp wall-clock time execution started.
 * @param sql the exact SQL text passed to the driver, unmodified.
 * @param normalizedSql a normalized, deterministic rendering of {@code sql}; see
 *     {@code li.selman.persistencetest.core.normalize.SqlNormalizer}.
 * @param statementType the kind of statement this is.
 * @param parameters bind parameters in position order; empty for statements with none.
 * @param duration wall-clock execution time, or {@code null} if execution never completed (e.g. the
 *     statement threw before the driver reported timing).
 * @param affectedRows rows affected as reported by the driver (JDBC update count), or {@code null} when not
 *     applicable (e.g. a {@code SELECT}) or not available.
 * @param exception the exception the statement failed with, or {@code null} if it succeeded.
 * @param threadName name of the thread that executed the statement.
 * @param connectionId an identifier stable for the lifetime of the underlying {@link java.sql.Connection},
 *     used to correlate statements executed on the same connection (e.g. within one transaction).
 */
public record CapturedQuery(
        long sequence,
        Instant timestamp,
        String sql,
        String normalizedSql,
        StatementType statementType,
        List<BindParameter> parameters,
        @Nullable Duration duration,
        @Nullable Long affectedRows,
        @Nullable Throwable exception,
        String threadName,
        String connectionId) {

    @SuppressWarnings("Var") // reassigning a compact constructor's implicit parameter for a defensive copy
    // is the standard record idiom; it can't be annotated @Var since the underlying field is final.
    public CapturedQuery {
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be >= 0, got " + sequence);
        }
        parameters = List.copyOf(parameters);
    }

    /** Whether this statement failed. */
    public boolean isFailure() {
        return exception != null;
    }

    /** Whether this is a data-modifying statement ({@code INSERT}/{@code UPDATE}/{@code DELETE}). */
    public boolean isMutation() {
        return statementType == StatementType.INSERT
                || statementType == StatementType.UPDATE
                || statementType == StatementType.DELETE;
    }
}
