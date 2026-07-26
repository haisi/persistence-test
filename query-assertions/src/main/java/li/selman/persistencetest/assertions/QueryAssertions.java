package li.selman.persistencetest.assertions;

import java.util.List;
import li.selman.persistencetest.capture.QueryCaptureContext;
import li.selman.persistencetest.core.CapturedQuery;

/** Entry point for the query-assertions DSL. */
public final class QueryAssertions {

    private QueryAssertions() {}

    /** Asserts on whatever has been captured on the current thread since the last reset. */
    public static QueriesAssert assertThatQueries() {
        return assertThatQueries(QueryCaptureContext.current().capturedQueries());
    }

    /** Asserts on an explicit list of captured queries, rather than the ambient capture context. */
    public static QueriesAssert assertThatQueries(List<CapturedQuery> queries) {
        return new QueriesAssert(queries);
    }
}
