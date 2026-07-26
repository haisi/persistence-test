/**
 * Spring Boot 4 auto-configuration: wraps the application's {@code DataSource} bean(s) with
 * {@code QueryCapture} automatically, and registers a {@code TestExecutionListener} that resets
 * {@code QueryCaptureContext} before every test method - so {@code @SpringBootTest} works without any
 * manual {@code QueryCapture.wrap(...)} or {@code @ExtendWith(QueryCaptureExtension.class)} wiring.
 */
@NullMarked
package li.selman.persistencetest.springboot;

import org.jspecify.annotations.NullMarked;
