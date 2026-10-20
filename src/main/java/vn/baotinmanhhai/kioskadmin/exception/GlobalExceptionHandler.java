package vn.baotinmanhhai.kioskadmin.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.baotinmanhhai.kioskadmin.dto.ApiResponse;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getHttpStatus()).body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    // Truoc day loi validate (@Valid) roi xuong handler generic ben duoi va tra ve HTTP 500 -
    // la bug: validate sai thi phai la 400, khong phai 500.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Du lieu khong hop le";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("400", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception e) {
        // QUAN TRONG: KHONG tra chi tiet exception (e.getMessage()) ve cho client - co the lo
        // thong tin noi bo (SQL, stack trace rut gon...). Chi log day du o server, tra message
        // chung cho client.
        log.error("Unhandled error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("500", "Loi he thong, vui long thu lai sau"));
    }
}
