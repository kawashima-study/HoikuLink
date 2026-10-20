package io.github.kawashima_study.hoikulink.auth.application;

import io.github.kawashima_study.hoikulink.auth.domain.AccessToken;
import io.github.kawashima_study.hoikulink.auth.domain.AccessTokenIssuer;
import io.github.kawashima_study.hoikulink.auth.domain.EmailAddress;
import io.github.kawashima_study.hoikulink.auth.domain.PasswordVerifier;
import io.github.kawashima_study.hoikulink.auth.domain.RawPassword;
import io.github.kawashima_study.hoikulink.auth.domain.RawRefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshToken;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenGenerator;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenHash;
import io.github.kawashima_study.hoikulink.auth.domain.RefreshTokenRepository;
import io.github.kawashima_study.hoikulink.auth.domain.UserAccount;
import io.github.kawashima_study.hoikulink.auth.domain.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ログインのユースケースの実装
 * <p>アカウントの読み込みからトークンの発行までを1つのトランザクションで行う
 */
@Service
class LoginService implements LoginUseCase {

    /** ログの出力先 */
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginService.class);

    /** アカウントの保存と読み出し */
    private final UserAccountRepository userAccountRepository;

    /** パスワードの照合 */
    private final PasswordVerifier passwordVerifier;

    /** アクセストークンの発行 */
    private final AccessTokenIssuer accessTokenIssuer;

    /** 生のリフレッシュトークンの作成 */
    private final RefreshTokenGenerator refreshTokenGenerator;

    /** リフレッシュトークンの保存 */
    private final RefreshTokenRepository refreshTokenRepository;

    /** 現在の時刻（テストで固定できるよう部品として受け取る） */
    private final Clock clock;

    /**
     * ログインのユースケースを作成
     * <p>コンストラクタが1つのためSpringが部品を自動で渡す
     *
     * @param userAccountRepository アカウントの保存と読み出し
     * @param passwordVerifier パスワードの照合
     * @param accessTokenIssuer アクセストークンの発行
     * @param refreshTokenGenerator 生のリフレッシュトークンの作成
     * @param refreshTokenRepository リフレッシュトークンの保存
     * @param clock 現在の時刻
     */
    LoginService(
            UserAccountRepository userAccountRepository,
            PasswordVerifier passwordVerifier,
            AccessTokenIssuer accessTokenIssuer,
            RefreshTokenGenerator refreshTokenGenerator,
            RefreshTokenRepository refreshTokenRepository,
            Clock clock) {
        this.userAccountRepository = userAccountRepository;
        this.passwordVerifier = passwordVerifier;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    /**
     * メールアドレスとパスワードでログインする
     * <p>失敗でも失敗回数の書き込みを確定させるため例外ではなく結果を返す
     *
     * @param command ログインの入力
     * @return ログインの結果（成功・失敗・ロック中のいずれか）
     * @throws io.github.kawashima_study.hoikulink.auth.domain.InvalidEmailAddressException メールアドレスの長さまたは形式が正しくない場合
     */
    @Override
    @Transactional
    public LoginResult login(LoginCommand command) {
        EmailAddress emailAddress = new EmailAddress(command.email()); // 形式の誤りはここで例外になる（書き込みの前のため取り消される書き込みはない）
        RawPassword rawPassword = new RawPassword(command.password());
        Instant now = Instant.now(clock);

        // 行をロックしトランザクションの終わりまで同じアカウントへの後からの処理を待たせる
        Optional<UserAccount> found = userAccountRepository.findByEmailForUpdate(emailAddress);
        if (found.isEmpty()) {
            passwordVerifier.matchesDummy(rawPassword); // アカウントがない場合も照合して処理時間をそろえる
            LOGGER.info("ログインに失敗しました。アカウントがありません。"); // メールアドレスは出力しない
            return new LoginResult.InvalidCredentials();
        }

        UserAccount account = found.get();
        boolean matches = passwordVerifier.matches(rawPassword, account.passwordHash());

        if (account.isLocked()) {
            return rejectLocked(account, matches);
        }
        if (!matches) {
            return recordFailure(account, now);
        }

        account.recordLoginSuccess();
        userAccountRepository.update(account, account.userId(), now); // 更新者はアカウント自身
        LoginResult result = issueTokens(account, now);
        LOGGER.info("ログインに成功しました。userId={}", account.userId());
        return result;
    }

    /**
     * ロック中のアカウントでのログインを断る
     * <p>失敗回数とロックの状態は変えない
     *
     * @param account ロック中のアカウント
     * @param matches パスワードの照合の結果
     * @return パスワードが正しい場合はロック中の結果 正しくない場合はログインの失敗の結果
     */
    private LoginResult rejectLocked(UserAccount account, boolean matches) {
        LOGGER.info("ロック中のアカウントでログインに失敗しました。userId={}", account.userId());
        // パスワードを知らない人にはアカウントの有無を伝えないため正しい場合だけロック中を返す
        return matches ? new LoginResult.AccountLocked() : new LoginResult.InvalidCredentials();
    }

    /**
     * パスワードが正しくない場合に失敗を記録する
     * <p>5回目の失敗でロックする
     *
     * @param account ログインに失敗したアカウント
     * @param now 現在の時刻
     * @return ログインの失敗の結果
     */
    private LoginResult recordFailure(UserAccount account, Instant now) {
        account.recordLoginFailure(now);
        userAccountRepository.update(account, account.userId(), now);
        if (account.isLocked()) {
            LOGGER.warn("ログインに続けて失敗したためアカウントをロックしました。userId={}", account.userId()); // 攻撃の可能性があるため後から探しやすいWARNにする
        } else {
            LOGGER.info(
                    "ログインに失敗しました。userId={} failedCount={}",
                    account.userId(),
                    account.loginLock().failedCount());
        }
        return new LoginResult.InvalidCredentials();
    }

    /**
     * アクセストークンとリフレッシュトークンを発行する
     * <p>リフレッシュトークンはハッシュ値だけをDBに保存する
     *
     * @param account ログインに成功したアカウント
     * @param now 現在の時刻
     * @return ログインの成功の結果
     */
    private LoginResult issueTokens(UserAccount account, Instant now) {
        AccessToken accessToken = accessTokenIssuer.issue(account.userId(), now);
        RawRefreshToken rawRefreshToken = refreshTokenGenerator.generate();
        RefreshToken refreshToken = RefreshToken.issue(account.userId(), RefreshTokenHash.of(rawRefreshToken), now);
        refreshTokenRepository.save(refreshToken, account.userId(), now); // 作成者はアカウント自身
        return new LoginResult.Success(accessToken, rawRefreshToken, refreshToken.expiresAt());
    }
}
