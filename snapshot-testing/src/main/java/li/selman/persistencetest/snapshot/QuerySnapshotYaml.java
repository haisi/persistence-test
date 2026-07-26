package li.selman.persistencetest.snapshot;

/**
 * Renders a {@link QuerySnapshot} as deterministic YAML text.
 *
 * <p>Hand-written rather than delegated to a general-purpose YAML library: the schema is small and fixed
 * (a list of four-field entries), and a general library's own formatting choices (key ordering, quoting,
 * scalar style) are one more source of non-determinism to control for no real benefit here. Multi-line SQL
 * uses YAML's block literal style ({@code |}), matching how a human would hand-write it.
 */
final class QuerySnapshotYaml {

    private QuerySnapshotYaml() {}

    static String render(QuerySnapshot snapshot) {
        if (snapshot.queries().isEmpty()) {
            return "queries: []";
        }
        StringBuilder yaml = new StringBuilder("queries:\n");
        for (QuerySnapshotEntry entry : snapshot.queries()) {
            yaml.append("  - type: ").append(entry.statementType()).append('\n');
            appendTables(yaml, entry.tables());
            appendNormalizedSql(yaml, entry.normalizedSql());
            yaml.append("    count: ").append(entry.count()).append('\n');
        }
        // No trailing newline: java-snapshot-testing strips it when persisting to the .snap file, so
        // keeping one here would make every freshly-rendered snapshot mismatch the file it's compared
        // against on the very next run.
        yaml.setLength(yaml.length() - 1);
        return yaml.toString();
    }

    private static void appendTables(StringBuilder yaml, java.util.List<String> tables) {
        if (tables.isEmpty()) {
            yaml.append("    tables: []\n");
            return;
        }
        yaml.append("    tables:\n");
        for (String table : tables) {
            yaml.append("      - ").append(table).append('\n');
        }
    }

    private static void appendNormalizedSql(StringBuilder yaml, String normalizedSql) {
        yaml.append("    normalizedSql: |\n");
        for (String line : normalizedSql.split("\n", -1)) {
            yaml.append("      ").append(line).append('\n');
        }
    }
}
