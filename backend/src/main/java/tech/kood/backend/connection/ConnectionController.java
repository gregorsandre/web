package tech.kood.backend.connection;

import org.springframework.web.bind.annotation.*;

@RestController
public class ConnectionController {
    private final ConnectionService service;

    ConnectionController(ConnectionService service) {
        this.service = service;
    }

    @GetMapping("/connections") //list?

    @GetMapping("/connections/requests") //list?

    @PostMapping("/connections/{id}") //send a request

    @PutMapping("/connections/{id}") //accept a request

    @DeleteMapping("/connections/{id}") // disconnect
}
