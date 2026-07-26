package li.selman.persistencetest.assertions;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import li.selman.persistencetest.analysis.QueryAnalyzer;
import li.selman.persistencetest.analysis.QueryStatistics;
import li.selman.persistencetest.analysis.RepeatedQueryShape;
import li.selman.persistencetest.core.BindParameter;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;

/**
 * Formats captured queries into the failure-message text every {@code QueriesAssert}/
 * {@code SingleCapturedQueryAssert} assertion uses - a single place responsible for making failures
 * immediately explain themselves, per the library's diagnostics goal: execution order, normalized SQL,
 * parameter samples, and summary statistics, not just "expected 2 but was 3".
 */
final class Diagnostics {

    private Diagnostics() {}

    static String describeQuery(CapturedQuery query) {
        String params = query.parameters().isEmpty() ? "" : " -- params: " + describeParameters(query.parameters());
        long millis = query.duration() == null ? 0 : query.duration().toMillis();
        return "#%d [%s] %s%s (%dms)"
                .formatted(query.sequence(), query.statementType(), query.normalizedSql(), params, millis);
    }

    static String describeParameters(List<BindParameter> parameters) {
        return parameters.stream()
                .map(parameter -> String.valueOf(parameter.value()))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    static String describeTimeline(List<CapturedQuery> queries) {
        if (queries.isEmpty()) {
            return "  (none)";
        }
        return QueryAnalyzer.timelineOf(queries).stream()
                .map(query -> "  " + describeQuery(query))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    static String describeStatistics(List<CapturedQuery> queries) {
        QueryStatistics stats = QueryAnalyzer.statisticsOf(queries);
        return "total=%d, select=%d, insert=%d, update=%d, delete=%d, ddl=%d, other=%d, tables=%s"
                .formatted(
                        stats.totalCount(),
                        stats.countOf(StatementType.SELECT),
                        stats.countOf(StatementType.INSERT),
                        stats.countOf(StatementType.UPDATE),
                        stats.countOf(StatementType.DELETE),
                        stats.countOf(StatementType.DDL),
                        stats.countOf(StatementType.OTHER),
                        stats.accessedTables());
    }

    static String describeRepeatedShapes(List<RepeatedQueryShape> shapes) {
        return shapes.stream()
                .map(shape -> "  executed %d times: %s%n%s"
                        .formatted(
                                shape.occurrenceCount(),
                                shape.normalizedSql(),
                                shape.occurrences().stream()
                                        .map(query -> "    params: " + describeParameters(query.parameters()))
                                        .collect(Collectors.joining(System.lineSeparator()))))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    static String withContext(String message, List<CapturedQuery> queries) {
        return "%s%n%nSummary: %s%n%nCaptured queries (%d total):%n%s"
                .formatted(message, describeStatistics(queries), queries.size(), describeTimeline(queries));
    }

    static String lowercase(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
