package li.selman.persistencetest.capture.junit;

import li.selman.persistencetest.capture.QueryCaptureContext;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Resets {@link QueryCaptureContext#current()} before each test and, optionally, injects it as a test
 * method parameter.
 *
 * <pre>{@code
 * @ExtendWith(QueryCaptureExtension.class)
 * class OrderRepositoryTest {
 *
 *     @Test
 *     void findsOrdersByCustomer(QueryCaptureContext queries) {
 *         orderRepository.findByCustomerId(customerId);
 *
 *         assertThat(queries.capturedQueries()).hasSize(1);
 *     }
 * }
 * }</pre>
 *
 * <p>Only resets state - it does not itself wrap any {@link javax.sql.DataSource}. Pair it with
 * {@link li.selman.persistencetest.capture.QueryCapture#wrap} wherever the {@code DataSource} bean is
 * created.
 */
public final class QueryCaptureExtension implements BeforeEachCallback, ParameterResolver {

    @Override
    public void beforeEach(ExtensionContext context) {
        QueryCaptureContext.current().reset();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == QueryCaptureContext.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return QueryCaptureContext.current();
    }
}
