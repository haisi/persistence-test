package li.selman.persistencetest.hibernate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.Test;

class HibernateQueriesAssertTest {

    private final CapturedQueryFixtures fixtures = new CapturedQueryFixtures();

    private final EntityTableResolver resolver = entityType -> entityType == Customer.class ? "customer" : "order_item";

    @Test
    void containsSelectPassesWhenAMatchingSelectExists() {
        var queries = List.of(fixtures.query(StatementType.SELECT, "select * from customer", "customer"));

        HibernateAssertions.assertThatQueries(queries, resolver).containsSelect(Customer.class);
    }

    @Test
    void containsSelectFailsWithActionableMessageWhenMissing() {
        var queries = List.of(fixtures.query(StatementType.SELECT, "select * from order_item", "order_item"));

        assertThatThrownBy(() ->
                        HibernateAssertions.assertThatQueries(queries, resolver).containsSelect(Customer.class))
                .hasMessageContaining("Expected a SELECT query against Customer (table 'customer')")
                .hasMessageContaining("found none among 1 captured queries");
    }

    @Test
    void containsInsertMatchesOnlyInsertsAgainstTheResolvedTable() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select * from customer", "customer"),
                fixtures.query(StatementType.INSERT, "insert into customer (id) values (?)", "customer"));

        HibernateAssertions.assertThatQueries(queries, resolver).containsInsert(Customer.class);
        assertThatThrownBy(() ->
                        HibernateAssertions.assertThatQueries(queries, resolver).containsInsert(OrderItem.class))
                .hasMessageContaining("Expected a INSERT query against OrderItem (table 'order_item')");
    }

    @Test
    void containsUpdateAndContainsDeleteMatchByTypeAndTable() {
        var queries = List.of(
                fixtures.query(StatementType.UPDATE, "update customer set name = ?", "customer"),
                fixtures.query(StatementType.DELETE, "delete from order_item where id = ?", "order_item"));

        HibernateAssertions.assertThatQueries(queries, resolver)
                .containsUpdate(Customer.class)
                .containsDelete(OrderItem.class);
    }
}
