package li.selman.persistencetest.snapshot;

import com.google.errorprone.annotations.Var;
import java.util.List;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Built-in {@link QuerySnapshotTransformer}: masks non-deterministic literals (UUIDs, timestamps,
 * generated IDs) in {@link QuerySnapshotEntry#normalizedSql()} and can strip schema qualifiers from table
 * names, so a snapshot doesn't churn on every run just because a generated value changed.
 *
 * <pre>{@code
 * SnapshotNormalizer.builder()
 *     .replaceUuid("<uuid>")
 *     .replaceTimestamp("<timestamp>")
 *     .replaceGeneratedIds("<id>")
 *     .build();
 * }</pre>
 *
 * <p>Replacements are applied in a fixed order (UUIDs, then timestamps, then generated IDs) regardless of
 * the order {@code replace*} was called in on the builder, since {@code replaceGeneratedIds}'s bare-integer
 * match would otherwise consume digits out of an not-yet-masked timestamp.
 *
 * <p><b>{@code ignoreAliases()} and {@code ignoreComments()} are no-ops</b>, kept only so code written
 * against the example API above compiles: comments never survive {@code JSqlParserSqlNormalizer}'s
 * parse/re-print round trip in the first place, and alias canonicalization is the same tracked limitation
 * documented on {@code JSqlParserSqlNormalizer} - collapsing alias names correctly requires rewriting every
 * column reference that points at them, which isn't implemented yet.
 */
public final class SnapshotNormalizer implements QuerySnapshotTransformer {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");
    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?");
    private static final Pattern GENERATED_ID_PATTERN = Pattern.compile("\\b\\d+\\b");

    private final @Nullable String uuidReplacement;
    private final @Nullable String timestampReplacement;
    private final @Nullable String generatedIdReplacement;
    private final boolean stripSchemas;

    private SnapshotNormalizer(
            @Nullable String uuidReplacement,
            @Nullable String timestampReplacement,
            @Nullable String generatedIdReplacement,
            boolean stripSchemas) {
        this.uuidReplacement = uuidReplacement;
        this.timestampReplacement = timestampReplacement;
        this.generatedIdReplacement = generatedIdReplacement;
        this.stripSchemas = stripSchemas;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public QuerySnapshot transform(QuerySnapshot snapshot) {
        return new QuerySnapshot(
                snapshot.queries().stream().map(this::transformEntry).toList());
    }

    private QuerySnapshotEntry transformEntry(QuerySnapshotEntry entry) {
        @Var String sql = entry.normalizedSql();
        if (uuidReplacement != null) {
            sql = UUID_PATTERN.matcher(sql).replaceAll(uuidReplacement);
        }
        if (timestampReplacement != null) {
            sql = TIMESTAMP_PATTERN.matcher(sql).replaceAll(timestampReplacement);
        }
        if (generatedIdReplacement != null) {
            sql = GENERATED_ID_PATTERN.matcher(sql).replaceAll(generatedIdReplacement);
        }
        List<String> tables = stripSchemas
                ? entry.tables().stream().map(SnapshotNormalizer::stripSchema).toList()
                : entry.tables();
        return new QuerySnapshotEntry(entry.statementType(), tables, sql, entry.count());
    }

    private static String stripSchema(String table) {
        int lastDot = table.lastIndexOf('.');
        return lastDot < 0 ? table : table.substring(lastDot + 1);
    }

    /** Builds a {@link SnapshotNormalizer}. */
    public static final class Builder {

        private @Nullable String uuidReplacement;
        private @Nullable String timestampReplacement;
        private @Nullable String generatedIdReplacement;
        private boolean stripSchemas;

        private Builder() {}

        /** Replaces UUID-shaped literals (e.g. {@code 8-4-4-4-12} hex groups) in the SQL text. */
        public Builder replaceUuid(String replacement) {
            this.uuidReplacement = replacement;
            return this;
        }

        /** Replaces ISO-8601-ish timestamp literals (e.g. {@code 2026-07-26 12:34:56.789}) in the SQL text. */
        public Builder replaceTimestamp(String replacement) {
            this.timestampReplacement = replacement;
            return this;
        }

        /**
         * Replaces every bare integer literal in the SQL text. Deliberately broad: distinguishing a
         * generated primary key literal from any other integer literal (e.g. a {@code LIMIT} value) would
         * require deeper semantic analysis than a text-level transformer can do.
         */
        public Builder replaceGeneratedIds(String replacement) {
            this.generatedIdReplacement = replacement;
            return this;
        }

        /** No-op; see the class Javadoc. */
        public Builder ignoreAliases() {
            return this;
        }

        /** No-op; see the class Javadoc. */
        public Builder ignoreComments() {
            return this;
        }

        /** Strips a {@code schema.} prefix from every table name in {@link QuerySnapshotEntry#tables()}. */
        public Builder ignoreSchemas() {
            this.stripSchemas = true;
            return this;
        }

        public SnapshotNormalizer build() {
            return new SnapshotNormalizer(uuidReplacement, timestampReplacement, generatedIdReplacement, stripSchemas);
        }
    }
}
