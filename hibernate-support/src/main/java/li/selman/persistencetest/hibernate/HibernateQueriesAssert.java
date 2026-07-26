package li.selman.persistencetest.hibernate;

import java.util.List;
import java.util.Locale;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;
import org.assertj.core.api.AbstractAssert;

/**
 * Entity-aware assertions over a list of {@link CapturedQuery}, resolving entity classes to table names via
 * an {@link EntityTableResolver}. Obtain one via {@link HibernateAssertions#assertThatQueries}.
 *
 * <p>A separate assert type from {@code query-assertions}' {@code QueriesAssert} rather than an extension
 * of it: Java has no mechanism to retroactively add methods to another module's fluent-assertion type, so
 * entity-aware and table-name-based assertions live in their own chain. Both read from the same
 * {@code QueryCaptureContext}, so combining
 * {@code assertThatQueries()....; HibernateAssertions.assertThatQueries(resolver)....} in the same test is
 * natural even without shared inheritance.
 */
public final class HibernateQueriesAssert extends AbstractAssert<HibernateQueriesAssert, List<CapturedQuery>> {

    private final EntityTableResolver resolver;

    HibernateQueriesAssert(List<CapturedQuery> actual, EntityTableResolver resolver) {
        super(actual, HibernateQueriesAssert.class);
        this.resolver = resolver;
    }

    public HibernateQueriesAssert containsSelect(Class<?> entityType) {
        return contains(StatementType.SELECT, entityType);
    }

    public HibernateQueriesAssert containsInsert(Class<?> entityType) {
        return contains(StatementType.INSERT, entityType);
    }

    public HibernateQueriesAssert containsUpdate(Class<?> entityType) {
        return contains(StatementType.UPDATE, entityType);
    }

    public HibernateQueriesAssert containsDelete(Class<?> entityType) {
        return contains(StatementType.DELETE, entityType);
    }

    private HibernateQueriesAssert contains(StatementType type, Class<?> entityType) {
        isNotNull();
        String table = resolver.tableNameOf(entityType).toLowerCase(Locale.ROOT);
        boolean found = actual.stream()
                .anyMatch(
                        query -> query.statementType() == type && query.tables().contains(table));
        if (!found) {
            failWithMessage(
                    "Expected a %s query against %s (table '%s') but found none among %d captured queries.",
                    type, entityType.getSimpleName(), table, actual.size());
        }
        return this;
    }
}
