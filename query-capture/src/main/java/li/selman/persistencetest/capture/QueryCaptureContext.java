package li.selman.persistencetest.capture;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import li.selman.persistencetest.core.CapturedQuery;

/**
 * Accumulates {@link CapturedQuery} instances for the current thread.
 *
 * <p>Backed by a thread-local rather than a single shared list: JDBC calls are synchronous on the calling
 * thread, so for the common case of a test method that talks to the database directly (or through
 * Hibernate/Spring Data JPA/JdbcTemplate, still synchronously on that same thread), {@link #current()}
 * transparently gives each test its own isolated view with no explicit wiring, and holds up correctly under
 * parallel JUnit 5 test execution and Java 25 virtual threads (each carries its own thread-locals).
 *
 * <p><b>Known limitation:</b> if code under test hands work off to a different thread (an {@code @Async}
 * method, an executor, a reactive scheduler) and that thread executes queries, those queries are recorded
 * against <em>that thread's</em> context, not the test thread's. They will not appear in
 * {@link #capturedQueries()} as called from the test method. Propagating capture across thread handoffs
 * would need cooperation from whatever does the handoff (e.g. wrapping the executor) and is not attempted
 * here.
 */
public final class QueryCaptureContext {

    private static final ThreadLocal<QueryCaptureContext> CURRENT = ThreadLocal.withInitial(QueryCaptureContext::new);

    private final List<CapturedQuery> queries = new CopyOnWriteArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    private QueryCaptureContext() {}

    /** The capture context for the calling thread. */
    public static QueryCaptureContext current() {
        return CURRENT.get();
    }

    /** Queries captured on this thread since the last {@link #reset()}, in execution order. */
    public List<CapturedQuery> capturedQueries() {
        return List.copyOf(queries);
    }

    /** Discards captured queries and restarts execution-order numbering from zero. */
    public void reset() {
        queries.clear();
        sequence.set(0);
    }

    void record(CapturedQuery query) {
        queries.add(query);
    }

    long nextSequence() {
        return sequence.getAndIncrement();
    }
}
