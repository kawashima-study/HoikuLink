package io.github.kawashima_study.hoikulink.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.kawashima_study.hoikulink.auth.domain.AccessToken;
import io.github.kawashima_study.hoikulink.auth.domain.AccessTokenIssuer;
import io.github.kawashima_study.hoikulink.auth.domain.EmailAddress;
import io.github.kawashima_study.hoikulink.auth.domain.InvalidEmailAddressException;
import io.github.kawashima_study.hoikulink.auth.domain.LoginLock;
import io.github.kawashima_study.hoikulink.auth.domain.PasswordHash;
import io.github.kawashima_study.hoikulink.auth.domain.PasswordVerifier;
import io.github.kawashima_study.hoikulink.auth.domain.RawPassword;
import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenGenerator;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenHash;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenRepository;
import io.github.kawashima_study.hoikulink.auth.domain.UserAccount;
import io.github.kawashima_study.hoikulink.auth.domain.UserAccountRepository;
import io.github.kawashima_study.hoikulink.shared.BusinessTimeZone;
import io.github.kawashima_study.hoikulink.shared.UserId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ログインのユースケース（LoginService）のテスト
 * <p>DBとArgon2idとJWTはMockitoの仮の部品に差し替えSpringは起動しない
 */
@ExtendWith(MockitoExtension.class) // このクラスで@Mockを使えるようにする
@DisplayName("ログインのユースケース")
class LoginServiceTests {

    /** 現在の時刻（テストデータ） */
    private static final Instant NOW = Instant.parse("2026-10-20T01:00:00Z");

    /** リフレッシュトークンの有効期限（現在の時刻の12時間後）本体と同じ計算をせず答えの日時をそのまま書く */
    private static final Instant REFRESH_TOKEN_EXPIRES_AT = Instant.parse("2026-10-20T13:00:00Z");

    /** ロックした日時（テストデータ） */
    private static final Instant LOCKED_AT = Instant.parse("2026-10-19T01:00:00Z");

    /** アカウントのユーザーID（テストデータ） */
    private static final UserId USER_ID = UserId.newId();

    /** 入力されたメールアドレス（テストデータ） */
    private static final String EMAIL = "hanako@example.com";

    /** 入力されたパスワード（テストデータ） */
    private static final String PASSWORD = "correct horse battery staple";

    /** 保存されているパスワードのハッシュ値（テストデータ） */
    private static final PasswordHash PASSWORD_HASH = new PasswordHash("$argon2id$test-hash");

    /** 発行するアクセストークン（テストデータ） */
    private static final AccessToken ACCESS_TOKEN = new AccessToken("test-access-token", Duration.ofMinutes(5));

    /** 作成する生のリフレッシュトークン（テストデータ） */
    private static final RawRefreshToken RAW_REFRESH_TOKEN = new RawRefreshToken("test-refresh-token");

    /** アカウントの保存と読み出しの仮の部品 */
    @Mock // Mockitoが仮の部品を作る（戻り値はテストの中でwhenで決める）
    private UserAccountRepository userAccountRepository;

    /** パスワードの照合の仮の部品 */
    @Mock
    private PasswordVerifier passwordVerifier;

    /** アクセストークンの発行の仮の部品 */
    @Mock
    private AccessTokenIssuer accessTokenIssuer;

    /** 生のリフレッシュトークンの作成の仮の部品 */
    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    /** リフレッシュトークンの保存の仮の部品 */
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    /** テスト対象 */
    private LoginService loginService;

    /**
     * 各テストの前にテスト対象を作成
     * <p>時刻を固定したClockを渡すため@InjectMocksを使わずnewで作る
     */
    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, BusinessTimeZone.JAPAN); // いつ実行しても同じ時刻になるよう固定する
        loginService = new LoginService(
                userAccountRepository,
                passwordVerifier,
                accessTokenIssuer,
                refreshTokenGenerator,
                refreshTokenRepository,
                clock);
    }

    /**
     * 正しいパスワードの場合にトークンを持つ成功の結果を返すことを確認
     */
    @Test
    @DisplayName("正しいパスワードならトークンを持つ成功の結果を返す。")
    void returnsSuccessWithTokens() {
        // 未ロックのアカウントと正しいパスワードとトークンの発行を用意
        UserAccount account = accountWith(LoginLock.initial());
        givenAccount(account);
        givenPasswordMatches(true);
        givenTokensIssued();

        // ログイン
        LoginResult result = loginService.login(new LoginCommand(EMAIL, PASSWORD));

        // 発行したトークンとリフレッシュトークンの有効期限を持つ成功の結果であることを確認
        assertThat(result)
                .isEqualTo(new LoginResult.Success(ACCESS_TOKEN, RAW_REFRESH_TOKEN, REFRESH_TOKEN_EXPIRES_AT));
    }

    /**
     * ログインに成功すると失敗回数を0に戻して書き込むことを確認
     */
    @Test
    @DisplayName("ログインに成功すると失敗回数を0に戻して書き込む。")
    void resetsFailedCountOnSuccess() {
        // 3回失敗しているアカウントと正しいパスワードを用意
        UserAccount account = accountWith(LoginLock.restore(3, null, null, null));
        givenAccount(account);
        givenPasswordMatches(true);
        givenTokensIssued();

        // ログイン
        loginService.login(new LoginCommand(EMAIL, PASSWORD));

        // 失敗回数が0になりアカウント自身を更新者として書き込んだことを確認
        assertThat(account.loginLock().failedCount()).isZero();
        verify(userAccountRepository).update(account, USER_ID, NOW); // 指定した引数で呼ばれたことを確認する
    }

    /**
     * ログインに成功するとリフレッシュトークンのハッシュ値を保存することを確認
     */
    @Test
    @DisplayName("ログインに成功するとリフレッシュトークンのハッシュ値を保存する。")
    void savesRefreshTokenHashOnSuccess() {
        // 未ロックのアカウントと正しいパスワードとトークンの発行を用意
        givenAccount(accountWith(LoginLock.initial()));
        givenPasswordMatches(true);
        givenTokensIssued();

        // ログイン
        loginService.login(new LoginCommand(EMAIL, PASSWORD));

        // 保存に渡されたリフレッシュトークンを取り出し生のトークンのハッシュ値を持つことを確認
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class); // 仮の部品に渡された引数を取り出す
        verify(refreshTokenRepository).save(captor.capture(), eq(USER_ID), eq(NOW));
        assertThat(captor.getValue().tokenHash()).isEqualTo(RefreshTokenHash.of(RAW_REFRESH_TOKEN));
    }

    /**
     * パスワードが正しくない場合に失敗の結果を返し失敗回数を書き込むことを確認
     */
    @Test
    @DisplayName("パスワードが正しくなければ失敗の結果を返し失敗回数を書き込む。")
    void recordsFailureForWrongPassword() {
        // 未ロックのアカウントと正しくないパスワードを用意
        UserAccount account = accountWith(LoginLock.initial());
        givenAccount(account);
        givenPasswordMatches(false);

        // ログイン
        LoginResult result = loginService.login(new LoginCommand(EMAIL, PASSWORD));

        // 失敗の結果を返し失敗回数1を書き込みトークンを発行しないことを確認
        assertThat(result).isEqualTo(new LoginResult.InvalidCredentials());
        assertThat(account.loginLock().failedCount()).isEqualTo(1);
        verify(userAccountRepository).update(account, USER_ID, NOW);
        verifyNoInteractions(accessTokenIssuer, refreshTokenGenerator, refreshTokenRepository); // 一度も呼ばれていないことを確認する
    }

    /**
     * 5回目の失敗でアカウントをロックすることを確認
     */
    @Test
    @DisplayName("5回目の失敗でアカウントをロックする。")
    void locksAccountOnFifthFailure() {
        // 4回失敗しているアカウントと正しくないパスワードを用意
        UserAccount account = accountWith(LoginLock.restore(4, null, null, null));
        givenAccount(account);
        givenPasswordMatches(false);

        // ログイン
        LoginResult result = loginService.login(new LoginCommand(EMAIL, PASSWORD));

        // 失敗の結果を返しロックした状態を書き込んだことを確認
        assertThat(result).isEqualTo(new LoginResult.InvalidCredentials());
        assertThat(account.isLocked()).isTrue();
        verify(userAccountRepository).update(account, USER_ID, NOW);
    }

    /**
     * アカウントがない場合にダミーで照合し失敗の結果を返すことを確認
     */
    @Test
    @DisplayName("アカウントがなければダミーで照合し失敗の結果を返す。")
    void verifiesDummyForUnknownAccount() {
        // アカウントがない状態を用意
        when(userAccountRepository.findByEmailForUpdate(new EmailAddress(EMAIL)))
                .thenReturn(Optional.empty());

        // ログイン
        LoginResult result = loginService.login(new LoginCommand(EMAIL, PASSWORD));

        // 失敗の結果を返しダミーで照合し書き込みもトークンの発行もしないことを確認
        assertThat(result).isEqualTo(new LoginResult.InvalidCredentials());
        verify(passwordVerifier).matchesDummy(new RawPassword(PASSWORD));
        verify(userAccountRepository, never()).update(any(), any(), any()); // 一度も呼ばれていないことを確認する
        verifyNoInteractions(accessTokenIssuer, refreshTokenGenerator, refreshTokenRepository);
    }

    /**
     * ロック中でパスワードが正しい場合にロック中の結果を返すことを確認
     */
    @Test
    @DisplayName("ロック中でパスワードが正しければロック中の結果を返す。")
    void returnsLockedForLockedAccountWithCorrectPassword() {
        // ロック中のアカウントと正しいパスワードを用意
        givenAccount(accountWith(LoginLock.restore(5, LOCKED_AT, null, null)));
        givenPasswordMatches(true);

        // ログイン
        LoginResult result = loginService.login(new LoginCommand(EMAIL, PASSWORD));

        // ロック中の結果を返し書き込みもトークンの発行もしないことを確認
        assertThat(result).isEqualTo(new LoginResult.AccountLocked());
        verify(userAccountRepository, never()).update(any(), any(), any());
        verifyNoInteractions(accessTokenIssuer, refreshTokenGenerator, refreshTokenRepository);
    }

    /**
     * ロック中でパスワードが正しくない場合に失敗の結果を返すことを確認
     * <p>パスワードを知らない人にアカウントがあることを伝えないため
     */
    @Test
    @DisplayName("ロック中でパスワードが正しくなければ失敗の結果を返す。")
    void returnsInvalidCredentialsForLockedAccountWithWrongPassword() {
        // ロック中のアカウントと正しくないパスワードを用意
        givenAccount(accountWith(LoginLock.restore(5, LOCKED_AT, null, null)));
        givenPasswordMatches(false);

        // ログイン
        LoginResult result = loginService.login(new LoginCommand(EMAIL, PASSWORD));

        // 失敗の結果を返し書き込まないことを確認
        assertThat(result).isEqualTo(new LoginResult.InvalidCredentials());
        verify(userAccountRepository, never()).update(any(), any(), any());
    }

    /**
     * メールアドレスの形式が正しくない場合に例外になりDBを読まないことを確認
     */
    @Test
    @DisplayName("メールアドレスの形式が正しくなければ例外になりアカウントを探さない。")
    void rejectsInvalidEmailAddress() {
        // 形式の誤ったメールアドレスでログインすると例外になることを確認
        assertThatThrownBy(() -> loginService.login(new LoginCommand("hanako", PASSWORD)))
                .isInstanceOf(InvalidEmailAddressException.class);

        // アカウントを探していないことを確認
        verifyNoInteractions(userAccountRepository);
    }

    /**
     * 指定したログインの失敗回数とロックの状態のアカウントを作成
     *
     * @param loginLock ログインの失敗回数とロックの状態
     * @return テスト用のアカウント
     */
    private static UserAccount accountWith(LoginLock loginLock) {
        return UserAccount.restore(USER_ID, new EmailAddress(EMAIL), PASSWORD_HASH, loginLock);
    }

    /**
     * メールアドレスで探すとアカウントが見つかる状態にする
     *
     * @param account 見つかるアカウント
     */
    private void givenAccount(UserAccount account) {
        // この引数で呼ばれたらこの値を返すと決める
        when(userAccountRepository.findByEmailForUpdate(new EmailAddress(EMAIL)))
                .thenReturn(Optional.of(account));
    }

    /**
     * パスワードの照合の結果を決める
     *
     * @param matches 照合の結果
     */
    private void givenPasswordMatches(boolean matches) {
        when(passwordVerifier.matches(new RawPassword(PASSWORD), PASSWORD_HASH)).thenReturn(matches);
    }

    /**
     * トークンを発行できる状態にする
     */
    private void givenTokensIssued() {
        when(accessTokenIssuer.issue(USER_ID, NOW)).thenReturn(ACCESS_TOKEN);
        when(refreshTokenGenerator.generate()).thenReturn(RAW_REFRESH_TOKEN);
    }
}
