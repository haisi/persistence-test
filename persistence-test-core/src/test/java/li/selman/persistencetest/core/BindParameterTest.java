package li.selman.persistencetest.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BindParameterTest {

    @Test
    void rejectsNonPositivePosition() {
        assertThatThrownBy(() -> new BindParameter(0, "x", "java.lang.String"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("position");
    }

    @Test
    void ofDerivesTypeNameFromValue() {
        var param = BindParameter.of(1, "hello");

        assertThat(param.position()).isEqualTo(1);
        assertThat(param.value()).isEqualTo("hello");
        assertThat(param.typeName()).isEqualTo("java.lang.String");
    }

    @Test
    void ofLeavesTypeNameNullForNullValue() {
        var param = BindParameter.of(1, null);

        assertThat(param.value()).isNull();
        assertThat(param.typeName()).isNull();
    }
}
