package li.selman.persistencetest.core.normalize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import li.selman.persistencetest.core.StatementType;
import org.junit.jupiter.api.Test;

class JSqlParserSqlNormalizerTest {

    private final JSqlParserSqlNormalizer normalizer = new JSqlParserSqlNormalizer();

    @Test
    void detectsStatementTypes() {
        assertThat(normalizer.normalize("select * from customer").statementType())
                .isEqualTo(StatementType.SELECT);
        assertThat(normalizer.normalize("insert into customer (id) values (1)").statementType())
                .isEqualTo(StatementType.INSERT);
        assertThat(normalizer
                        .normalize("update customer set name = 'x' where id = 1")
                        .statementType())
                .isEqualTo(StatementType.UPDATE);
        assertThat(normalizer.normalize("delete from customer where id = 1").statementType())
                .isEqualTo(StatementType.DELETE);
        assertThat(normalizer.normalize("create table customer (id bigint)").statementType())
                .isEqualTo(StatementType.DDL);
        assertThat(normalizer.normalize("drop table customer").statementType()).isEqualTo(StatementType.DDL);
    }

    @Test
    void extractsSortedLowercasedTables() {
        var result = normalizer.normalize("select * from Orders o join Customer c on o.customer_id = c.id");

        assertThat(result.tables()).containsExactly("customer", "orders");
    }

    @Test
    void deduplicatesRepeatedTableReferences() {
        var result = normalizer.normalize("select * from customer a, customer b where a.parent_id = b.id");

        assertThat(result.tables()).containsExactly("customer");
    }

    @Test
    void isStableAcrossWhitespaceAndFormatting() {
        var compact = normalizer.normalize("select id,name from customer where id=1");
        var formatted = normalizer.normalize("""
                select
                    id,
                    name
                from
                    customer
                where
                    id = 1
                """);

        assertThat(formatted.normalizedSql()).isEqualTo(compact.normalizedSql());
    }

    @Test
    void isStableAcrossSqlComments() {
        var withoutComment = normalizer.normalize("select id from customer where id = 1");
        var withComment = normalizer.normalize(
                "select id from customer /* fetched for order */ where id = 1 -- trailing comment");

        assertThat(withComment.normalizedSql()).isEqualTo(withoutComment.normalizedSql());
    }

    @Test
    void isStableAcrossKeywordCasing() {
        var lower = normalizer.normalize("select id from customer where id = 1");
        var upper = normalizer.normalize("SELECT id FROM customer WHERE id = 1");

        assertThat(upper.normalizedSql()).isEqualTo(lower.normalizedSql());
    }

    @Test
    void isStableAcrossIdentifierQuotingStyle() {
        var unquoted = normalizer.normalize("select id from customer where id = 1");
        var doubleQuoted = normalizer.normalize("select \"id\" from \"customer\" where \"id\" = 1");

        assertThat(doubleQuoted.normalizedSql()).isEqualTo(unquoted.normalizedSql());
    }

    @Test
    void preservesDifferencesInPredicates() {
        var eq1 = normalizer.normalize("select * from customer where id = 1");
        var eq2 = normalizer.normalize("select * from customer where id = 2");

        assertThat(eq1.normalizedSql()).isNotEqualTo(eq2.normalizedSql());
    }

    @Test
    void preservesDifferencesInJoins() {
        var innerJoin = normalizer.normalize("select * from orders o join customer c on o.customer_id = c.id");
        var leftJoin = normalizer.normalize("select * from orders o left join customer c on o.customer_id = c.id");

        assertThat(innerJoin.normalizedSql()).isNotEqualTo(leftJoin.normalizedSql());
    }

    @Test
    void preservesDifferencesInSelectedColumns() {
        var idOnly = normalizer.normalize("select id from customer");
        var idAndName = normalizer.normalize("select id, name from customer");

        assertThat(idOnly.normalizedSql()).isNotEqualTo(idAndName.normalizedSql());
    }

    @Test
    void preservesDifferencesInLimit() {
        var noLimit = normalizer.normalize("select * from customer order by id");
        var limited = normalizer.normalize("select * from customer order by id limit 10");

        assertThat(noLimit.normalizedSql()).isNotEqualTo(limited.normalizedSql());
    }

    @Test
    void throwsSqlNormalizationExceptionForUnparsableSql() {
        assertThatThrownBy(() -> normalizer.normalize("not even close to sql {{{"))
                .isInstanceOf(SqlNormalizationException.class)
                .hasMessageContaining("Failed to normalize SQL");
    }

    @Test
    void normalizedQueryDefensivelyCopiesTables() {
        var tables = new java.util.ArrayList<>(List.of("customer"));
        var query = new NormalizedQuery(StatementType.SELECT, tables, "select * from customer");

        tables.add("orders");

        assertThat(query.tables()).containsExactly("customer");
    }
}
