package li.selman.persistencetest.assertions;

import static li.selman.persistencetest.assertions.QueryAssertions.assertThatQueries;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import li.selman.persistencetest.core.BindParameter;
import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.Test;

class SingleCapturedQueryAssertTest {

    private final CapturedQueryFixtures fixtures = new CapturedQueryFixtures();

    @Test
    void capturedQueryExposesTheWrappedQuery() {
        var query = fixtures.query(StatementType.SELECT, "select * from customer", "customer");

        assertThat(assertThatQueries(List.of(query)).lastSelect().capturedQuery())
                .isEqualTo(query);
    }

    @Test
    void isSelectFailsForNonSelect() {
        var query = fixtures.query(StatementType.UPDATE, "update customer set name = ?", "customer");

        assertThatThrownBy(() -> assertThatQueries(List.of(query)).last().isSelect())
                .hasMessageContaining("Expected query to be SELECT but was UPDATE");
    }

    @Test
    void hasTableFailsWhenTableNotAccessed() {
        var query = fixtures.query(StatementType.SELECT, "select * from customer", "customer");

        assertThatThrownBy(() -> assertThatQueries(List.of(query)).last().hasTable("orders"))
                .hasMessageContaining("Expected query to access table 'orders'")
                .hasMessageContaining("[customer]");
    }

    @Test
    void hasParameterCountChecksBindParameterCount() {
        var query = fixtures.query(
                StatementType.SELECT,
                "select * from customer where id = ?",
                List.of(BindParameter.of(1, 1L)),
                "customer");

        assertThatQueries(List.of(query)).last().hasParameterCount(1);
        assertThatThrownBy(() -> assertThatQueries(List.of(query)).last().hasNoBindParameters())
                .hasMessageContaining("Expected 0 bind parameter(s) but found 1");
    }
}
