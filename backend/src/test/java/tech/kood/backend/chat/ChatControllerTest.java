package tech.kood.backend.chat;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tech.kood.backend.auth.JwtService;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Runs against the local database; every test is rolled back.
@SpringBootTest(properties = "spring.flyway.validate-on-migrate=false")
@AutoConfigureMockMvc
@Transactional
class ChatControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;

    UUID alice;
    UUID bob;
    UUID carol;

    @BeforeEach
    void setUp() {
        alice = user();
        bob = user();
        carol = user();
        jdbc.update("""
                INSERT INTO connections (requester_id, addressee_id, status, responded_at)
                VALUES (?, ?, 'accepted', now())""", alice, bob);
    }

    @Test
    void bothSidesGetTheSameChat() throws Exception {
        assertEquals(openChat(alice, bob), openChat(bob, alice));
    }

    @Test
    void strangersGet404Everywhere() throws Exception {
        long chat = openChat(alice, bob);
        mvc.perform(post("/chats/with/{id}", alice).header("Authorization", auth(carol)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/chats/{id}", chat).header("Authorization", auth(carol)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/chats/{id}/messages", chat).header("Authorization", auth(carol)))
                .andExpect(status().isNotFound());
        send(carol, chat, "hi").andExpect(status().isNotFound());
        mvc.perform(post("/chats/{id}/read", chat).header("Authorization", auth(carol)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/chats/{id}", 999_999_999).header("Authorization", auth(alice)))
                .andExpect(status().isNotFound());
    }

    @Test
    void sentMessageIsUnreadForTheOtherUserOnly() throws Exception {
        long chat = openChat(alice, bob);
        send(alice, chat, "  hi bob  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("hi bob"))
                .andExpect(jsonPath("$.senderId").value(alice.toString()))
                .andExpect(jsonPath("$.createdAt").isString());
        mvc.perform(get("/chats").header("Authorization", auth(bob)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(chat))
                .andExpect(jsonPath("$[0].userId").value(alice.toString()))
                .andExpect(jsonPath("$[0].unread").value(1))
                .andExpect(jsonPath("$[0].lastMessageAt").isString());
        mvc.perform(get("/chats").header("Authorization", auth(alice)))
                .andExpect(jsonPath("$[0].unread").value(0));
    }

    @Test
    void chatsWithoutMessagesAreNotListedButCanBeOpened() throws Exception {
        long chat = openChat(alice, bob);
        mvc.perform(get("/chats").header("Authorization", auth(alice)))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/chats/{id}", chat).header("Authorization", auth(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(bob.toString()));
    }

    @Test
    void invalidBodiesAreRejected() throws Exception {
        long chat = openChat(alice, bob);
        send(alice, chat, "   ").andExpect(status().isBadRequest());
        send(alice, chat, "a".repeat(2001)).andExpect(status().isBadRequest());
        send(alice, chat, "nul\\u0000char").andExpect(status().isBadRequest());
        send(alice, chat, "a".repeat(2000)).andExpect(status().isCreated());
    }

    @Test
    void historyIsPagedNewestFirst() throws Exception {
        long chat = openChat(alice, bob);
        for (int i = 1; i <= 35; i++) {
            send(i % 2 == 0 ? bob : alice, chat, "m" + i).andExpect(status().isCreated());
        }
        String firstPage = mvc.perform(get("/chats/{id}/messages", chat).header("Authorization", auth(bob)))
                .andExpect(jsonPath("$", hasSize(30)))
                .andExpect(jsonPath("$[0].body").value("m35"))
                .andExpect(jsonPath("$[29].body").value("m6"))
                .andReturn().getResponse().getContentAsString();
        Number oldest = JsonPath.read(firstPage, "$[29].id");
        mvc.perform(get("/chats/{id}/messages", chat).param("before", oldest.toString())
                        .header("Authorization", auth(bob)))
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].body").value("m5"));
        mvc.perform(get("/chats/{id}/messages", chat).param("limit", "10").header("Authorization", auth(bob)))
                .andExpect(jsonPath("$", hasSize(10)));
    }

    @Test
    void markingReadClearsUnread() throws Exception {
        long chat = openChat(alice, bob);
        send(alice, chat, "hello").andExpect(status().isCreated());
        mvc.perform(post("/chats/{id}/read", chat).header("Authorization", auth(bob)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/chats").header("Authorization", auth(bob)))
                .andExpect(jsonPath("$[0].unread").value(0));
    }

    @Test
    void chatIsHiddenAfterDisconnect() throws Exception {
        long chat = openChat(alice, bob);
        send(alice, chat, "hello").andExpect(status().isCreated());
        jdbc.update("DELETE FROM connections WHERE requester_id = ? AND addressee_id = ?", alice, bob);
        mvc.perform(get("/chats").header("Authorization", auth(alice)))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/chats/{id}/messages", chat).header("Authorization", auth(alice)))
                .andExpect(status().isNotFound());
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, password_hash) VALUES (?, ?, 'x')", id, id + "@test.local");
        return id;
    }

    private String auth(UUID userId) {
        return "Bearer " + jwt.create(userId);
    }

    private long openChat(UUID me, UUID other) throws Exception {
        String json = mvc.perform(post("/chats/with/{id}", other).header("Authorization", auth(me)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    // Bodies used here contain no quotes or backslashes, so plain concatenation is valid JSON
    private ResultActions send(UUID me, long chat, String body) throws Exception {
        return mvc.perform(post("/chats/{id}/messages", chat)
                .header("Authorization", auth(me))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"" + body + "\"}"));
    }
}
