package com.picktory.common.error.handler;

import com.picktory.common.BaseResponse;
import com.picktory.common.BaseResponseStatus;
import com.picktory.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 400 Bad Request - 잘못된 JSON 요청 처리 (Enum 값 오류 포함)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<?>> handleJsonParseException(HttpMessageNotReadableException ex) {
        log.warn("JSON 파싱 오류 발생: {}", ex.getMessage());

        if (ex.getMessage().contains("com.picktory.domain.bundle.enums.DesignType")) {
            return ResponseEntity.badRequest().body(new BaseResponse<>(BaseResponseStatus.INVALID_DESIGN_TYPE));
        }

        return ResponseEntity.badRequest().body(new BaseResponse<>(BaseResponseStatus.INVALID_JSON_REQUEST));
    }

    /**
     * 400 Bad Request - 유효성 검증 실패 (Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("유효성 검증 오류 발생: {}", ex.getMessage());

        // 모든 검증 오류 메시지를 리스트로 수집
        List<String> errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage()) // 검증 실패 메시지 추출
                .toList();

        // 첫 번째 오류 메시지를 응답으로 반환
        String errorMessage = errorMessages.isEmpty() ? "유효성 검증 오류가 발생했습니다." : errorMessages.get(0);

        return ResponseEntity.badRequest().body(new BaseResponse<>(false, HttpStatus.BAD_REQUEST.value(), errorMessage));
    }

    /**
     * 500 Internal Server Error - 알 수 없는 서버 오류
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleGenericException(Exception ex) {
        log.error("서버 오류 발생: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BaseResponse<>(BaseResponseStatus.INTERNAL_SERVER_ERROR));
    }


    /**
     * BaseException (커스텀 예외) 처리
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<?>> handleBaseException(BaseException e) {
        log.warn("BaseException 발생: {}", e.getMessage());
        return ResponseEntity
                .status(e.getStatus().getCode())  // 예외에 지정된 HTTP 상태 코드 반환
                .body(new BaseResponse<>(e.getStatus()));
    }
}
