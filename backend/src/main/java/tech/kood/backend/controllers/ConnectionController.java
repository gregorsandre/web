package tech.kood.backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tech.kood.backend.auth.User;
import tech.kood.backend.services.ConnectionService;

import java.util.List;
import java.util.UUID;

@RestController
public class ConnectionController {
    private final ConnectionService service;

    ConnectionController(ConnectionService service) {
        this.service = service;
    }

    @GetMapping("/connections") //list?
    List<UUID> connections(@AuthenticationPrincipal User me) {
        return service.getConnections(me.id());
    }

    @GetMapping("/connections/requests") //list?
    List<UUID> requests(@AuthenticationPrincipal User me) {
        return service.getRequests(me.id());
    }

    @PostMapping("/connections/{id}") //send a request
    @ResponseStatus(HttpStatus.CREATED)
    void sendRequest(@AuthenticationPrincipal User me, @PathVariable("id") UUID id) {
        service.sendRequest(me.id(), id);
    }

    @PutMapping("/connections/{id}") //accept a request
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void acceptRequest(@PathVariable("id") UUID id, @AuthenticationPrincipal User me) {
        service.acceptRequest(id, me.id());
    }

//    @DeleteMapping("/connections/requests/{id}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    void dismiss(@PathVariable("id") UUID id, @AuthenticationPrincipal User me) {
//        service.dismissRecommendation(me.id(), id);
//    }

    @DeleteMapping("/connections/{id}") // disconnect
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable("id") UUID id, @AuthenticationPrincipal User me) {
        service.deleteConnection(id, me.id());
    }
}
