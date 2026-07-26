package li.selman.persistencetest.capture;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QueryCaptureContextTest {

    @AfterEach
    void resetSharedThreadLocalState() {
        QueryCaptureContext.current().reset();
    }

    @Test
    void recordsQueriesInOrder() {
        var context = QueryCaptureContext.current();

        context.record(query(context, "select 1"));
        context.record(query(context, "select 2"));

        assertThat(context.capturedQueries()).extracting(CapturedQuery::sql).containsExactly("select 1", "select 2");
        assertThat(context.capturedQueries())
                .extracting(CapturedQuery::sequence)
                .containsExactly(0L, 1L);
    }

    @Test
    void resetClearsQueriesAndRestartsSequence() {
        var context = QueryCaptureContext.current();
        context.record(query(context, "select 1"));

        context.reset();
        context.record(query(context, "select 2"));

        assertThat(context.capturedQueries()).extracting(CapturedQuery::sql).containsExactly("select 2");
        assertThat(context.capturedQueries().get(0).sequence()).isZero();
    }

    @Test
    void capturedQueriesIsDefensivelyCopied() {
        var context = QueryCaptureContext.current();
        context.record(query(context, "select 1"));

        List<CapturedQuery> snapshot = context.capturedQueries();
        context.record(query(context, "select 2"));

        assertThat(snapshot).hasSize(1);
    }

    @Test
    void currentIsIsolatedPerThread() throws InterruptedException {
        var mainThreadContext = QueryCaptureContext.current();
        mainThreadContext.record(query(mainThreadContext, "select from main thread"));

        var otherThreadCaptured = new java.util.concurrent.atomic.AtomicReference<List<CapturedQuery>>();
        var latch = new CountDownLatch(1);
        Thread other = new Thread(() -> {
            otherThreadCaptured.set(QueryCaptureContext.current().capturedQueries());
            latch.countDown();
        });
        other.start();
        latch.await();

        assertThat(otherThreadCaptured.get()).isEmpty();
        assertThat(mainThreadContext.capturedQueries()).hasSize(1);
    }

    // affectedRows/exception are @Nullable on CapturedQuery, but that annotation doesn't survive on the
    // compiled canonical constructor's parameters when read cross-module (query-capture -> the already
    // compiled persistence-test-core classes) - see the longer note in QueryCaptureListener. Harmless here:
    // this is test data, not the thing under test.
    @SuppressWarnings({"NullAway", "NullArgumentForNonNullParameter"})
    private static CapturedQuery query(QueryCaptureContext context, String sql) {
        return new CapturedQuery(
                context.nextSequence(),
                Instant.now(),
                sql,
                sql,
                StatementType.SELECT,
                List.of(),
                List.of(),
                Duration.ofMillis(1),
                null,
                null,
                Thread.currentThread().getName(),
                "conn-1");
    }
}
