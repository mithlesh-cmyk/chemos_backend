package chemos.chem_os.exception;

import chemos.chem_os.CompnayAlreadyExistsException;
import chemos.chem_os.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CompnayAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleCompanyAlreadyExists(CompnayAlreadyExistsException ex) {
        ApiErrorResponse errorBody = new ApiErrorResponse(
                "Resource Conflict",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(errorBody, HttpStatus.CONFLICT);
    }
}
