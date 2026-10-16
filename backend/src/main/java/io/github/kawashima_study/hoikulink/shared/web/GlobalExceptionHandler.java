package io.github.kawashima_study.hoikulink.shared.web;

import io.github.kawashima_study.hoikulink.shared.ApplicationException;
import io.github.kawashima_study.hoikulink.shared.CommonErrorCode;
import io.github.kawashima_study.hoikulink.shared.ErrorCode;
import io.github.kawashima_study.hoikulink.shared.InvalidIdException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * すべての例外をRFC 9457の形式（code と requestId を追加）の応答に変換する
 * <p>500のときは内部の情報（例外のメッセージなど）を返さない
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** ログの出力先 */
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** エラーの種類（type）の先頭に付ける文字列 */
    private static final String TYPE_PREFIX = "urn:hoikulink:error:";

    /** 応答に追加するエラーコードの項目名 */
    private static final String CODE = "code";

    /** Springが返すHTTPのステータスと共通のエラーコードの対応 */
    private static final Map<HttpStatus, CommonErrorCode> CODES_BY_STATUS = Map.of(
            HttpStatus.UNAUTHORIZED, CommonErrorCode.UNAUTHENTICATED,
            HttpStatus.FORBIDDEN, CommonErrorCode.FORBIDDEN,
            HttpStatus.NOT_FOUND, CommonErrorCode.RESOURCE_NOT_FOUND,
            HttpStatus.TOO_MANY_REQUESTS, CommonErrorCode.TOO_MANY_REQUESTS,
            HttpStatus.SERVICE_UNAVAILABLE, CommonErrorCode.SERVICE_UNAVAILABLE);

    /** エラーコードからHTTPのステータスを決める部品 */
    private final ErrorStatusResolver statusResolver;

    /**
     * 例外の変換の部品を作成
     *
     * @param statusResolver エラーコードからHTTPのステータスを決める部品
     */
    public GlobalExceptionHandler(ErrorStatusResolver statusResolver) {
        this.statusResolver = statusResolver;
    }

    /**
     * 業務のエラーをエラーコードに対応するステータスの応答に変換
     *
     * @param ex 業務のエラー
     * @return エラーの応答
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ProblemDetail> handleApplicationException(ApplicationException ex) {
        ErrorCode errorCode = ex.errorCode();
        HttpStatus status = statusResolver.resolve(errorCode);
        LOGGER.info("業務のエラーを返します。code={}", errorCode.code());
        return ResponseEntity.status(status).body(problem(status, errorCode, ex.getMessage()));
    }

    /**
     * IDの形式の誤りを存在しない場合と同じ404の応答に変換
     *
     * @param ex IDの形式の誤り
     * @return エラーの応答
     */
    @ExceptionHandler(InvalidIdException.class)
    public ResponseEntity<ProblemDetail> handleInvalidId(InvalidIdException ex) {
        ErrorCode errorCode = CommonErrorCode.RESOURCE_NOT_FOUND;
        HttpStatus status = statusResolver.resolve(errorCode);
        LOGGER.info("IDの形式が正しくないため、対象なしとして扱います。{}", ex.getMessage());
        return ResponseEntity.status(status).body(problem(status, errorCode, null));
    }

    /**
     * 想定外のエラーを内部の情報を含まない500の応答に変換
     *
     * @param ex 想定外のエラー
     * @return エラーの応答
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        ErrorCode errorCode = CommonErrorCode.INTERNAL_ERROR;
        HttpStatus status = statusResolver.resolve(errorCode);
        LOGGER.error("想定外のエラーが発生しました。", ex);
        return ResponseEntity.status(status).body(problem(status, errorCode, null));
    }

    /**
     * 入力チェックのエラーを項目ごとの一覧を含む応答に変換
     *
     * @param ex 入力チェックのエラー
     * @param headers 応答のヘッダー
     * @param status Springが決めたHTTPのステータス
     * @param request リクエスト
     * @return エラーの応答
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorCode errorCode = CommonErrorCode.VALIDATION_ERROR;
        HttpStatus validationStatus = statusResolver.resolve(errorCode);
        ProblemDetail body = problem(validationStatus, errorCode, "入力内容を確認してください。");
        body.setProperty("errors", fieldErrors(ex));
        return handleExceptionInternal(ex, body, headers, validationStatus, request);
    }

    /**
     * Springが作った応答に code と requestId がなければこのシステムの形に作り直す
     *
     * @param ex 発生した例外
     * @param body 応答の本文
     * @param headers 応答のヘッダー
     * @param statusCode HTTPのステータス
     * @param request リクエスト
     * @return エラーの応答
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (hasErrorCode(body)) {
            return super.handleExceptionInternal(ex, body, headers, statusCode, request);
        }
        LOGGER.info(
                "リクエストを処理できませんでした。status={}, reason={}",
                statusCode.value(),
                ex.getClass().getSimpleName());
        ProblemDetail problem = problem(statusCode, commonCodeOf(statusCode), null);
        return super.handleExceptionInternal(ex, problem, headers, statusCode, request);
    }

    /**
     * RFC 9457の形式の応答の本文を作成
     *
     * @param status HTTPのステータス
     * @param errorCode エラーコード
     * @param detail 詳細の説明（返さない場合は null）
     * @return 応答の本文
     */
    private static ProblemDetail problem(HttpStatusCode status, ErrorCode errorCode, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(
                TYPE_PREFIX + errorCode.code().toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setTitle(errorCode.title());
        problem.setDetail(detail);
        problem.setProperty(CODE, errorCode.code());
        problem.setProperty(RequestIdFilter.MDC_KEY, MDC.get(RequestIdFilter.MDC_KEY));
        return problem;
    }

    /**
     * 応答の本文にエラーコードが入っているかを返す
     *
     * @param body 応答の本文
     * @return エラーコードが入っている場合 true
     */
    private static boolean hasErrorCode(Object body) {
        return body instanceof ProblemDetail detail
                && detail.getProperties() != null
                && detail.getProperties().containsKey(CODE);
    }

    /**
     * HTTPのステータスから共通のエラーコードを選ぶ
     *
     * @param status HTTPのステータス
     * @return 共通のエラーコード（対応がない場合は4xxなら INVALID_REQUEST それ以外は INTERNAL_ERROR）
     */
    private static ErrorCode commonCodeOf(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        CommonErrorCode errorCode = known == null ? null : CODES_BY_STATUS.get(known);
        if (errorCode != null) {
            return errorCode;
        }
        return status.is4xxClientError() ? CommonErrorCode.INVALID_REQUEST : CommonErrorCode.INTERNAL_ERROR;
    }

    /**
     * 入力チェックのエラーを項目ごとの一覧に変換
     * <p>入力された値は個人の情報を含みうるため返さずに項目とコードとメッセージのみを返す
     *
     * @param ex 入力チェックのエラー
     * @return 項目ごとのエラーの一覧
     */
    private static List<FieldErrorItem> fieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toItem)
                .toList();
    }

    /**
     * 1項目の入力チェックのエラーを応答の形に変換
     * <p>制約の名前（例 NotBlank）を大文字とアンダースコアのコード（例 NOT_BLANK）にする
     *
     * @param error 1項目の入力チェックのエラー
     * @return 応答に入れる1項目のエラー
     */
    private static FieldErrorItem toItem(FieldError error) {
        String constraint = Objects.requireNonNullElse(error.getCode(), "Invalid");
        String code = constraint.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        return new FieldErrorItem(error.getField(), code, Objects.requireNonNullElse(error.getDefaultMessage(), ""));
    }

    /**
     * 応答に入れる1項目の入力チェックのエラー
     *
     * @param field 項目名
     * @param code エラーのコード（例 NOT_BLANK）
     * @param message エラーのメッセージ
     */
    public record FieldErrorItem(String field, String code, String message) {}
}
