package com.example.board.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * 모든 에러 응답을 ProblemDetail(RFC 9457)로 통일한다.
 * 프레임워크 기본 예외는 ResponseEntityExceptionHandler가 ProblemDetail로 변환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Business exception: code={}, message={}", errorCode.name(), e.getMessage());
        return toResponse(errorCode, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception e) {
        log.error("Unexpected exception", e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        ProblemDetail problemDetail = toProblemDetail(errorCode, errorCode.getMessage());
        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldErrorDetail::from)
                .toList();
        problemDetail.setProperty("errors", errors);
        return ResponseEntity.status(errorCode.getStatus()).body(problemDetail);
    }

    private ResponseEntity<ProblemDetail> toResponse(ErrorCode errorCode, String detail) {
        return ResponseEntity.status(errorCode.getStatus()).body(toProblemDetail(errorCode, detail));
    }

    private ProblemDetail toProblemDetail(ErrorCode errorCode, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), detail);
        problemDetail.setProperty("code", errorCode.name());
        return problemDetail;
    }

    // 입력값(rejectedValue)은 비밀번호 등 민감 정보일 수 있으므로 응답에 포함하지 않는다.
    record FieldErrorDetail(String field, String message) {

        static FieldErrorDetail from(FieldError fieldError) {
            return new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
        }
    }
}
