package li.selman.persistencetest.hibernate;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import li.selman.persistencetest.capture.QueryCapture;
import li.selman.persistencetest.capture.QueryCaptureContext;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the no-arg {@code assertThatQueries(resolver)} overload against the same ambient
 * {@code QueryCaptureContext} query-assertions reads from, and that matching happens against the table
 * Hibernate actually resolves for the entity - not a re-derived guess.
 */
class HibernateAssertionsTest {

    private static final String DB_URL = "jdbc:h2:mem:hibernate-assertions-test;DB_CLOSE_DELAY=-1";

    private static EntityManagerFactory entityManagerFactory;
    private DataSource capturingDataSource;

    @BeforeAll
    static void setUpEntityManagerFactory() {
        entityManagerFactory = new Configuration()
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", DB_URL)
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .addAnnotatedClass(Customer.class)
                .buildSessionFactory();
    }

    @AfterAll
    static void tearDownEntityManagerFactory() {
        entityManagerFactory.close();
    }

    @BeforeEach
    void setUp() throws SQLException {
        var h2 = new JdbcDataSource();
        h2.setUrl(DB_URL);
        capturingDataSource = QueryCapture.wrap(h2);
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

        var resolver = new HibernateEntityTableResolver(entityManagerFactory);
        HibernateAssertions.assertThatQueries(resolver).containsSelect(Customer.class);
    }
}
