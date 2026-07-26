package li.selman.persistencetest.capture;

import javax.sql.DataSource;
import li.selman.persistencetest.core.normalize.JSqlParserSqlNormalizer;
import li.selman.persistencetest.core.normalize.SqlNormalizer;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

/** Entry point for wrapping a {@link DataSource} so every statement executed through it is captured. */
public final class QueryCapture {

    private QueryCapture() {}

    /**
     * Wraps {@code dataSource} using the default {@link JSqlParserSqlNormalizer}.
     *
     * <p>Captured queries are recorded on {@link QueryCaptureContext#current()} for the thread that
     * executes them.
     */
    public static DataSource wrap(DataSource dataSource) {
        return wrap(dataSource, new JSqlParserSqlNormalizer());
    }

    /** Like {@link #wrap(DataSource)}, but with a caller-supplied {@link SqlNormalizer}. */
    public static DataSource wrap(DataSource dataSource, SqlNormalizer normalizer) {
        return ProxyDataSourceBuilder.create(dataSource)
                .name("persistence-test")
                .listener(new QueryCaptureListener(normalizer))
                .build();
    }
}
