package li.selman.persistencetest.plan;

import static li.selman.persistencetest.plan.PlanAssertions.assertThatPlanOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import li.selman.persistencetest.core.BindParameter;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises {@link PostgresExecutionPlanAnalyzer} against a real PostgreSQL instance - in particular, that
 * {@code EXPLAIN ANALYZE}'s side effect on a data-modifying statement never survives, which is the whole
 * reason this analyzer wraps the call in a savepoint it always rolls back to.
 */
@Testcontainers
class PostgresExecutionPlanAnalyzerIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final AtomicLong SEQUENCE = new AtomicLong();

    private static Connection connection;

    @BeforeAll
    static void setUp() throws SQLException {
        connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        try (var statement = connection.createStatement()) {
            statement.execute("create table customer (id bigint primary key, email text, name text)");
            statement.execute("create index idx_customer_email on customer (email)");
            // Enough rows that the planner genuinely prefers an index scan for a selective lookup, rather
            // than a real Postgres just picking Seq Scan anyway because the whole table fits in one page.
            statement.execute("""
                    insert into customer (id, email, name)
                    select i, 'user' || i || '@example.com', 'User ' || i
                    from generate_series(1, 5000) as i
                    """);
            statement.execute("analyze customer");
        }
    }

    @AfterAll
    static void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void usesIndexForASelectiveEqualityLookup() {
        CapturedQuery query = query(
                StatementType.SELECT,
                "select * from customer where email = ?",
                List.of(BindParameter.of(1, "user2500@example.com")));

        assertThatPlanOf(connection, query)
                .usesIndex()
                .usesAnyIndexOn("customer")
                .usesIndex("idx_customer_email");
    }

    @Test
    void fullTableScanDoesNotAvoidSequentialScan() {
        CapturedQuery query = query(StatementType.SELECT, "select * from customer", List.of());

        assertThatThrownBy(() -> assertThatPlanOf(connection, query).avoidsSequentialScan())
                .hasMessageContaining("Expected the plan to avoid a sequential scan");
    }

    @Test
    void explainAnalyzeDoesNotPersistSideEffectsOfADataModifyingStatement() throws SQLException {
        long countBefore = countCustomers();

        CapturedQuery deleteQuery = query(StatementType.DELETE, "delete from customer where id = 1", List.of());
        new PostgresExecutionPlanAnalyzer().explain(connection, deleteQuery);

        assertThat(countCustomers()).isEqualTo(countBefore);
    }

    @Test
    void connectionAutoCommitStateIsRestoredAfterExplain() throws SQLException {
        connection.setAutoCommit(true);

        assertThatPlanOf(connection, query(StatementType.SELECT, "select * from customer where id = 1", List.of()));

        assertThat(connection.getAutoCommit()).isTrue();
    }

    private long countCustomers() throws SQLException {
        try (var statement = connection.createStatement();
                var resultSet = statement.executeQuery("select count(*) from customer")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    // affectedRows/exception are @Nullable on CapturedQuery; see the note in query-capture's
    // QueryCaptureListener for why NullAway doesn't see that cross-module.
    @SuppressWarnings({"NullAway", "NullArgumentForNonNullParameter"})
    private static CapturedQuery query(StatementType type, String sql, List<BindParameter> parameters) {
        return new CapturedQuery(
                SEQUENCE.getAndIncrement(),
                Instant.now(),
                sql,
                sql,
                type,
                List.of("customer"),
                parameters,
                Duration.ofMillis(1),
                null,
                null,
                "main",
                "conn-1");
    }
}
