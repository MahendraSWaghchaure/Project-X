package com.app.exception;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {
		
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String,String>> handleValidationException(MethodArgumentNotValidException ex){
		Map<String,String> errors =  ex.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(
						fieldError -> fieldError.getField(),
						fieldError -> fieldError.getDefaultMessage()
						
					));
		
		
		return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
	}
}
