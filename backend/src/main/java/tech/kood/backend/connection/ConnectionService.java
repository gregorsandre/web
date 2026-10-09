package tech.kood.backend.connection;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.kood.backend.NotFoundException;
import tech.kood.backend.access.AccessService;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConnectionService {
    private final ConnectionRepository repository;
    private final AccessService accessService;

    ConnectionService(ConnectionRepository connectionRep, AccessService accessService) {
        this.repository = connectionRep;
        this.accessService = accessService;
    }

    public Optional<Connection> getConnection(UUID requesterId, UUID addresseeId) {
        return repository.findBetween(requesterId, addresseeId);
    }

    public List<UUID> getConnections(UUID id) {
        return repository.findConnectedIds(id);
    }

    public List<UUID> getRequests(UUID id) {
        return repository.findRequests(id);
    }

    @Transactional
    public void sendRequest(UUID myId, UUID addresseeId) {
        boolean target = accessService.canView(myId, addresseeId);
        if (target == false) {
            throw new NotFoundException("Not allowed for " + myId + " to see " + addresseeId);
        } else {
            Optional<Connection> existingRequests = repository.findBetween(myId, addresseeId);
            if (addresseeId.equals(myId)) {
                throw new NotFoundException("Cannot request yourself");
            } else if (existingRequests.isPresent()) {
                throw new NotFoundException("Request or connection already exists");
            } else {
                repository.newRequest(myId, addresseeId);
            }
        }
    }

    public void acceptRequest(UUID requesterId, UUID myId) {
        int request = repository.acceptRequest(requesterId, myId);
        if (request == 0) {
            throw new NotFoundException("No pending request from " + requesterId + " and " + myId);
        }
    }

    public void deleteConnection(UUID userA, UUID userB) {
        int deletion = repository.deleteConnection(userA, userB);
        if (deletion == 0) {
            throw new NotFoundException("No connection between " + userA + " and " + userB);
        }
    }
}
