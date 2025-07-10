package com.proptech.realestate.exception;

import com.proptech.realestate.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private MessageSource messageSource;

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        String message = messageSource.getMessage("error.resource.not_found", 
            new Object[]{ex.getResourceName(), ex.getFieldName(), ex.getFieldValue()}, 
            request.getLocale());
        
        return ApiResponse.error(message, "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleInvalidRequestException(InvalidRequestException ex, WebRequest request) {
        String message = messageSource.getMessage("error.bad_request", null, request.getLocale());
        
        return ApiResponse.error(message, "INVALID_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleGlobalException(Exception ex, WebRequest request) {
        String message = messageSource.getMessage("error.internal_server", null, request.getLocale());
        
        return ApiResponse.error(message, "INTERNAL_SERVER_ERROR", ex.getMessage());
    }
}