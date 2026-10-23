package tech.kood.backend.avatar;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tech.kood.backend.auth.JwtService;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Runs against the local database; every test is rolled back.
@SpringBootTest(properties = "spring.flyway.validate-on-migrate=false")
@AutoConfigureMockMvc
@Transactional
class AvatarControllerTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 5, 6, 7};

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;

    UUID me;

    @BeforeEach
    void setUp() {
        me = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, password_hash) VALUES (?, ?, 'x')", me, me + "@test.local");
        jdbc.update("INSERT INTO profiles (user_id, display_name) VALUES (?, 'Me')", me);
    }

    @Test
    void uploadedPictureIsServedWithoutLogin() throws Exception {
        String link = avatarOf(upload(PNG, "me.png", "image/png")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(me.toString()))
                .andExpect(jsonPath("$.avatar", startsWith("/avatars/"))));

        mvc.perform(get("/me").header("Authorization", auth()))
                .andExpect(jsonPath("$.avatar").value(link));
        mvc.perform(get(link))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PNG));
    }

    @Test
    void typeComesFromTheBytesNotTheName() throws Exception {
        String link = avatarOf(upload(JPEG, "looks-like.png", "image/png").andExpect(status().isOk()));
        mvc.perform(get(link)).andExpect(header().string("Content-Type", "image/jpeg"));
    }

    @Test
    void replacingGivesANewLinkAndKillsTheOldOne() throws Exception {
        String first = avatarOf(upload(PNG, "a.png", "image/png"));
        String second = avatarOf(upload(JPEG, "b.jpg", "image/jpeg"));

        assertNotEquals(first, second);
        mvc.perform(get(first)).andExpect(status().isNotFound());
        mvc.perform(get(second)).andExpect(status().isOk());
    }

    @Test
    void removingClearsThePicture() throws Exception {
        String link = avatarOf(upload(PNG, "a.png", "image/png"));

        mvc.perform(delete("/me/avatar").header("Authorization", auth()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/me").header("Authorization", auth()))
                .andExpect(jsonPath("$.avatar").doesNotExist());
        mvc.perform(get(link)).andExpect(status().isNotFound());
        // removing again is fine
        mvc.perform(delete("/me/avatar").header("Authorization", auth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonImagesAreRefused() throws Exception {
        upload("just text".getBytes(StandardCharsets.UTF_8), "fake.jpg", "image/jpeg").andExpect(status().isBadRequest());
        upload("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8), "x.svg", "image/svg+xml")
                .andExpect(status().isBadRequest());
        upload(new byte[0], "empty.png", "image/png").andExpect(status().isBadRequest());
    }

    @Test
    void tooLargeIsRefused() throws Exception {
        byte[] big = Arrays.copyOf(PNG, (int) AvatarImages.MAX_BYTES + 1);
        upload(big, "big.png", "image/png").andExpect(status().isContentTooLarge());
    }

    @Test
    void changingNeedsALogin() throws Exception {
        mvc.perform(multipart(HttpMethod.PUT, "/me/avatar").file(new MockMultipartFile("file", "a.png", "image/png", PNG)))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/me/avatar")).andExpect(status().isUnauthorized());
    }

    @Test
    void unknownLinksAre404() throws Exception {
        mvc.perform(get("/avatars/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/avatars/not-a-uuid")).andExpect(status().isNotFound());
    }

    private ResultActions upload(byte[] data, String name, String type) throws Exception {
        return mvc.perform(multipart(HttpMethod.PUT, "/me/avatar")
                .file(new MockMultipartFile("file", name, type, data))
                .header("Authorization", auth()));
    }

    private String avatarOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.avatar");
    }

    private String auth() {
        return "Bearer " + jwt.create(me);
    }
}
