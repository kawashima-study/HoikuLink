package io.github.kawashima_study.hoikulink.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("プレフィックス付きULID")
class PrefixedUlidTests {

    private static final String VALID_ID = "USR01KABCDEFGHJKMNPQRSTVWXYZ0";

    @Test
    @DisplayName("作ったIDは、プレフィックスで始まる29文字で、形式の確認を通る")
    void generateReturnsValidId() {
        String id = PrefixedUlid.generate("USR");

        assertThat(id).hasSize(PrefixedUlid.LENGTH).startsWith("USR");
        assertThat(PrefixedUlid.requireValid("USR", id)).isEqualTo(id);
    }

    @Test
    @DisplayName("たくさん作っても、同じIDにならない")
    void generateReturnsUniqueIds() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(PrefixedUlid.generate("USR"));
        }

        assertThat(ids).hasSize(10_000);
    }

    @Test
    @DisplayName("時間をおいて作ったIDは、文字の順に並べると作った順になる")
    void generateReturnsIdsInTimeOrder() throws InterruptedException {
        String first = PrefixedUlid.generate("USR");
        Thread.sleep(2);
        String second = PrefixedUlid.generate("USR");

        assertThat(first).isLessThan(second);
    }

    @ParameterizedTest
    @ValueSource(strings = {"usr", "US", "USER", "U1R"})
    @DisplayName("プレフィックスが大文字3文字でなければ、IDを作れない")
    void generateRejectsInvalidPrefix(String prefix) {
        assertThatThrownBy(() -> PrefixedUlid.generate(prefix)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("形式が正しく、プレフィックスが合うIDは受け付ける")
    void requireValidAcceptsValidId() {
        assertThat(PrefixedUlid.requireValid("USR", VALID_ID)).isEqualTo(VALID_ID);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "USR01KABCDEFGHJKMNPQRSTVWXYZ",
                "USR01KABCDEFGHJKMNPQRSTVWXYZ01",
                "NRS01KABCDEFGHJKMNPQRSTVWXYZ0",
                "usr01kabcdefghjkmnpqrstvwxyz0",
                "USR01KABCDEFGHIKMNPQRSTVWXYZ0",
                "USR81KABCDEFGHJKMNPQRSTVWXYZ0"
            })
    @DisplayName("形式の違うIDや、プレフィックスの違うIDは受け付けない")
    void requireValidRejectsInvalidId(String value) {
        assertThatThrownBy(() -> PrefixedUlid.requireValid("USR", value)).isInstanceOf(RuntimeException.class);
    }
}
