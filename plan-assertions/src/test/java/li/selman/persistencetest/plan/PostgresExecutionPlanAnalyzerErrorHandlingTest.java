package li.selman.persistencetest.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link PostgresExecutionPlanAnalyzer}'s connection-failure handling - paths that are impractical
 * to trigger against a real database (a broken connection, a failed savepoint) but real enough to happen
 * against a pooled/flaky connection in production, using hand-rolled JDBC proxies rather than a mocking
 * library this project doesn't otherwise depend on.
 */
class PostgresExecutionPlanAnalyzerErrorHandlingTest {

    private static final CapturedQuery QUERY = query();

    @Test
    void wrapsFailureToReadAutoCommitState() {
        Connection connection = connectionThrowingOn("getAutoCommit");

        assertThatThrownBy(() -> new PostgresExecutionPlanAnalyzer().explain(connection, QUERY))
                .isInstanceOf(ExecutionPlanException.class)
                .hasMessageContaining("Failed to prepare connection for EXPLAIN");
    }

    @Test
    void wrapsFailureToCreateASavepointAndStillRestoresAutoCommit() {
        var autoCommitRestored = new boolean[1];
        Connection connection = proxy(Connection.class, (proxyInstance, method, args) -> switch (method.getName()) {
            case "getAutoCommit" -> true;
            case "setAutoCommit" -> {
                if (Boolean.TRUE.equals(args[0])) {
                    autoCommitRestored[0] = true;
                }
                yield null;
            }
            case "setSavepoint" -> throw new SQLException("cannot create savepoint");
            default -> defaultValueFor(method.getReturnType());
        });

        assertThatThrownBy(() -> new PostgresExecutionPlanAnalyzer().explain(connection, QUERY))
                .isInstanceOf(ExecutionPlanException.class)
                .hasMessageContaining("Failed to prepare connection for EXPLAIN");
        assertThat(autoCommitRestored[0]).isTrue();
    }

    @Test
    void wrapsFailureToPrepareOrExecuteTheExplainStatement() {
        Connection connection = workingConnection(preparedStatementThrowingOnExecute());

        assertThatThrownBy(() -> new PostgresExecutionPlanAnalyzer().explain(connection, QUERY))
                .isInstanceOf(ExecutionPlanException.class)
                .hasMessageContaining("Failed to EXPLAIN")
                .hasMessageContaining(QUERY.sql());
    }

    @Test
    void wrapsAnExplainThatReturnsNoRows() {
        ResultSet emptyResultSet = proxy(ResultSet.class, (proxyInstance, method, args) -> {
            if ("next".equals(method.getName())) {
                return false;
            }
            return defaultValueFor(method.getReturnType());
        });
        PreparedStatement statement = proxy(PreparedStatement.class, (proxyInstance, method, args) -> {
            if ("executeQuery".equals(method.getName())) {
                return emptyResultSet;
            }
            return defaultValueFor(method.getReturnType());
        });

        Connection connection = workingConnection(statement);

        assertThatThrownBy(() -> new PostgresExecutionPlanAnalyzer().explain(connection, QUERY))
                .isInstanceOf(ExecutionPlanException.class)
                .hasMessageContaining("Failed to EXPLAIN");
    }

    @Test
    void aFailedCleanupRollbackDoesNotPreventReturningTheParsedPlan() {
        Connection connection =
                workingConnection(preparedStatementReturning(leafPlanJson()), (proxyInstance, method, args) -> {
                    if ("rollback".equals(method.getName()) && args != null && args.length == 1) {
                        throw new SQLException("rollback failed");
                    }
                    return null;
                });

        ExecutionPlan plan = new PostgresExecutionPlanAnalyzer().explain(connection, QUERY);

        assertThat(plan.root().nodeType()).isEqualTo("Seq Scan");
    }

    @Test
    void aFailedAutoCommitRestoreDoesNotPreventReturningTheParsedPlan() {
        Connection connection =
                workingConnection(preparedStatementReturning(leafPlanJson()), (proxyInstance, method, args) -> {
                    if ("setAutoCommit".equals(method.getName()) && Boolean.TRUE.equals(args[0])) {
                        throw new SQLException("cannot restore autocommit");
                    }
                    return null;
                });

        ExecutionPlan plan = new PostgresExecutionPlanAnalyzer().explain(connection, QUERY);

        assertThat(plan.root().nodeType()).isEqualTo("Seq Scan");
    }

    private static String leafPlanJson() {
        return """
                [{"Plan": {"Node Type": "Seq Scan", "Relation Name": "customer", "Plan Rows": 1, "Actual Rows": 1}}]
                """;
    }

    private static PreparedStatement preparedStatementReturning(String explainJson) {
        ResultSet resultSet = proxy(ResultSet.class, (proxyInstance, method, args) -> switch (method.getName()) {
            case "next" -> true;
            case "getString" -> explainJson;
            default -> defaultValueFor(method.getReturnType());
        });
        return proxy(PreparedStatement.class, (proxyInstance, method, args) -> {
            if ("executeQuery".equals(method.getName())) {
                return resultSet;
            }
            return defaultValueFor(method.getReturnType());
        });
    }

    private static PreparedStatement preparedStatementThrowingOnExecute() {
        return proxy(PreparedStatement.class, (proxyInstance, method, args) -> {
            if ("executeQuery".equals(method.getName())) {
                throw new SQLException("boom");
            }
            return defaultValueFor(method.getReturnType());
        });
    }

    /** A {@link Connection} whose {@code getAutoCommit}/{@code setSavepoint} succeed uneventfully. */
    private static Connection workingConnection(PreparedStatement statement) {
        return workingConnection(statement, (proxyInstance, method, args) -> null);
    }

    private static Connection workingConnection(PreparedStatement statement, InvocationHandler cleanupHandler) {
        Savepoint savepoint =
                proxy(Savepoint.class, (proxyInstance, method, args) -> defaultValueFor(method.getReturnType()));
        return proxy(Connection.class, (proxyInstance, method, args) -> switch (method.getName()) {
            case "getAutoCommit" -> true;
            case "setSavepoint" -> savepoint;
            case "prepareStatement" -> statement;
            case "rollback", "setAutoCommit" -> cleanupHandler.invoke(proxyInstance, method, args);
            default -> defaultValueFor(method.getReturnType());
        });
    }

    private static Connection connectionThrowingOn(String methodName) {
        return proxy(Connection.class, (proxyInstance, method, args) -> {
            if (method.getName().equals(methodName)) {
                throw new SQLException("boom");
            }
            return defaultValueFor(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                PostgresExecutionPlanAnalyzerErrorHandlingTest.class.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static @Nullable Object defaultValueFor(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }

    // affectedRows/exception are @Nullable on CapturedQuery; see the note in query-capture's
    // QueryCaptureListener for why NullAway doesn't see that cross-module.
    @SuppressWarnings({"NullAway", "NullArgumentForNonNullParameter"})
    private static CapturedQuery query() {
        return new CapturedQuery(
                0,
                Instant.EPOCH,
                "select * from customer",
                "select * from customer",
                StatementType.SELECT,
                List.of("customer"),
                List.of(),
                Duration.ofMillis(1),
                null,
                null,
                "main",
                "conn-1");
    }
}
