package li.selman.persistencetest.assertions;

import static li.selman.persistencetest.assertions.QueryAssertions.assertThatQueries;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import li.selman.persistencetest.core.BindParameter;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.Test;

class QueriesAssertTest {

    private final CapturedQueryFixtures fixtures = new CapturedQueryFixtures();

    @Test
    void countsQueriesByType() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select * from customer", "customer"),
                fixtures.query(StatementType.SELECT, "select * from orders", "orders"),
                fixtures.query(StatementType.UPDATE, "update customer set name = ?", "customer"));

        assertThatQueries(queries).selects(2).updates(1).inserts(0).deletes(0).hasTotalCount(3);
    }

    @Test
    void selectsFailsWithActionableMessageOnMismatch() {
        var queries = List.of(fixtures.query(StatementType.SELECT, "select * from customer", "customer"));

        assertThatThrownBy(() -> assertThatQueries(queries).selects(2))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected 2 SELECT queries but found 1")
                .hasMessageContaining("select * from customer")
                .hasMessageContaining("Summary:");
    }

    @Test
    void containsNoDeletePassesWhenThereAreNoDeletes() {
        var queries = List.of(fixtures.query(StatementType.SELECT, "select * from customer", "customer"));

        assertThatQueries(queries).containsNoDelete();
    }

    @Test
    void containsNoDeleteFailsWhenThereIsADelete() {
        var queries = List.of(fixtures.query(StatementType.DELETE, "delete from customer where id = ?", "customer"));

        assertThatThrownBy(() -> assertThatQueries(queries).containsNoDelete())
                .hasMessageContaining("Expected no DELETE queries but found 1");
    }

    @Test
    void containsTablePassesWhenAnyQueryAccessesIt() {
        var queries = List.of(fixtures.query(StatementType.SELECT, "select * from customer", "customer"));

        assertThatQueries(queries).containsTable("customer").containsTable("CUSTOMER");
    }

    @Test
    void containsTableFailsWithAccessedTablesListed() {
        var queries = List.of(fixtures.query(StatementType.SELECT, "select * from customer", "customer"));

        assertThatThrownBy(() -> assertThatQueries(queries).containsTable("orders"))
                .hasMessageContaining("Expected queries to access table 'orders'")
                .hasMessageContaining("customer");
    }

    @Test
    void hasNoNPlusOnePassesWhenShapesAreNotRepeated() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select * from customer where id = ?", "customer"),
                fixtures.query(StatementType.SELECT, "select * from orders where id = ?", "orders"));

        assertThatQueries(queries).hasNoNPlusOne();
    }

    @Test
    void hasNoNPlusOneFailsWithCandidatesListed() {
        List<CapturedQuery> queries = List.of(
                fixtures.query(
                        StatementType.SELECT,
                        "select * from order_item where order_id = ?",
                        List.of(BindParameter.of(1, 1L)),
                        "order_item"),
                fixtures.query(
                        StatementType.SELECT,
                        "select * from order_item where order_id = ?",
                        List.of(BindParameter.of(1, 2L)),
                        "order_item"));

        assertThatThrownBy(() -> assertThatQueries(queries).hasNoNPlusOne())
                .hasMessageContaining("Detected 1 likely N+1 pattern(s)")
                .hasMessageContaining("executed 2 times")
                .hasMessageContaining("order_item");
    }

    @Test
    void hasNoDuplicatesFailsOnExactRepeats() {
        List<BindParameter> params = List.of(BindParameter.of(1, 1L));
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select * from customer where id = ?", params, "customer"),
                fixtures.query(StatementType.SELECT, "select * from customer where id = ?", params, "customer"));

        assertThatThrownBy(() -> assertThatQueries(queries).hasNoDuplicates())
                .hasMessageContaining("Detected 1 duplicate query")
                .hasMessageContaining("executed 2 times");
    }

    @Test
    void lastSelectReturnsMostRecentSelect() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select * from customer", "customer"),
                fixtures.query(StatementType.UPDATE, "update customer set name = ?", "customer"),
                fixtures.query(StatementType.SELECT, "select * from orders", "orders"));

        assertThatQueries(queries).lastSelect().isSelect().hasTable("orders");
    }

    @Test
    void firstSelectReturnsEarliestSelect() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select * from customer", "customer"),
                fixtures.query(StatementType.SELECT, "select * from orders", "orders"));

        assertThatQueries(queries).firstSelect().hasTable("customer");
    }

    @Test
    void lastSelectFailsWhenThereAreNoSelects() {
        var queries = List.of(fixtures.query(StatementType.UPDATE, "update customer set name = ?", "customer"));

        assertThatThrownBy(() -> assertThatQueries(queries).lastSelect())
                .hasMessageContaining("Expected at least one SELECT query but found none");
    }

    @Test
    void firstAndLastReturnEndsOfTimelineRegardlessOfType() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select * from customer", "customer"),
                fixtures.query(StatementType.UPDATE, "update customer set name = ?", "customer"));

        assertThatQueries(queries).first().isSelect();
        assertThatQueries(queries).last().isUpdate();
    }

    @Test
    void ignoreExcludesMatchingQueriesFromSubsequentAssertions() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select * from flyway_schema_history", "flyway_schema_history"),
                fixtures.query(StatementType.SELECT, "select * from customer", "customer"));

        assertThatQueries(queries)
                .ignore(QueryFilters.isFlywayMetadata())
                .selects(1)
                .containsTable("customer");
    }

    @Test
    void ignoreTablesIsShorthandForAccessesAnyTable() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select * from flyway_schema_history", "flyway_schema_history"),
                fixtures.query(StatementType.SELECT, "select * from customer", "customer"));

        assertThatQueries(queries).ignoreTables("flyway_schema_history").hasTotalCount(1);
    }

    @Test
    void chainingReturnsSameAssertInstanceType() {
        var queries = List.of(fixtures.query(StatementType.SELECT, "select 1", "customer"));

        assertThat(assertThatQueries(queries).selects(1).containsTable("customer"))
                .isInstanceOf(QueriesAssert.class);
    }
}
