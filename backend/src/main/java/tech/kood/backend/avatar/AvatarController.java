package tech.kood.backend.avatar;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.kood.backend.NotFoundException;
import tech.kood.backend.auth.User;
import tech.kood.backend.services.ProfileBioService;

import java.io.IOException;
import java.time.Duration;

@RestController
public class AvatarController {
    private final AvatarService avatars;
    private final ProfileBioService profiles;

    AvatarController(AvatarService avatars, ProfileBioService profiles) {
        this.avatars = avatars;
        this.profiles = profiles;
    }

    // Answers like /me, so the page can show the new picture straight away
    @PutMapping("/me/avatar")
    ProfileBioService.NameAndPic upload(@AuthenticationPrincipal User me, @RequestParam("file") MultipartFile file) throws IOException {
        avatars.upload(me.id(), file);
        return profiles.getNameAndPic(me.id(), "me").orElseThrow(() -> new NotFoundException("Profile not found"));
    }

    @DeleteMapping("/me/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@AuthenticationPrincipal User me) {
        avatars.remove(me.id());
    }

    // Public on purpose: <img> can't send the token, and the random id is only handed out
    // through /users/{id}, which checks who may see whom. A changed picture gets a new id,
    // so it can be cached for good.
    @GetMapping("/avatars/{id}")
    ResponseEntity<byte[]> picture(@PathVariable("id") String id) {
        return avatars.find(id)
                .map(avatar -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(avatar.contentType()))
                        .header("X-Content-Type-Options", "nosniff")
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .body(avatar.data()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
