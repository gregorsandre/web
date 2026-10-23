package tech.kood.backend.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tech.kood.backend.auth.JwtService;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Runs against the local database; every test is rolled back.
@SpringBootTest(properties = "spring.flyway.validate-on-migrate=false")
@AutoConfigureMockMvc
@Transactional
class BioEndpointTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;

    UUID alice;
    UUID bob;

    @BeforeEach
    void setUp() {
        alice = userWithBio();
        bob = userWithBio();
        jdbc.update("""
                INSERT INTO connections (requester_id, addressee_id, status, responded_at)
                VALUES (?, ?, 'accepted', now())""", alice, bob);
    }

    @Test
    void meBioIsTheSameAsMyUsersBio() throws Exception {
        String viaMe = bio("/me/bio", alice);
        String viaId = bio("/users/" + alice + "/bio", alice);
        assertEquals(viaMe, viaId);
    }

    @Test
    void ownBioHasPreferencesLocationAndSports() throws Exception {
        mvc.perform(get("/me/bio").header("Authorization", auth(alice)))
                .andExpect(jsonPath("$.userId").value(alice.toString()))
                .andExpect(jsonPath("$.ageMin").value(25))
                .andExpect(jsonPath("$.lat").value(59.4))
                .andExpect(jsonPath("$.maxRadiusKm").value(30))
                .andExpect(jsonPath("$.tags", hasSize(1)));
    }

    @Test
    void othersSeeSportsButNotLocationOrPreferences() throws Exception {
        mvc.perform(get("/users/{id}/bio", alice).header("Authorization", auth(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(alice.toString()))
                .andExpect(jsonPath("$.tags", hasSize(1)))
                .andExpect(jsonPath("$.lat").doesNotExist())
                .andExpect(jsonPath("$.lng").doesNotExist())
                .andExpect(jsonPath("$.ageMin").doesNotExist())
                .andExpect(jsonPath("$.maxRadiusKm").doesNotExist());
    }

    private String bio(String path, UUID me) throws Exception {
        return mvc.perform(get(path).header("Authorization", auth(me)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private UUID userWithBio() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, password_hash) VALUES (?, ?, 'x')", id, id + "@test.local");
        jdbc.update("INSERT INTO profiles (user_id, display_name, completed) VALUES (?, 'Test', true)", id);
        jdbc.update("""
                INSERT INTO bios (user_id, skill_level, goal, looking_for, env_pref, age, age_min, age_max,
                                  lat, lng, max_radius_km, availability)
                VALUES (?, 3, 'fun', 'regular_partner', 'outdoor', 30, 25, 35, 59.4, 24.7, 30, ARRAY['weekend'])""", id);
        jdbc.update("INSERT INTO user_tags (user_id, tag_id) SELECT ?, id FROM tags ORDER BY id LIMIT 1", id);
        return id;
    }

    private String auth(UUID userId) {
        return "Bearer " + jwt.create(userId);
    }
}
