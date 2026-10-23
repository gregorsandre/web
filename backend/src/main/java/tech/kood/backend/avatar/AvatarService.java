package tech.kood.backend.avatar;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
class AvatarService {
    static final String WRONG_TYPE = "Use a JPEG, PNG or WebP image";
    static final String TOO_LARGE = "Picture must be 2 MB or smaller";

    private final AvatarRepository avatars;

    AvatarService(AvatarRepository avatars) {
        this.avatars = avatars;
    }

    @Transactional
    void upload(UUID userId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, WRONG_TYPE);
        }
        // Spring's multipart limit catches this first on a real server; checked here too
        if (file.getSize() > AvatarImages.MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, TOO_LARGE);
        }
        byte[] data = file.getBytes();
        String type = AvatarImages.detectType(data)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, WRONG_TYPE));
        avatars.replace(userId, type, data);
    }

    @Transactional
    void remove(UUID userId) {
        avatars.remove(userId);
    }

    Optional<AvatarRepository.StoredAvatar> find(String rawId) {
        try {
            return avatars.find(UUID.fromString(rawId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
