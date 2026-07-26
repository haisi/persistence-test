package li.selman.persistencetest.snapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import li.selman.persistencetest.capture.QueryCaptureContext;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;

/** Builds {@link QuerySnapshot} instances from captured queries. */
public final class QuerySnapshots {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private QuerySnapshots() {}

    /** {@link #of(List, SnapshotLevel)} at the recommended default level, {@link SnapshotLevel#SEMANTIC}. */
    public static QuerySnapshot of(List<CapturedQuery> queries) {
        return of(queries, SnapshotLevel.SEMANTIC);
    }

    /** Builds a snapshot from {@code queries} at the given {@link SnapshotLevel}. */
    public static QuerySnapshot of(List<CapturedQuery> queries, SnapshotLevel level) {
        Map<GroupKey, List<CapturedQuery>> grouped = new LinkedHashMap<>();
        for (CapturedQuery query : queries) {
            String sql = level == SnapshotLevel.SQL ? collapseWhitespace(query.sql()) : query.normalizedSql();
            grouped.computeIfAbsent(new GroupKey(query.statementType(), query.tables(), sql), key -> new ArrayList<>())
                    .add(query);
        }
        List<QuerySnapshotEntry> entries = grouped.entrySet().stream()
                .map(entry -> new QuerySnapshotEntry(
                        entry.getKey().statementType(),
                        entry.getKey().tables(),
                        entry.getKey().sql(),
                        entry.getValue().size()))
                .toList();
        return new QuerySnapshot(entries);
    }

    /** {@link #of(List)} over {@code QueryCaptureContext.current().capturedQueries()}. */
    public static QuerySnapshot current() {
        return current(SnapshotLevel.SEMANTIC);
    }

    /** {@link #of(List, SnapshotLevel)} over {@code QueryCaptureContext.current().capturedQueries()}. */
    public static QuerySnapshot current(SnapshotLevel level) {
        return of(QueryCaptureContext.current().capturedQueries(), level);
    }

    private static String collapseWhitespace(String sql) {
        return WHITESPACE.matcher(sql.strip()).replaceAll(" ");
    }

    private record GroupKey(StatementType statementType, List<String> tables, String sql) {}
}
