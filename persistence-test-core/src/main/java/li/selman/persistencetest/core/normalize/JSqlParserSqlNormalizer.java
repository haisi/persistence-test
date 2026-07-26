package li.selman.persistencetest.core.normalize;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import li.selman.persistencetest.core.StatementType;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.sequence.CreateSequence;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;

/**
 * Default {@link SqlNormalizer}, backed by parsing the statement into a real AST with
 * <a href="https://github.com/JSqlParser/JSqlParser">JSqlParser</a> and re-rendering it, rather than
 * pattern-matching the raw SQL text.
 *
 * <p>Re-rendering the parsed AST (instead of regex-cleaning the original string) is what makes the
 * following stable by construction, since none of it survives the parse/re-print round trip:
 *
 * <ul>
 *   <li>whitespace and formatting
 *   <li>SQL comments (dropped by the tokenizer; never part of the AST)
 *   <li>keyword casing (the deparser always emits its own canonical casing)
 * </ul>
 *
 * Identifier quoting (e.g. {@code "customer"} vs {@code customer} vs {@code `customer`}) is handled with a
 * targeted post-process: it is not semantically meaningful in any of the supported dialects, but JSqlParser
 * preserves the original quote character in the AST, so it would otherwise leak into the re-rendered SQL.
 *
 * <p><b>Known limitation:</b> alias names (e.g. {@code c} in {@code customer c}) are preserved as written,
 * not canonicalized. Two queries that are identical except for alias spelling will normalize to different
 * {@link NormalizedQuery#normalizedSql()} today. Canonicalizing aliases correctly requires rewriting every
 * column reference that points at them (including through subqueries and CTEs), which is tracked as
 * follow-up work rather than implemented here; getting it subtly wrong (e.g. colliding two distinct tables
 * in a self-join) would be worse than not doing it. {@link NormalizedQuery#tables()} is unaffected by this
 * limitation, since it is derived independently.
 */
public final class JSqlParserSqlNormalizer implements SqlNormalizer {

    private static final Pattern DOUBLE_QUOTED_IDENTIFIER = Pattern.compile("\"([A-Za-z_][A-Za-z0-9_]*)\"");
    private static final Pattern BACKTICK_QUOTED_IDENTIFIER = Pattern.compile("`([A-Za-z_][A-Za-z0-9_]*)`");
    private static final Pattern BRACKET_QUOTED_IDENTIFIER = Pattern.compile("\\[([A-Za-z_][A-Za-z0-9_]*)]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Override
    public NormalizedQuery normalize(String sql) {
        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            throw new SqlNormalizationException(sql, e);
        }

        StatementType statementType = statementTypeOf(statement);
        List<String> tables = tablesOf(statement);
        String normalizedSql = stripIdentifierQuoting(statement.toString());

        return new NormalizedQuery(statementType, tables, normalizedSql);
    }

    private static StatementType statementTypeOf(Statement statement) {
        if (statement instanceof Select) {
            return StatementType.SELECT;
        }
        if (statement instanceof Insert) {
            return StatementType.INSERT;
        }
        if (statement instanceof Update) {
            return StatementType.UPDATE;
        }
        if (statement instanceof Delete) {
            return StatementType.DELETE;
        }
        if (statement instanceof CreateTable
                || statement instanceof CreateView
                || statement instanceof CreateIndex
                || statement instanceof CreateSequence
                || statement instanceof Alter
                || statement instanceof Drop
                || statement instanceof Truncate) {
            return StatementType.DDL;
        }
        return StatementType.OTHER;
    }

    private static List<String> tablesOf(Statement statement) {
        TablesNamesFinder<Object> finder = new TablesNamesFinder<>();
        Set<String> rawTableNames = finder.getTables(statement);
        return rawTableNames.stream()
                .map(String::toLowerCase)
                .distinct()
                .sorted()
                .toList();
    }

    private static String stripIdentifierQuoting(String sql) {
        String withoutDoubleQuotes = DOUBLE_QUOTED_IDENTIFIER.matcher(sql).replaceAll("$1");
        String withoutBackticks =
                BACKTICK_QUOTED_IDENTIFIER.matcher(withoutDoubleQuotes).replaceAll("$1");
        String withoutBrackets =
                BRACKET_QUOTED_IDENTIFIER.matcher(withoutBackticks).replaceAll("$1");
        return WHITESPACE.matcher(withoutBrackets).replaceAll(" ").trim();
    }
}
