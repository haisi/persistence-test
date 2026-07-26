package li.selman.persistencetest.springboot;

import javax.sql.DataSource;
import li.selman.persistencetest.capture.QueryCapture;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Wraps every {@link DataSource} bean in the context with {@link QueryCapture#wrap(DataSource)} after it's
 * fully initialized, substituting the wrapped instance for all subsequent injection - so beans that
 * {@code @Autowired DataSource} never need to know capture is happening.
 *
 * <p>If the application defines more than one {@code DataSource} bean, every one of them is wrapped.
 */
final class QueryCaptureBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource) {
            return QueryCapture.wrap(dataSource);
        }
        return bean;
    }
}
