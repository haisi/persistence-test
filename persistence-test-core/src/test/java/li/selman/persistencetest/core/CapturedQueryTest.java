package li.selman.persistencetest.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CapturedQueryTest {

    @Test
    void rejectsNegativeSequence() {
        assertThatThrownBy(() -> query(-1, StatementType.SELECT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sequence");
    }

    @Test
    void defensivelyCopiesParameterList() {
        var mutableParams = new java.util.ArrayList<BindParameter>();
        mutableParams.add(BindParameter.of(1, "a"));
        var captured = new CapturedQuery(
                0,
                Instant.EPOCH,
                "select 1",
                "select 1",
                StatementType.SELECT,
                mutableParams,
                Duration.ofMillis(5),
                null,
                null,
                "main",
                "conn-1");

        mutableParams.add(BindParameter.of(2, "b"));

        assertThat(captured.parameters()).hasSize(1);
    }

    @Test
    void isFailureReflectsPresenceOfException() {
        assertThat(query(0, StatementType.SELECT).isFailure()).isFalse();

        var failed = new CapturedQuery(
                0,
                Instant.EPOCH,
                "select 1",
                "select 1",
                StatementType.SELECT,
                List.of(),
                null,
                null,
                new java.sql.SQLException("boom"),
                "main",
                "conn-1");
        assertThat(failed.isFailure()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = StatementType.class,
            names = {"INSERT", "UPDATE", "DELETE"})
    void mutationTypesAreMutations(StatementType type) {
        assertThat(query(0, type).isMutation()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = StatementType.class,
            names = {"SELECT", "DDL", "OTHER"})
    void nonMutationTypesAreNotMutations(StatementType type) {
        assertThat(query(0, type).isMutation()).isFalse();
    }

    private static CapturedQuery query(long sequence, StatementType type) {
        return new CapturedQuery(
                sequence,
                Instant.EPOCH,
                "sql",
                "sql",
                type,
                List.of(),
                Duration.ofMillis(1),
                null,
                null,
                "main",
                "conn-1");
    }
}
