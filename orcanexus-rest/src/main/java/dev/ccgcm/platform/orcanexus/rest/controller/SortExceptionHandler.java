package dev.ccgcm.platform.orcanexus.rest.controller;

import dev.ccgcm.platform.orcanexus.domain.model.InvalidSortFieldException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates sort-field rejections raised by the persistence adapter into HTTP 400 responses. */
@RestControllerAdvice
public class SortExceptionHandler {

  @ExceptionHandler(InvalidSortFieldException.class)
  public ProblemDetail handleInvalidSortField(InvalidSortFieldException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
  }
}
