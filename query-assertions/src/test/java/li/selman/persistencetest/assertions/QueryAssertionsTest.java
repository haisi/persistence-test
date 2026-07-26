package li.selman.persistencetest.assertions;

import static li.selman.persistencetest.assertions.QueryAssertions.assertThatQueries;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import li.selman.persistencetest.capture.QueryCapture;
import li.selman.persistencetest.capture.QueryCaptureContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the no-arg {@code assertThatQueries()} overload against a real capture pipeline (H2 wrapped by
 * {@link QueryCapture}), rather than reaching into query-capture's package-private
 * {@code QueryCaptureContext.record()} to fabricate state.
 */
class QueryAssertionsTest {

    private DataSource capturingDataSource;

    @BeforeEach
    void setUp() throws SQLException {
        var h2 = new JdbcDataSource();
        h2.setUrl("jdbc:h2:mem:query-assertions-test-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        capturingDataSource = QueryCapture.wrap(h2);

        try (Connection connection = capturingDataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute("create table customer (id bigint primary key)");
        }
        QueryCaptureContext.current().reset();
    }

    @AfterEach
    void tearDown() {
        QueryCaptureContext.current().reset();
    }

    @Test
    void noArgAssertThatQueriesReadsFromTheAmbientCaptureContext() throws SQLException {
        try (Connection connection = capturingDataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute("select * from customer");
        }

        assertThatQueries().selects(1).containsTable("customer");
    }

    @Test
    void explicitListOverloadIgnoresTheAmbientCaptureContext() throws SQLException {
        try (Connection connection = capturingDataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute("select * from customer");
        }

        assertThat(QueryCaptureContext.current().capturedQueries()).hasSize(1);
        assertThatQueries(java.util.List.of()).hasTotalCount(0);
    }
}
