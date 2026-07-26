package li.selman.persistencetest.hibernate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HibernateEntityTableResolverTest {

    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    static void setUpEntityManagerFactory() {
        entityManagerFactory = new Configuration()
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty(
                        "hibernate.connection.url", "jdbc:h2:mem:hibernate-support-resolver-test;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .addAnnotatedClass(Customer.class)
                .addAnnotatedClass(OrderItem.class)
                .buildSessionFactory();
    }

    @AfterAll
    static void tearDownEntityManagerFactory() {
        entityManagerFactory.close();
    }

    @Test
    void resolvesTheMappedTableNameOfEachEntity() {
        var resolver = new HibernateEntityTableResolver(entityManagerFactory);

        assertThat(resolver.tableNameOf(Customer.class)).isEqualTo("customer");
        assertThat(resolver.tableNameOf(OrderItem.class)).isEqualTo("order_item");
    }

    @Test
    void rejectsAClassThatIsNotAMappedEntity() {
        var resolver = new HibernateEntityTableResolver(entityManagerFactory);

        assertThatThrownBy(() -> resolver.tableNameOf(String.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a mapped JPA entity");
    }
}
