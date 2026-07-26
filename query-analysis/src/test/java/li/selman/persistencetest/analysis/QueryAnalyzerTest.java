package li.selman.persistencetest.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import li.selman.persistencetest.core.BindParameter;
import li.selman.persistencetest.core.CapturedQuery;
import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.Test;

class QueryAnalyzerTest {

    private final CapturedQueryFixtures fixtures = new CapturedQueryFixtures();

    @Test
    void accessedTablesOfUnionsTables() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select 1", "customer"),
                fixtures.query(StatementType.UPDATE, "update 1", "orders"));

        assertThat(QueryAnalyzer.accessedTablesOf(queries)).containsExactlyInAnyOrder("customer", "orders");
    }

    @Test
    void timelineOfOrdersBySequenceRegardlessOfInputOrder() {
        CapturedQuery first = fixtures.query(StatementType.SELECT, "select 1", "customer");
        CapturedQuery second = fixtures.query(StatementType.SELECT, "select 2", "customer");

        List<CapturedQuery> timeline = QueryAnalyzer.timelineOf(List.of(second, first));

        assertThat(timeline).extracting(CapturedQuery::sequence).containsExactly(0L, 1L);
    }

    @Test
    void duplicatesOfGroupsSameSqlAndSameParameters() {
        List<BindParameter> params = List.of(BindParameter.of(1, 42L));
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select * from customer where id = ?", params, "customer"),
                fixtures.query(StatementType.SELECT, "select * from customer where id = ?", params, "customer"),
                fixtures.query(StatementType.SELECT, "select * from orders", "orders"));

        List<DuplicateQueryGroup> duplicates = QueryAnalyzer.duplicatesOf(queries);

        assertThat(duplicates).hasSize(1);
        assertThat(duplicates.get(0).normalizedSql()).isEqualTo("select * from customer where id = ?");
        assertThat(duplicates.get(0).occurrenceCount()).isEqualTo(2);
    }

    @Test
    void duplicatesOfDoesNotGroupSameSqlWithDifferentParameters() {
        var queries = List.of(
                fixtures.query(
                        StatementType.SELECT,
                        "select * from customer where id = ?",
                        List.of(BindParameter.of(1, 1L)),
                        "customer"),
                fixtures.query(
                        StatementType.SELECT,
                        "select * from customer where id = ?",
                        List.of(BindParameter.of(1, 2L)),
                        "customer"));

        assertThat(QueryAnalyzer.duplicatesOf(queries)).isEmpty();
    }

    @Test
    void repeatedShapesOfGroupsSameSqlRegardlessOfParameters() {
        var queries = List.of(
                fixtures.query(
                        StatementType.SELECT,
                        "select * from customer where id = ?",
                        List.of(BindParameter.of(1, 1L)),
                        "customer"),
                fixtures.query(
                        StatementType.SELECT,
                        "select * from customer where id = ?",
                        List.of(BindParameter.of(1, 2L)),
                        "customer"),
                fixtures.query(StatementType.SELECT, "select * from orders", "orders"));

        List<RepeatedQueryShape> shapes = QueryAnalyzer.repeatedShapesOf(queries);

        assertThat(shapes).hasSize(1);
        assertThat(shapes.get(0).occurrenceCount()).isEqualTo(2);
    }

    @Test
    void repeatedShapesOfIgnoresShapesSeenOnlyOnce() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select * from customer", "customer"),
                fixtures.query(StatementType.SELECT, "select * from orders", "orders"));

        assertThat(QueryAnalyzer.repeatedShapesOf(queries)).isEmpty();
    }

    @Test
    void nPlusOneCandidatesOfFlagsRepeatedSelectShapes() {
        var queries = List.of(
                fixtures.query(
                        StatementType.SELECT,
                        "select * from order_item where order_id = ?",
                        List.of(BindParameter.of(1, 1L)),
                        "order_item"),
                fixtures.query(
                        StatementType.SELECT,
                        "select * from order_item where order_id = ?",
                        List.of(BindParameter.of(1, 2L)),
                        "order_item"),
                fixtures.query(
                        StatementType.SELECT,
                        "select * from order_item where order_id = ?",
                        List.of(BindParameter.of(1, 3L)),
                        "order_item"));

        List<RepeatedQueryShape> candidates = QueryAnalyzer.nPlusOneCandidatesOf(queries);

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).occurrenceCount()).isEqualTo(3);
    }

    @Test
    void nPlusOneCandidatesOfIgnoresNonSelectRepeatedShapes() {
        var queries = List.of(
                fixtures.query(
                        StatementType.UPDATE,
                        "update customer set name = ? where id = ?",
                        List.of(BindParameter.of(1, "a"), BindParameter.of(2, 1L)),
                        "customer"),
                fixtures.query(
                        StatementType.UPDATE,
                        "update customer set name = ? where id = ?",
                        List.of(BindParameter.of(1, "b"), BindParameter.of(2, 2L)),
                        "customer"));

        assertThat(QueryAnalyzer.nPlusOneCandidatesOf(queries)).isEmpty();
    }

    @Test
    void nPlusOneCandidatesOfRespectsCustomThreshold() {
        var queries = List.of(
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

        assertThat(QueryAnalyzer.nPlusOneCandidatesOf(queries, 2)).hasSize(1);
        assertThat(QueryAnalyzer.nPlusOneCandidatesOf(queries, 3)).isEmpty();
    }

    @Test
    void nPlusOneCandidatesOfRejectsThresholdBelowTwo() {
        assertThatThrownBy(() -> QueryAnalyzer.nPlusOneCandidatesOf(List.of(), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threshold");
    }
}
