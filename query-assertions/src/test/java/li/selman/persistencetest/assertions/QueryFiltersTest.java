package li.selman.persistencetest.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.Test;

class QueryFiltersTest {

    private final CapturedQueryFixtures fixtures = new CapturedQueryFixtures();

    @Test
    void accessesAnyTableMatchesCaseInsensitively() {
        var query = fixtures.query(StatementType.SELECT, "select * from Customer", "customer");

        assertThat(QueryFilters.accessesAnyTable("CUSTOMER").test(query)).isTrue();
        assertThat(QueryFilters.accessesAnyTable("orders").test(query)).isFalse();
    }

    @Test
    void isFlywayMetadataMatchesFlywayTable() {
        var query =
                fixtures.query(StatementType.SELECT, "select * from flyway_schema_history", "flyway_schema_history");

        assertThat(QueryFilters.isFlywayMetadata().test(query)).isTrue();
    }

    @Test
    void isLiquibaseMetadataMatchesEitherLiquibaseTable() {
        var changelog = fixtures.query(StatementType.SELECT, "select * from databasechangelog", "databasechangelog");
        var lock = fixtures.query(StatementType.SELECT, "select * from databasechangeloglock", "databasechangeloglock");

        assertThat(QueryFilters.isLiquibaseMetadata().test(changelog)).isTrue();
        assertThat(QueryFilters.isLiquibaseMetadata().test(lock)).isTrue();
    }

    @Test
    void isPostgresCatalogQueryMatchesPgAndInformationSchemaTables() {
        var pgCatalog = fixtures.query(StatementType.SELECT, "select * from pg_type", "pg_type");
        var infoSchema = fixtures.query(
                StatementType.SELECT, "select * from information_schema.tables", "information_schema.tables");
        var ordinary = fixtures.query(StatementType.SELECT, "select * from customer", "customer");

        assertThat(QueryFilters.isPostgresCatalogQuery().test(pgCatalog)).isTrue();
        assertThat(QueryFilters.isPostgresCatalogQuery().test(infoSchema)).isTrue();
        assertThat(QueryFilters.isPostgresCatalogQuery().test(ordinary)).isFalse();
    }

    @Test
    void isConnectionValidationQueryMatchesBareSelectOne() {
        var ping = fixtures.query(StatementType.SELECT, "select 1");
        var ordinary = fixtures.query(StatementType.SELECT, "select * from customer", "customer");

        assertThat(QueryFilters.isConnectionValidationQuery().test(ping)).isTrue();
        assertThat(QueryFilters.isConnectionValidationQuery().test(ordinary)).isFalse();
    }
}
