package com.aivanouski.ttstorage.error.handler;

import com.aivanouski.ttstorage.error.BaseRestException;
import com.aivanouski.ttstorage.error.ParseException;
import com.aivanouski.ttstorage.error.model.ErrorCodes;
import com.aivanouski.ttstorage.error.model.ErrorModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(Exception.class)
    public void defaultExceptionHandler(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception exception) throws IOException {
        HttpStatus httpStatus = resolveResponseStatus(exception);
        String errorCode = resolveCustomResponseCode(exception);

        if (exception instanceof BindException) {
            exception = buildPrettyMessageForFieldValidation((BindException) exception);
        }

        ErrorModel attributeValue = new ErrorModel(
                Instant.now().toString(),
                errorCode,
                httpStatus.value(),
                request.getServletPath().isEmpty() ? request.getPathInfo() : request.getServletPath(),
                exception.getClass().getSimpleName(),
                exception.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(httpStatus.value());
        response.getWriter().write(new ObjectMapper().writeValueAsString(attributeValue));
        response.getWriter().close();
    }

    public ValidationException buildPrettyMessageForFieldValidation(BindException exception) {
        Map<String, String> errors = new HashMap<>();

        for (org.springframework.validation.ObjectError objectError : exception.getBindingResult().getAllErrors()) {
            FieldError fieldError = (FieldError) objectError;
            String fieldName = fieldError.getField();
            String errorMessage = fieldError.getDefaultMessage();

            errors.put(fieldName, errorMessage);
        }

        String errorJson;
        try {
            errorJson = new ObjectMapper().writeValueAsString(errors);
        } catch (IOException e) {
            log.error("Failed to serialize validation errors to JSON", e);
            throw new ParseException(ErrorCodes.PARSE_VALIDATION_ERROR,
                    "error occurred when serialize validation errors", e);
        }

        return new ValidationException(errorJson);
    }

    private HttpStatus resolveResponseStatus(Exception e) {
        ResponseStatus annotation = AnnotatedElementUtils.findMergedAnnotation(e.getClass(), ResponseStatus.class);
        return annotation != null
                ? annotation.code()
                : getCustomResponseStatus(e);
    }

    private HttpStatus getCustomResponseStatus(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException) {
            return HttpStatus.BAD_REQUEST;
        } else if (exception instanceof MethodArgumentTypeMismatchException) {
            return HttpStatus.BAD_REQUEST;
        } else if (exception instanceof ValidationException) {
            return HttpStatus.BAD_REQUEST;
        } else if (exception instanceof MissingServletRequestPartException) {
            return HttpStatus.BAD_REQUEST;
        } else if (exception instanceof HttpMessageNotReadableException) {
            return HttpStatus.BAD_REQUEST;
        } else if (exception instanceof jakarta.validation.ConstraintViolationException) {
            return HttpStatus.BAD_REQUEST;
        } else if (exception instanceof MissingServletRequestParameterException) {
            return HttpStatus.BAD_REQUEST;
        } else if (exception instanceof MultipartException) {
            return HttpStatus.BAD_REQUEST;
        } else if (exception instanceof java.nio.file.AccessDeniedException) {
            return HttpStatus.FORBIDDEN;
        } else if (exception instanceof HttpClientErrorException.NotFound) {
            return HttpStatus.NOT_FOUND;
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    private String resolveCustomResponseCode(Exception exception) {
        if (exception instanceof BaseRestException) {
            return ((BaseRestException) exception).getCode();
        } else if (exception instanceof MissingServletRequestPartException) {
            return ErrorCodes.FILE_ABSENT_ERROR;
        } else if (exception instanceof HttpMessageNotReadableException) {
            return ErrorCodes.INVALID_JSON_ERROR;
        } else if (exception instanceof jakarta.validation.ConstraintViolationException) {
            return ErrorCodes.VALIDATION_FAILED_ERROR;
        } else if (exception instanceof MissingServletRequestParameterException) {
            return ErrorCodes.REQUEST_PARAM_ABSENT_ERROR;
        } else if (exception instanceof MultipartException) {
            return ErrorCodes.MULTIPART_PARSING_ERROR;
        } else {
            return ErrorCodes.INTERNAL_SERVER_ERROR;
        }
    }
}