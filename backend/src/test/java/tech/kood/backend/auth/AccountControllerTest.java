package tech.kood.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Runs against the local database; every test is rolled back.
@SpringBootTest(properties = "spring.flyway.validate-on-migrate=false")
@AutoConfigureMockMvc
@Transactional
class AccountControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;

    @Test
    void ownerGetsTheirEmailAndNothingElse() throws Exception {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, password_hash) VALUES (?, ?, 'x')", id, "owner@test.local");

        mvc.perform(get("/account").header("Authorization", "Bearer " + jwt.create(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@test.local"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/account")).andExpect(status().isUnauthorized());
    }
}
