package li.selman.persistencetest.springboot;

import li.selman.persistencetest.capture.QueryCaptureContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * Resets {@link QueryCaptureContext#current()} before each test method.
 *
 * <p>Registered via {@code META-INF/spring.factories}, which the Spring TestContext Framework picks up as
 * a <em>default</em> listener for every test using it (e.g. anything with
 * {@code @ExtendWith(SpringExtension.class)} or {@code @SpringBootTest}) - no {@code @TestExecutionListeners}
 * declaration needed on the test class itself, unless that class already uses
 * {@code mergeMode = MergeMode.REPLACE_DEFAULTS}.
 */
public final class QueryCaptureTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        QueryCaptureContext.current().reset();
    }
}
