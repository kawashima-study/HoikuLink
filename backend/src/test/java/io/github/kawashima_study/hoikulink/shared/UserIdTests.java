package io.github.kawashima_study.hoikulink.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("アカウントのID")
class UserIdTests {

    @Test
    @DisplayName("新しいIDは、USRで始まる")
    void newIdStartsWithPrefix() {
        assertThat(UserId.newId().value()).startsWith("USR");
    }

    @Test
    @DisplayName("同じ文字列から作ったIDは、等しいと判定される")
    void idsWithSameValueAreEqual() {
        String value = UserId.newId().value();

        assertThat(new UserId(value)).isEqualTo(new UserId(value));
    }

    @Test
    @DisplayName("他の種類のIDの文字列からは作れない")
    void rejectsIdWithOtherPrefix() {
        String nurseryId = PrefixedUlid.generate("NRS");

        assertThatThrownBy(() -> new UserId(nurseryId)).isInstanceOf(IllegalArgumentException.class);
    }
}
