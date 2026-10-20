package io.github.kawashima_study.hoikulink.auth.application;

/**
 * アクセストークンの再発行のユースケースの入口
 */
public interface RefreshUseCase {

    /**
     * リフレッシュトークンを交換しアクセストークンを再発行する
     *
     * @param command 再発行の入力
     * @return 再発行の結果（成功・再発行できないのいずれか）
     */
    RefreshResult refresh(RefreshCommand command);
}
