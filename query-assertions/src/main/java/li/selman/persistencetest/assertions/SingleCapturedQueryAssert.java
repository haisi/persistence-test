package li.selman.persistencetest.assertions;

import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;
import org.assertj.core.api.AbstractAssert;

/**
 * Assertions on a single {@link CapturedQuery}, as returned by e.g. {@link QueriesAssert#lastSelect()}.
 *
 * <p>{@link #capturedQuery()} exposes the wrapped query so other modules (e.g. {@code plan-assertions}) can
 * build further assertions on top of it without this module needing to know about them.
 */
public final class SingleCapturedQueryAssert extends AbstractAssert<SingleCapturedQueryAssert, CapturedQuery> {

    SingleCapturedQueryAssert(CapturedQuery actual) {
        super(actual, SingleCapturedQueryAssert.class);
    }

    /** The query this assertion wraps. */
    public CapturedQuery capturedQuery() {
        isNotNull();
        return actual;
    }

    public SingleCapturedQueryAssert isSelect() {
        return hasType(StatementType.SELECT);
    }

    public SingleCapturedQueryAssert isInsert() {
        return hasType(StatementType.INSERT);
    }

    public SingleCapturedQueryAssert isUpdate() {
        return hasType(StatementType.UPDATE);
    }

    public SingleCapturedQueryAssert isDelete() {
        return hasType(StatementType.DELETE);
    }

    private SingleCapturedQueryAssert hasType(StatementType expected) {
        isNotNull();
        if (actual.statementType() != expected) {
            failWithMessage(
                    "Expected query to be %s but was %s:%n%s",
                    expected, actual.statementType(), Diagnostics.describeQuery(actual));
        }
        return this;
    }

    /** Whether this query's {@link CapturedQuery#tables()} contains {@code table} (case-insensitive). */
    public SingleCapturedQueryAssert hasTable(String table) {
        isNotNull();
        String lower = Diagnostics.lowercase(table);
        if (!actual.tables().contains(lower)) {
            failWithMessage(
                    "Expected query to access table '%s' but it accessed %s:%n%s",
                    table, actual.tables(), Diagnostics.describeQuery(actual));
        }
        return this;
    }

    public SingleCapturedQueryAssert hasParameterCount(int expected) {
        isNotNull();
        int actualCount = actual.parameters().size();
        if (actualCount != expected) {
            failWithMessage(
                    "Expected %d bind parameter(s) but found %d:%n%s",
                    expected, actualCount, Diagnostics.describeQuery(actual));
        }
        return this;
    }

    public SingleCapturedQueryAssert hasNoBindParameters() {
        return hasParameterCount(0);
    }
}
