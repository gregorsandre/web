package tech.kood.backend.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// The email is the login, so it stays out of /me and /users (no authentication data
// there) and is only handed to its owner here
@RestController
public class AccountController {

    record Account(String email) {}

    @GetMapping("/account")
    Account account(@AuthenticationPrincipal User me) {
        return new Account(me.email());
    }
}
