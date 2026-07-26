package li.selman.persistencetest.springboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import li.selman.persistencetest.capture.QueryCaptureContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Uses {@link ApplicationContextRunner} rather than a full {@code @SpringBootTest} - the standard,
 * Docker-free way Spring Boot's own autoconfigure modules test themselves.
 */
class PersistenceTestAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersistenceTestAutoConfiguration.class))
            .withUserConfiguration(TestDataSourceConfiguration.class);

    @Test
    void wrapsTheDataSourceBeanSoQueriesThroughItAreCaptured() {
        contextRunner.run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            QueryCaptureContext.current().reset();

            try (Connection connection = dataSource.getConnection();
                    var statement = connection.createStatement()) {
                statement.execute("select 1");
            }

            assertThat(QueryCaptureContext.current().capturedQueries()).hasSize(1);
        });
    }

    @Test
    void canBeDisabledViaProperty() {
        contextRunner.withPropertyValues("persistence-test.enabled=false").run(context -> {
            DataSource dataSource = context.getBean(DataSource.class);
            QueryCaptureContext.current().reset();

            try (Connection connection = dataSource.getConnection();
                    var statement = connection.createStatement()) {
                statement.execute("select 1");
            }

            assertThat(QueryCaptureContext.current().capturedQueries()).isEmpty();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestDataSourceConfiguration {

        @Bean
        DataSource dataSource() {
            var h2 = new JdbcDataSource();
            h2.setUrl("jdbc:h2:mem:autoconfigure-test-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
            return h2;
        }
    }
}
