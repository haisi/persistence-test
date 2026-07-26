package li.selman.persistencetest.hibernate;

import java.util.List;
import li.selman.persistencetest.capture.QueryCaptureContext;
import li.selman.persistencetest.core.CapturedQuery;

/** Entry point for entity-aware query assertions. */
public final class HibernateAssertions {

    private HibernateAssertions() {}

    /** Asserts on whatever has been captured on the current thread since the last reset. */
    public static HibernateQueriesAssert assertThatQueries(EntityTableResolver resolver) {
        return assertThatQueries(QueryCaptureContext.current().capturedQueries(), resolver);
    }

    /** Asserts on an explicit list of captured queries, rather than the ambient capture context. */
    public static HibernateQueriesAssert assertThatQueries(List<CapturedQuery> queries, EntityTableResolver resolver) {
        return new HibernateQueriesAssert(queries, resolver);
    }
}
