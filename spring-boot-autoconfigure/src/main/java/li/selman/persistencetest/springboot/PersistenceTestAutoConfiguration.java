package li.selman.persistencetest.springboot;

import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that wraps every {@link DataSource} bean with query capture. Registered via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 *
 * <p>Disable with {@code persistence-test.enabled=false} (e.g. in a profile that shouldn't pay the capture
 * overhead).
 */
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(prefix = "persistence-test", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PersistenceTestAutoConfiguration {

    @Bean
    // Deliberately static and dependency-free: BeanPostProcessor @Bean methods must not reference other
    // beans as parameters, or Spring will instantiate this configuration class (and whatever it depends
    // on) too early in the container lifecycle, before post-processors are meant to be registered.
    static BeanPostProcessor queryCaptureBeanPostProcessor() {
        return new QueryCaptureBeanPostProcessor();
    }
}
