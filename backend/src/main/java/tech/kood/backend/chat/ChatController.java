package tech.kood.backend.chat;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tech.kood.backend.auth.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/chats")
public class ChatController {
    private final ChatService service;

    ChatController(ChatService service) {
        this.service = service;
    }

    @GetMapping
    List<ChatSummary> list(@AuthenticationPrincipal User me) {
        return service.listChats(me.id());
    }

    @GetMapping("/{id}")
    ChatSummary chat(@AuthenticationPrincipal User me, @PathVariable("id") long id) {
        return service.getChat(me.id(), id);
    }

    // Opens the one chat with a connected user, creating it the first time
    @PostMapping("/with/{userId}")
    Map<String, Long> open(@AuthenticationPrincipal User me, @PathVariable("userId") UUID userId) {
        return Map.of("id", service.openChat(me.id(), userId));
    }

    @GetMapping("/{id}/messages")
    List<Message> messages(@AuthenticationPrincipal User me, @PathVariable("id") long id,
                           @RequestParam(name = "before", required = false) Long before,
                           @RequestParam(name = "limit", required = false) Integer limit) {
        return service.messages(me.id(), id, before, limit);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    Message send(@AuthenticationPrincipal User me, @PathVariable("id") long id, @RequestBody SendMessageRequest request) {
        return service.send(me.id(), id, request.body());
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void read(@AuthenticationPrincipal UUID me, @PathVariable("id") long id) {
        service.markRead(me, id);
    }
}
