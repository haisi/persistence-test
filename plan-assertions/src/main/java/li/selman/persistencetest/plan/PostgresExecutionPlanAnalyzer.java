package li.selman.persistencetest.plan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import li.selman.persistencetest.core.BindParameter;
import li.selman.persistencetest.core.CapturedQuery;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;

/**
 * PostgreSQL {@link ExecutionPlanAnalyzer}, using {@code EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)}.
 *
 * <p><b>{@code ANALYZE} executes the statement</b> - for a data-modifying statement (an {@code INSERT},
 * {@code UPDATE}, or {@code DELETE}) that means the side effect actually happens, not just an estimate.
 * This method guarantees no such side effect survives: it runs the whole {@code EXPLAIN} inside a savepoint
 * on the given connection and always rolls back to it afterward, regardless of success or failure. The
 * connection's autocommit state is saved and restored around this, since setting a savepoint requires
 * autocommit to be off.
 */
public final class PostgresExecutionPlanAnalyzer implements ExecutionPlanAnalyzer {

    @Override
    public ExecutionPlan explain(Connection connection, CapturedQuery query) {
        boolean originalAutoCommit;
        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new ExecutionPlanException("Failed to prepare connection for EXPLAIN", e);
        }

        Savepoint savepoint;
        try {
            savepoint = connection.setSavepoint();
        } catch (SQLException e) {
            restoreAutoCommitQuietly(connection, originalAutoCommit);
            throw new ExecutionPlanException("Failed to prepare connection for EXPLAIN", e);
        }

        try {
            return parseExplainJson(fetchExplainJson(connection, query));
        } catch (SQLException e) {
            throw new ExecutionPlanException("Failed to EXPLAIN: " + query.sql(), e);
        } finally {
            rollbackToSavepointQuietly(connection, savepoint);
            restoreAutoCommitQuietly(connection, originalAutoCommit);
        }
    }

    private static String fetchExplainJson(Connection connection, CapturedQuery query) throws SQLException {
        String explainSql = "EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + query.sql();
        try (PreparedStatement statement = connection.prepareStatement(explainSql)) {
            bindParameters(statement, query.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("EXPLAIN returned no rows for: " + query.sql());
                }
                return resultSet.getString(1);
            }
        }
    }

    private static void bindParameters(PreparedStatement statement, List<BindParameter> parameters)
            throws SQLException {
        for (BindParameter parameter : parameters) {
            statement.setObject(parameter.position(), parameter.value());
        }
    }

    private static void rollbackToSavepointQuietly(Connection connection, @Nullable Savepoint savepoint) {
        if (savepoint == null) {
            return;
        }
        try {
            connection.rollback(savepoint);
        } catch (SQLException e) {
            // Best-effort: by this point we're already unwinding from either success or another failure,
            // and the caller owns the connection's lifecycle from here - there's nothing more to do with a
            // failed cleanup rollback than let it go.
        }
    }

    private static void restoreAutoCommitQuietly(Connection connection, boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            // Best-effort; see rollbackToSavepointQuietly.
        }
    }

    static ExecutionPlan parseExplainJson(String json) {
        JSONObject planWrapper = new JSONArray(json).getJSONObject(0);
        return new ExecutionPlan(parseNode(planWrapper.getJSONObject("Plan")));
    }

    private static PlanNode parseNode(JSONObject json) {
        List<PlanNode> children = new ArrayList<>();
        if (json.has("Plans")) {
            JSONArray childNodes = json.getJSONArray("Plans");
            for (int i = 0; i < childNodes.length(); i++) {
                children.add(parseNode(childNodes.getJSONObject(i)));
            }
        }
        return new PlanNode(
                json.getString("Node Type"),
                json.optString("Relation Name", null),
                json.optString("Index Name", null),
                json.optLong("Plan Rows", 0),
                json.optLong("Actual Rows", 0),
                children);
    }
}
