package io.github.kawashima_study.hoikulink.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kawashima_study.hoikulink.shared.InvalidIdException;
import io.github.kawashima_study.hoikulink.shared.PrefixedUlid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * リフレッシュトークンのID（RefreshTokenId）のテスト
 */
@DisplayName("リフレッシュトークンのID")
class RefreshTokenIdTests {

    /**
     * 新しいIDがRTKで始まることを確認
     */
    @Test
    @DisplayName("新しいIDはRTKで始まる。")
    void newIdStartsWithPrefix() {
        // 新しいIDの値がRTKで始まることを確認
        assertThat(RefreshTokenId.newId().value()).startsWith("RTK");
    }

    /**
     * 同じ文字列から作成したIDが等しいと判定されることを確認
     */
    @Test
    @DisplayName("同じ文字列から作ったIDは等しいと判定される。")
    void idsWithSameValueAreEqual() {
        // IDの文字列を用意
        String value = RefreshTokenId.newId().value();

        // 同じ文字列から作った2つのIDが等しいことを確認
        assertThat(new RefreshTokenId(value)).isEqualTo(new RefreshTokenId(value));
    }

    /**
     * 他の種類のIDの文字列からは作成できないことを確認
     */
    @Test
    @DisplayName("他の種類のIDの文字列からは作れない。")
    void rejectsIdWithOtherPrefix() {
        // 園のID（NRSで始まる）を用意
        String nurseryId = PrefixedUlid.generate("NRS");

        // リフレッシュトークンIDとして作ると例外になることを確認
        assertThatThrownBy(() -> new RefreshTokenId(nurseryId)).isInstanceOf(InvalidIdException.class);
    }
}
