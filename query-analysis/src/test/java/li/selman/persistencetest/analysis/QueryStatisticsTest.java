package li.selman.persistencetest.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.Test;

class QueryStatisticsTest {

    private final CapturedQueryFixtures fixtures = new CapturedQueryFixtures();

    @Test
    void emptyListYieldsZeroedStatistics() {
        var stats = QueryStatistics.of(List.of());

        assertThat(stats.totalCount()).isZero();
        assertThat(stats.countOf(StatementType.SELECT)).isZero();
        assertThat(stats.totalDuration()).isZero();
        assertThat(stats.averageDuration()).isZero();
        assertThat(stats.accessedTables()).isEmpty();
    }

    @Test
    void countsQueriesByType() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select 1", "customer"),
                fixtures.query(StatementType.SELECT, "select 2", "customer"),
                fixtures.query(StatementType.INSERT, "insert 1", "customer"));

        var stats = QueryStatistics.of(queries);

        assertThat(stats.totalCount()).isEqualTo(3);
        assertThat(stats.countOf(StatementType.SELECT)).isEqualTo(2);
        assertThat(stats.countOf(StatementType.INSERT)).isEqualTo(1);
        assertThat(stats.countOf(StatementType.DELETE)).isZero();
    }

    @Test
    void sumsDurationAndAverages() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select 1", "customer"),
                fixtures.query(StatementType.SELECT, "select 2", "customer"));

        var stats = QueryStatistics.of(queries);

        assertThat(stats.totalDuration()).isEqualTo(Duration.ofMillis(2));
        assertThat(stats.averageDuration()).isEqualTo(Duration.ofMillis(1));
    }

    @Test
    void unionsAccessedTablesAcrossQueries() {
        var queries = List.of(
                fixtures.query(StatementType.SELECT, "select 1", "customer"),
                fixtures.query(StatementType.SELECT, "select 2", "orders", "customer"));

        var stats = QueryStatistics.of(queries);

        assertThat(stats.accessedTables()).containsExactlyInAnyOrder("customer", "orders");
    }
}
