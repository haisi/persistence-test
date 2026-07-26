package li.selman.persistencetest.analysis;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import li.selman.persistencetest.core.BindParameter;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;

/** Builds minimal {@link CapturedQuery} instances for tests, without the full JDBC capture pipeline. */
final class CapturedQueryFixtures {

    private final AtomicLong sequence = new AtomicLong();

    CapturedQuery query(StatementType type, String normalizedSql, String... tables) {
        return query(type, normalizedSql, List.of(), tables);
    }

    // affectedRows/exception are @Nullable on CapturedQuery; see the note in query-capture's
    // QueryCaptureListener for why NullAway doesn't see that cross-module.
    @SuppressWarnings({"NullAway", "NullArgumentForNonNullParameter"})
    CapturedQuery query(StatementType type, String normalizedSql, List<BindParameter> parameters, String... tables) {
        return new CapturedQuery(
                sequence.getAndIncrement(),
                Instant.now(),
                normalizedSql,
                normalizedSql,
                type,
                List.of(tables),
                parameters,
                Duration.ofMillis(1),
                null,
                null,
                "main",
                "conn-1");
    }
}
