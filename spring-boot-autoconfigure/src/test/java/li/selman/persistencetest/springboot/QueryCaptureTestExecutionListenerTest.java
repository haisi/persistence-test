package li.selman.persistencetest.springboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import javax.sql.DataSource;
import li.selman.persistencetest.capture.QueryCapture;
import li.selman.persistencetest.capture.QueryCaptureContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestContext;

class QueryCaptureTestExecutionListenerTest {

    @Test
    void beforeTestMethodResetsCapturedQueries() throws Exception {
        var h2 = new JdbcDataSource();
        h2.setUrl("jdbc:h2:mem:listener-test-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        DataSource dataSource = QueryCapture.wrap(h2);
        QueryCaptureContext.current().reset();

        try (Connection connection = dataSource.getConnection();
                var statement = connection.createStatement()) {
            statement.execute("select 1");
        }
        assertThat(QueryCaptureContext.current().capturedQueries()).hasSize(1);

        new QueryCaptureTestExecutionListener().beforeTestMethod(unusedTestContext());

        assertThat(QueryCaptureContext.current().capturedQueries()).isEmpty();
    }

    // The listener never reads its TestContext argument, so an inert proxy is enough - avoids either a
    // Mockito dependency or hand-implementing TestContext's full interface.
    private static TestContext unusedTestContext() {
        return (TestContext) Proxy.newProxyInstance(
                QueryCaptureTestExecutionListenerTest.class.getClassLoader(),
                new Class<?>[] {TestContext.class},
                (proxyInstance, method, args) -> {
                    throw new UnsupportedOperationException("not used by the listener under test");
                });
    }
}
