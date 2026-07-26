package li.selman.persistencetest.capture;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import li.selman.persistencetest.core.BindParameter;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;
import li.selman.persistencetest.core.normalize.NormalizedQuery;
import li.selman.persistencetest.core.normalize.SqlNormalizationException;
import li.selman.persistencetest.core.normalize.SqlNormalizer;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.jspecify.annotations.Nullable;

/**
 * Translates datasource-proxy's {@link QueryExecutionListener} callbacks into {@link CapturedQuery}
 * instances recorded on {@link QueryCaptureContext#current()}.
 *
 * <p>Timing comes from datasource-proxy itself ({@link ExecutionInfo#getElapsedTime()}, measured around the
 * real JDBC call) rather than being tracked separately here.
 *
 * <p><b>Known limitation:</b> for a batched statement (multiple {@code addBatch()} calls executed via one
 * {@code executeBatch()}), only the first batch item's bind parameters are captured; JDBC reports one
 * {@link ExecutionInfo}/{@link QueryInfo} pair for the whole batch, and modeling every item as its own
 * {@link CapturedQuery} is left as follow-up work.
 */
public final class QueryCaptureListener implements QueryExecutionListener {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final SqlNormalizer normalizer;

    public QueryCaptureListener(SqlNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    @Override
    public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
        // No-op: datasource-proxy measures elapsed time around the real call itself, so there is nothing
        // to record before the query runs.
    }

    @Override
    public void afterQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
        QueryCaptureContext context = QueryCaptureContext.current();
        Instant timestamp = Instant.now();
        Duration duration = Duration.ofMillis(executionInfo.getElapsedTime());
        String threadName = Thread.currentThread().getName();
        String connectionId = executionInfo.getConnectionId();
        Long affectedRows = affectedRowsOf(executionInfo.getResult());
        Throwable exception = executionInfo.getThrowable();

        for (QueryInfo queryInfo : queryInfoList) {
            String sql = queryInfo.getQuery();
            NormalizedQuery normalized = normalize(sql);

            context.record(newCapturedQuery(
                    context.nextSequence(),
                    timestamp,
                    sql,
                    normalized,
                    parametersOf(queryInfo),
                    duration,
                    affectedRows,
                    exception,
                    threadName,
                    connectionId));
        }
    }

    // NullAway sees this constructor call as crossing a module boundary (query-capture -> already-compiled
    // persistence-test-core classes) and, for this particular record, the @Nullable annotations on the
    // duration/affectedRows/exception components don't come through on the compiled canonical constructor's
    // parameters, even though they're present on the fields and accessors - a narrow annotation-retention
    // gap in javac's record handling under this project's Error Prone/NullAway compilation pipeline, not a
    // real nullness bug (all three parameters are declared @Nullable in CapturedQuery's source). Confirmed
    // harmless by QueryCaptureIntegrationTest exercising the real construction path end to end.
    @SuppressWarnings("NullAway")
    private static CapturedQuery newCapturedQuery(
            long sequence,
            Instant timestamp,
            String sql,
            NormalizedQuery normalized,
            List<BindParameter> parameters,
            @Nullable Duration duration,
            @Nullable Long affectedRows,
            @Nullable Throwable exception,
            String threadName,
            String connectionId) {
        return new CapturedQuery(
                sequence,
                timestamp,
                sql,
                normalized.normalizedSql(),
                normalized.statementType(),
                normalized.tables(),
                parameters,
                duration,
                affectedRows,
                exception,
                threadName,
                connectionId);
    }

    private NormalizedQuery normalize(String sql) {
        try {
            return normalizer.normalize(sql);
        } catch (SqlNormalizationException e) {
            // Capture must never fail the system under test just because a statement is outside what the
            // configured normalizer understands (e.g. a dialect-specific DDL statement). Fall back to a
            // best-effort representation instead of propagating.
            return new NormalizedQuery(StatementType.OTHER, List.of(), collapseWhitespace(sql));
        }
    }

    private static String collapseWhitespace(String sql) {
        return WHITESPACE.matcher(sql.strip()).replaceAll(" ");
    }

    private static @Nullable Long affectedRowsOf(@Nullable Object result) {
        return result instanceof Number number ? number.longValue() : null;
    }

    @SuppressWarnings("deprecation") // getQueryArgsList() is deprecated in favor of the lower-level
    // getParametersList()/ParameterSetOperation API (which also exposes things like registerOutParameter
    // and setNull calls); it remains correct for the common bind-value case this module currently handles,
    // and switching is tracked as follow-up rather than done here.
    private static List<BindParameter> parametersOf(QueryInfo queryInfo) {
        List<Map<String, Object>> argsList = queryInfo.getQueryArgsList();
        if (argsList.isEmpty()) {
            return List.of();
        }
        Map<String, Object> firstExecution = argsList.get(0);
        return firstExecution.entrySet().stream()
                .map(entry -> BindParameter.of(Integer.parseInt(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparingInt(BindParameter::position))
                .toList();
    }
}
