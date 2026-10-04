package tech.kood.backend.auth;

import tech.kood.backend.user.User;
import tech.kood.backend.user.UserRepository;
import tech.kood.backend.auth.dto.AuthResponse;
import tech.kood.backend.auth.dto.LoginRequest;
import tech.kood.backend.auth.dto.RegisterRequest;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {
    private final String dummyHash;
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, JdbcTemplate jdbc, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.jwt = jwt;
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional // if the profile insert fails, the user insert is also rolled back
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim();
        String hash = encoder.encode(req.password());

        UUID id;
        try {
            id = users.create(email, hash);
        } catch (DuplicateKeyException e) {
            throw new EmailAlreadyUsedException();
        }
        jdbc.update("INSERT INTO profiles (user_id) VALUES (?)", id);
        return new AuthResponse(jwt.create(id));
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().trim()).orElse(null);
        if (user == null) {
            encoder.matches(req.password(), dummyHash);
            throw new InvalidCredentialsException();
        }
        if (!encoder.matches(req.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AuthResponse(jwt.create(user.id()));
    }
}