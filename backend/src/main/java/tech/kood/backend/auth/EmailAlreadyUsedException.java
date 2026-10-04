package tech.kood.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Email already registered")
public class EmailAlreadyUsedException extends RuntimeException {};