package com.library.management.controller;

import com.library.management.exception.BusinessRuleException;
import com.library.management.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        log.warn("Resource not found: {}", ex.getMessage());
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorTitle", "Resource Not Found");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(BusinessRuleException.class)
    public String handleBusinessRuleException(BusinessRuleException ex, Model model) {
        log.warn("Business rule violation: {}", ex.getMessage());
        model.addAttribute("errorCode", "400");
        model.addAttribute("errorTitle", "Business Rule Violation");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        log.error("Unhandled application exception", ex);
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorTitle", "Unexpected Application Error");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred. Please try again.");
        return "error";
    }
}
