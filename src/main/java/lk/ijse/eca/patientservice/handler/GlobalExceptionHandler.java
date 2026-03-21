package lk.ijse.eca.patientservice.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lk.ijse.eca.patientservice.exception.DuplicatePatientException;
import lk.ijse.eca.patientservice.exception.FileOperationException;
import lk.ijse.eca.patientservice.exception.PatientNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.rmi.StubNotFoundException;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@Order(-1)
public class GlobalExceptionHandler {

    @ExceptionHandler(StubNotFoundException.class)
    public ResponseEntity<ProblemDetail> handlePatientNotFound(
            PatientNotFoundException ex, HttpServletRequest request) {
        log.error("Patient not found: {}", ex.getMessage());
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.NOT_FOUND,
                "Patient not found",
                ex.getMessage(),
                request.getRequestURI());
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(DuplicatePatientException.class)
    public ResponseEntity<ProblemDetail> handleDuplicatePatient(
            DuplicatePatientException ex, HttpServletRequest request){
        log.warn("Duplicate patient: {}", ex.getMessage());
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.CONFLICT,
                "Duplicate patient",
                ex.getMessage(),
                request.getRequestURI());
        return problemResponse(HttpStatus.CONFLICT, problem);
    }

    @ExceptionHandler(FileOperationException.class)
    public ResponseEntity<ProblemDetail> handleFileOperation(
            FileOperationException ex, HttpServletRequest request){
        log.error("File operation failed: {}", ex.getMessage(),ex);
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "File operation Error",
                ex.getMessage(),
                request.getRequestURI());
        return problemResponse(HttpStatus.INTERNAL_SERVER_ERROR, problem);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleBindException(
            BindException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed for request [{}]: {}", request.getRequestURI(), detail);

        ProblemDetail problem = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                detail,
                request.getRequestURI());

        ex.getBindingResult().getFieldErrors().forEach(fe ->
                problem.setProperty(fe.getField(), fe.getDefaultMessage()));

        return problemResponse(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File Too Large",
                "The uploaded picture exceeds the maximum allowed size of 5 MB.",
                request.getRequestURI());
        return problemResponse(HttpStatus.PAYLOAD_TOO_LARGE, problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        String detail = ex.getConstraintViolations()
                .stream()
                .map(cv -> {
                    String path = cv.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return field + ": " + cv.getMessage();
                })
                .collect(Collectors.joining("; "));

        log.warn("Constraint violation for [{}]: {}", request.getRequestURI(), detail);

        ProblemDetail problem = buildProblemDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, "Validation Error", detail, request.getRequestURI());

        ex.getConstraintViolations().forEach(cv -> {
            String path = cv.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            problem.setProperty(field, cv.getMessage());
        });

        return problemResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        ProblemDetail problem = buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI());
        return problemResponse(HttpStatus.INTERNAL_SERVER_ERROR, problem);
    }

    // private helpers
    private ProblemDetail buildProblemDetail(
            HttpStatus status, String title, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(instance));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    private ResponseEntity<ProblemDetail> problemResponse(HttpStatus status, ProblemDetail problem){
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
