package io.github.kawashima_study.hoikulink.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * アカウントのID（UserId）のテスト
 */
@DisplayName("アカウントのID")
class UserIdTests {

    /**
     * 新しいIDが USR で始まることを確認
     */
    @Test
    @DisplayName("新しいIDはUSRで始まる。")
    void newIdStartsWithPrefix() {
        // 新しいIDの値が USR で始まることを確認
        assertThat(UserId.newId().value()).startsWith("USR");
    }

    /**
     * 同じ文字列から作成したIDが等しいと判定されることを確認
     */
    @Test
    @DisplayName("同じ文字列から作ったIDは等しいと判定される。")
    void idsWithSameValueAreEqual() {
        // IDの文字列を用意
        String value = UserId.newId().value();

        // 同じ文字列から作った2つのIDが等しいことを確認
        assertThat(new UserId(value)).isEqualTo(new UserId(value));
    }

    /**
     * 他の種類のIDの文字列からは作成できないことを確認
     */
    @Test
    @DisplayName("他の種類のIDの文字列からは作れない。")
    void rejectsIdWithOtherPrefix() {
        // 園のID（NRS で始まる）を用意
        String nurseryId = PrefixedUlid.generate("NRS");

        // ユーザーIDとして作ると例外になることを確認
        assertThatThrownBy(() -> new UserId(nurseryId)).isInstanceOf(IllegalArgumentException.class);
    }
}
