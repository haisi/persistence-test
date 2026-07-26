package li.selman.persistencetest.capture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link QueryCapture} against a real H2 database through the real datasource-proxy interception
 * path, rather than mocking {@link net.ttddyy.dsproxy.listener.QueryExecutionListener} callbacks - in
 * particular, this is what confirms bind parameter positions actually come back 1-based as JDBC callers
 * expect, since that mapping is owned by datasource-proxy, not by this module.
 */
class QueryCaptureIntegrationTest {

    private DataSource capturingDataSource;

    @BeforeEach
    void setUp() throws SQLException {
        var h2 = new JdbcDataSource();
        h2.setUrl("jdbc:h2:mem:query-capture-test-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        capturingDataSource = QueryCapture.wrap(h2);

        try (Connection connection = capturingDataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute("create table customer (id bigint primary key, name varchar(100))");
        }
        QueryCaptureContext.current().reset();
    }

    @AfterEach
    void tearDown() {
        QueryCaptureContext.current().reset();
    }

    @Test
    void capturesSelectWithBindParametersInPositionOrder() throws SQLException {
        try (Connection connection = capturingDataSource.getConnection();
                PreparedStatement insert =
                        connection.prepareStatement("insert into customer (id, name) values (?, ?)")) {
            insert.setLong(1, 1L);
            insert.setString(2, "Ada Lovelace");
            insert.executeUpdate();
        }

        try (Connection connection = capturingDataSource.getConnection();
                PreparedStatement select = connection.prepareStatement("select name from customer where id = ?")) {
            select.setLong(1, 1L);
            select.executeQuery().close();
        }

        var queries = QueryCaptureContext.current().capturedQueries();
        assertThat(queries).hasSize(2);

        CapturedQuery insertQuery = queries.get(0);
        assertThat(insertQuery.statementType()).isEqualTo(StatementType.INSERT);
        assertThat(insertQuery.tables()).containsExactly("customer");
        assertThat(insertQuery.parameters()).hasSize(2);
        assertThat(insertQuery.parameters().get(0).position()).isEqualTo(1);
        assertThat(insertQuery.parameters().get(0).value()).isEqualTo(1L);
        assertThat(insertQuery.parameters().get(1).position()).isEqualTo(2);
        assertThat(insertQuery.parameters().get(1).value()).isEqualTo("Ada Lovelace");
        assertThat(insertQuery.affectedRows()).isEqualTo(1L);
        assertThat(insertQuery.isFailure()).isFalse();

        CapturedQuery selectQuery = queries.get(1);
        assertThat(selectQuery.statementType()).isEqualTo(StatementType.SELECT);
        assertThat(selectQuery.tables()).containsExactly("customer");
        assertThat(selectQuery.normalizedSql()).contains("customer").contains("id");
        assertThat(selectQuery.parameters()).hasSize(1);
        assertThat(selectQuery.parameters().get(0).value()).isEqualTo(1L);
    }

    @Test
    void assignsIncreasingSequenceAcrossStatements() throws SQLException {
        try (Connection connection = capturingDataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute("select * from customer");
            statement.execute("select * from customer");
        }

        var queries = QueryCaptureContext.current().capturedQueries();
        assertThat(queries).extracting(CapturedQuery::sequence).containsExactly(0L, 1L);
    }

    @Test
    void capturesExceptionOnFailingStatement() {
        assertThatThrownBy(() -> {
                    try (Connection connection = capturingDataSource.getConnection();
                            var statement = connection.createStatement()) {
                        statement.execute("select * from no_such_table");
                    }
                })
                .isInstanceOf(SQLException.class);

        var queries = QueryCaptureContext.current().capturedQueries();
        assertThat(queries).hasSize(1);
        assertThat(queries.get(0).isFailure()).isTrue();
        assertThat(queries.get(0).exception()).isInstanceOf(SQLException.class);
    }
}
