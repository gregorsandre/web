package tech.kood.backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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
    List<UUID> connections(@AuthenticationPrincipal UUID me) {
        return service.getConnections(me);
    }

    @GetMapping("/connections/requests") //list?
    List<UUID> requests(@AuthenticationPrincipal UUID me) {
        return service.getRequests(me);
    }

    @PostMapping("/connections/{id}") //send a request
    @ResponseStatus(HttpStatus.CREATED)
    void sendRequest(@AuthenticationPrincipal UUID me, @PathVariable("id") UUID id) {
        service.sendRequest(me, id);
    }

    @PutMapping("/connections/{id}") //accept a request
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void acceptRequest(@PathVariable("id") UUID id, @AuthenticationPrincipal UUID me) {
        service.acceptRequest(id, me);
    }

    @DeleteMapping("/connections/{id}") // disconnect
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable("id") UUID id, @AuthenticationPrincipal UUID me) {
        service.deleteConnection(id, me);
    }
}
