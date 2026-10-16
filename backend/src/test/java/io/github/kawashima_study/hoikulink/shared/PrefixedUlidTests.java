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

/**
 * プレフィックス付きULID（PrefixedUlid）のテスト
 */
@DisplayName("プレフィックス付きULID")
class PrefixedUlidTests {

    /** 形式が正しいユーザーID */
    private static final String VALID_ID = "USR01KABCDEFGHJKMNPQRSTVWXYZ0";

    /**
     * 作成したIDがプレフィックスで始まる29文字で形式の確認を通ることを確認
     */
    @Test
    @DisplayName("作ったIDはプレフィックスで始まる29文字で形式の確認を通る。")
    void generateReturnsValidId() {
        // IDを作成
        String id = PrefixedUlid.generate("USR");

        // 29文字で USR で始まり形式の確認を通ることを確認
        assertThat(id).hasSize(PrefixedUlid.LENGTH).startsWith("USR");
        assertThat(PrefixedUlid.requireValid("USR", id)).isEqualTo(id);
    }

    /**
     * たくさん作成しても同じIDにならないことを確認
     */
    @Test
    @DisplayName("たくさん作っても同じIDにならない。")
    void generateReturnsUniqueIds() {
        // 1万個のIDを作成
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(PrefixedUlid.generate("USR"));
        }

        // 重複がなく1万個あることを確認
        assertThat(ids).hasSize(10_000);
    }

    /**
     * 時間をおいて作成したIDが文字の順に並べると作成した順になることを確認
     *
     * @throws InterruptedException 待っている間に割り込まれた場合
     */
    @Test
    @DisplayName("時間をおいて作ったIDは文字の順に並べると作った順になる。")
    void generateReturnsIdsInTimeOrder() throws InterruptedException {
        // 2ミリ秒の間をおいて2つのIDを作成
        String first = PrefixedUlid.generate("USR");
        Thread.sleep(2);
        String second = PrefixedUlid.generate("USR");

        // 先に作ったIDのほうが文字の順で前になることを確認
        assertThat(first).isLessThan(second);
    }

    /**
     * プレフィックスが大文字3文字でなければIDを作成できないことを確認
     *
     * @param prefix 大文字3文字でないプレフィックス
     */
    @ParameterizedTest
    @ValueSource(strings = {"usr", "US", "USER", "U1R"})
    @DisplayName("プレフィックスが大文字3文字でなければIDを作れない。")
    void generateRejectsInvalidPrefix(String prefix) {
        // 小文字 2文字 4文字 数字を含むプレフィックスで作成すると例外になることを確認
        assertThatThrownBy(() -> PrefixedUlid.generate(prefix)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 形式が正しくプレフィックスが合うIDを受け付けることを確認
     */
    @Test
    @DisplayName("形式が正しくプレフィックスが合うIDは受け付ける。")
    void requireValidAcceptsValidId() {
        // 形式が正しいIDがそのまま返ることを確認
        assertThat(PrefixedUlid.requireValid("USR", VALID_ID)).isEqualTo(VALID_ID);
    }

    /**
     * 形式の違うIDやプレフィックスの違うIDを受け付けないことを確認
     *
     * @param value 形式またはプレフィックスが正しくないID
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                // 28文字（1文字少ない）
                "USR01KABCDEFGHJKMNPQRSTVWXYZ",
                // 30文字（1文字多い）
                "USR01KABCDEFGHJKMNPQRSTVWXYZ01",
                // プレフィックスが違う
                "NRS01KABCDEFGHJKMNPQRSTVWXYZ0",
                // 小文字
                "usr01kabcdefghjkmnpqrstvwxyz0",
                // ULIDで使わない文字（I）を含む
                "USR01KABCDEFGHIKMNPQRSTVWXYZ0",
                // ULIDの先頭が8（0〜7でない）
                "USR81KABCDEFGHJKMNPQRSTVWXYZ0"
            })
    @DisplayName("形式の違うIDやプレフィックスの違うIDは受け付けない。")
    void requireValidRejectsInvalidId(String value) {
        // 例外になることを確認
        assertThatThrownBy(() -> PrefixedUlid.requireValid("USR", value)).isInstanceOf(RuntimeException.class);
    }
}
