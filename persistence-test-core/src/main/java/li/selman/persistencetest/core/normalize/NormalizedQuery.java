package li.selman.persistencetest.core.normalize;

import java.util.List;
import li.selman.persistencetest.core.StatementType;

/**
 * The semantic shape of a SQL statement, independent of formatting, comments, casing, or identifier
 * quoting style.
 *
 * @param statementType the kind of statement.
 * @param tables tables (and views) referenced by the statement, lower-cased and sorted for a deterministic
 *     representation. Schema-qualified names keep their schema prefix.
 * @param normalizedSql a deterministic re-rendering of the statement: comments and incidental whitespace
 *     are gone and keyword casing is canonical, but table/column identifiers and alias names are preserved
 *     as written. See {@link JSqlParserSqlNormalizer} for why alias identity isn't collapsed yet.
 */
public record NormalizedQuery(StatementType statementType, List<String> tables, String normalizedSql) {

    @SuppressWarnings("Var") // reassigning a compact constructor's implicit parameter for a defensive copy
    // is the standard record idiom; it can't be annotated @Var since the underlying field is final.
    public NormalizedQuery {
        tables = List.copyOf(tables);
    }
}
