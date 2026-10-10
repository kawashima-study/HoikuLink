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
 * すべての例外を、RFC 9457の形式（code・requestIdを追加）の応答に変換する。
 * 500のときは、内部の情報（例外のメッセージなど）を返さない。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String TYPE_PREFIX = "urn:hoikulink:error:";

    private static final String CODE = "code";

    private static final Map<HttpStatus, CommonErrorCode> CODES_BY_STATUS = Map.of(
            HttpStatus.UNAUTHORIZED, CommonErrorCode.UNAUTHENTICATED,
            HttpStatus.FORBIDDEN, CommonErrorCode.FORBIDDEN,
            HttpStatus.NOT_FOUND, CommonErrorCode.RESOURCE_NOT_FOUND,
            HttpStatus.TOO_MANY_REQUESTS, CommonErrorCode.TOO_MANY_REQUESTS,
            HttpStatus.SERVICE_UNAVAILABLE, CommonErrorCode.SERVICE_UNAVAILABLE);

    private final ErrorStatusResolver statusResolver;

    public GlobalExceptionHandler(ErrorStatusResolver statusResolver) {
        this.statusResolver = statusResolver;
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ProblemDetail> handleApplicationException(ApplicationException ex) {
        ErrorCode errorCode = ex.errorCode();
        HttpStatus status = statusResolver.resolve(errorCode);
        LOGGER.info("業務のエラーを返します。code={}", errorCode.code());
        return ResponseEntity.status(status).body(problem(status, errorCode, ex.getMessage()));
    }

    @ExceptionHandler(InvalidIdException.class)
    public ResponseEntity<ProblemDetail> handleInvalidId(InvalidIdException ex) {
        ErrorCode errorCode = CommonErrorCode.RESOURCE_NOT_FOUND;
        HttpStatus status = statusResolver.resolve(errorCode);
        LOGGER.info("IDの形式が正しくないため、対象なしとして扱います。{}", ex.getMessage());
        return ResponseEntity.status(status).body(problem(status, errorCode, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        ErrorCode errorCode = CommonErrorCode.INTERNAL_ERROR;
        HttpStatus status = statusResolver.resolve(errorCode);
        LOGGER.error("想定外のエラーが発生しました。", ex);
        return ResponseEntity.status(status).body(problem(status, errorCode, null));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorCode errorCode = CommonErrorCode.VALIDATION_ERROR;
        HttpStatus validationStatus = statusResolver.resolve(errorCode);
        ProblemDetail body = problem(validationStatus, errorCode, "入力内容を確認してください。");
        body.setProperty("errors", fieldErrors(ex));
        return handleExceptionInternal(ex, body, headers, validationStatus, request);
    }

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

    private static boolean hasErrorCode(Object body) {
        return body instanceof ProblemDetail detail
                && detail.getProperties() != null
                && detail.getProperties().containsKey(CODE);
    }

    private static ErrorCode commonCodeOf(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        CommonErrorCode errorCode = known == null ? null : CODES_BY_STATUS.get(known);
        if (errorCode != null) {
            return errorCode;
        }
        return status.is4xxClientError() ? CommonErrorCode.INVALID_REQUEST : CommonErrorCode.INTERNAL_ERROR;
    }

    // 入力された値（個人の情報を含みうる）は返さず、項目・コード・メッセージだけを返す。
    private static List<FieldErrorItem> fieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toItem)
                .toList();
    }

    private static FieldErrorItem toItem(FieldError error) {
        String constraint = Objects.requireNonNullElse(error.getCode(), "Invalid");
        String code = constraint.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        return new FieldErrorItem(error.getField(), code, Objects.requireNonNullElse(error.getDefaultMessage(), ""));
    }

    public record FieldErrorItem(String field, String code, String message) {}
}
