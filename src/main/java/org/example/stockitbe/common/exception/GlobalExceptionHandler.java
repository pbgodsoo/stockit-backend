package org.example.stockitbe.common.exception;

import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.common.model.BaseResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity handleValidationException(MethodArgumentNotValidException e){
        Map<String, String> errors = new HashMap<>();
        for(FieldError error : e.getBindingResult().getFieldErrors()){
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BaseResponse.fail(BaseResponseStatus.REQUEST_ERROR, errors));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity handleException(BaseException e){
        BaseResponseStatus status = e.getStatus();
        int errorCode = status.getCode();
        int statusCode = statusCodeMapper(errorCode);
        BaseResponse response = BaseResponse.fail(status);

        return ResponseEntity.status(statusCode).body(response);
    }

    private int statusCodeMapper(int errorCode){
        if (errorCode >= 5000) {
            return 500;
        }
        if (errorCode >= 4000) {
            return 400;
        }
        if (errorCode >= 3000) {
            return 401;
        }
        return 400;
    }
}
