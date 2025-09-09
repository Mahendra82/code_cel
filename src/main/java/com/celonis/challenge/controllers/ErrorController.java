package com.celonis.challenge.controllers;

import com.celonis.challenge.exceptions.NotAuthorizedException;
import com.celonis.challenge.exceptions.OverloadedException;
import com.celonis.challenge.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public java.util.Map<String, Object> handleNotFound() {
        logger.warn("Entity not found");
        return java.util.Map.of("error", "Not found");
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(NotAuthorizedException.class)
    public java.util.Map<String, Object> handleNotAuthorized() {
        logger.warn("Not authorized");
        return java.util.Map.of("error", "Not authorized");
    }

    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(OverloadedException.class)
    public java.util.Map<String, Object> handleOverloaded(OverloadedException e) {
        logger.warn("Overloaded: {}", e.getMessage());
        return java.util.Map.of("error", e.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public java.util.Map<String, Object> handleInternalError(Exception e) {
        logger.error("Unhandled Exception in Controller", e);
        return java.util.Map.of("error", "Internal error");
    }

}
