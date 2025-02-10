package com.picktory.common.exception;

import com.picktory.common.BaseResponseStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BaseExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        return ResponseEntity
                .status(e.getStatus().getCode())
                .body(new ErrorResponse(e.getStatus().getCode(), e.getStatus().getMessage()));
    }

    public static class ErrorResponse {
        private final int code;
        private final String message;

        public ErrorResponse(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
