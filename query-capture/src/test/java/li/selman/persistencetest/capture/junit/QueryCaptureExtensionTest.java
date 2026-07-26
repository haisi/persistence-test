package li.selman.persistencetest.capture.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import li.selman.persistencetest.capture.QueryCapture;
import li.selman.persistencetest.capture.QueryCaptureContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(QueryCaptureExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QueryCaptureExtensionTest {

    private static final DataSource DATA_SOURCE;

    static {
        var h2 = new JdbcDataSource();
        h2.setUrl("jdbc:h2:mem:query-capture-extension-test;DB_CLOSE_DELAY=-1");
        DATA_SOURCE = QueryCapture.wrap(h2);
    }

    @Test
    @Order(1)
    void firstTestRecordsAQuery(QueryCaptureContext context) throws SQLException {
        assertThat(context.capturedQueries()).isEmpty();

        try (Connection connection = DATA_SOURCE.getConnection();
                var statement = connection.createStatement()) {
            statement.execute("select 1");
        }

        assertThat(context.capturedQueries()).hasSize(1);
    }

    @Test
    @Order(2)
    void secondTestStartsWithAResetContext(QueryCaptureContext context) {
        assertThat(context.capturedQueries()).isEmpty();
    }
}
