package tech.kood.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tech.kood.backend.auth.User;
import tech.kood.backend.records.Bio;
import tech.kood.backend.records.BioWithTags;
import tech.kood.backend.records.ProfileBioInput;
import tech.kood.backend.records.Tag;
import tech.kood.backend.services.ProfileBioService;

import java.util.List;

@RestController
public class ProfileBioController {

    private final ProfileBioService service;

    ProfileBioController(ProfileBioService service) {
        this.service = service;
    }

    @GetMapping("/users/{id}")
    ResponseEntity<ProfileBioService.NameAndPic> findUser(@PathVariable("id") String id, @AuthenticationPrincipal User me) {
        return service.getNameAndPic(me.id(), id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{id}/profile")
    ResponseEntity<String> findProfile(@PathVariable("id") String id, @AuthenticationPrincipal User me) {
        return service.getAbout(me.id(), id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{id}/bio")
    ResponseEntity<BioWithTags> findBioWithTags(@PathVariable("id") String id, @AuthenticationPrincipal User me) {
        return service.getBioWithTags(me.id(), id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    ResponseEntity<ProfileBioService.NameAndPic> findMe(@AuthenticationPrincipal User me) {
        return service.getNameAndPic(me.id(), "me").map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/me/profile")
    ResponseEntity<String> findMyProfile(@AuthenticationPrincipal User me) {
        return service.getAbout(me.id(), "me").map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/me/bio")
    ResponseEntity<Bio> findMyBio(@AuthenticationPrincipal User me) {
        return service.getMyBio(me.id(), "me").map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/tags")
    List<Tag> findAllTags() {
        return service.getAllTags();
    }

    @GetMapping("/me/tags")
    List<Tag> findMyTags(@AuthenticationPrincipal User me) {
        return service.getMyTags(me.id());
    }

    @PutMapping("/me/setup")
    ResponseEntity<Void> saveMySetup(@RequestBody ProfileBioInput input, @AuthenticationPrincipal User me) {
        service.saveProfileSetup(me.id(), input);
        return ResponseEntity.noContent().build();
    }
}
