package li.selman.persistencetest.snapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;

/** Builds minimal {@link CapturedQuery} instances for tests, without the full JDBC capture pipeline. */
final class CapturedQueryFixtures {

    private final AtomicLong sequence = new AtomicLong();

    CapturedQuery query(StatementType type, String sql, String normalizedSql, String... tables) {
        return newCapturedQuery(sequence.getAndIncrement(), type, sql, normalizedSql, tables);
    }

    // affectedRows/exception are @Nullable on CapturedQuery; see the note in query-capture's
    // QueryCaptureListener for why NullAway doesn't see that cross-module.
    @SuppressWarnings({"NullAway", "NullArgumentForNonNullParameter"})
    private static CapturedQuery newCapturedQuery(
            long sequence, StatementType type, String sql, String normalizedSql, String... tables) {
        return new CapturedQuery(
                sequence,
                Instant.now(),
                sql,
                normalizedSql,
                type,
                List.of(tables),
                List.of(),
                Duration.ofMillis(1),
                null,
                null,
                "main",
                "conn-1");
    }
}
