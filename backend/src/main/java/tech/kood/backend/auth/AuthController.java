package tech.kood.backend.auth;

import tech.kood.backend.auth.dto.AuthResponse;
import tech.kood.backend.auth.dto.LoginRequest;
import tech.kood.backend.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService service;

    AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/auth/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return new ResponseEntity<>(service.register(req), HttpStatus.CREATED);
    }

    @PostMapping("/auth/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return new ResponseEntity<>(service.login(req), HttpStatus.OK);
    }
}