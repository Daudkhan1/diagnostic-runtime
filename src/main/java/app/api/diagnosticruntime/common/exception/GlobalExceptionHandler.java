package app.api.diagnosticruntime.common.exception;

import app.api.diagnosticruntime.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Handle IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return new  ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return new ResponseEntity<>(ApiResponse.error("Invalid username or password"), HttpStatus.UNAUTHORIZED);
    }

    // Handle other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        HttpStatus status = extractHttpStatus(ex);
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), status);
    }

    private HttpStatus extractHttpStatus(Exception ex) {
        try {
            Method method = ex.getClass().getMethod("getStatusCode");
            Object result = method.invoke(ex);

            if (result instanceof HttpStatus) {
                return (HttpStatus) result;
            } else if (result instanceof Integer) {
                return HttpStatus.valueOf((Integer) result);
            } else if (result != null) {
                return HttpStatus.valueOf(Integer.parseInt(result.toString()));
            }
        } catch (Exception ignored) {
            // Method not found or invocation failed, fall through to default
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}

