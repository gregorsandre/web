package tech.kood.backend.profilebio;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ProfileBioController {

    private final ProfileBioService service;

    ProfileBioController(ProfileBioService service) {
        this.service = service;
    }

    @GetMapping("/users/{id}")
    ResponseEntity<ProfileBioService.NameAndPic> findUser(@PathVariable("id") UUID id) {
        return service.getNameAndPic(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{id}/profile")
    ResponseEntity<String> findProfile(@PathVariable("id") UUID id) {
        return service.getAbout(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{id}/bio")
    ResponseEntity<Bio> findBio(@PathVariable("id") UUID id) {
        return service.getBio(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
