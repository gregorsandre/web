package tech.kood.backend.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tech.kood.backend.access.AccessService;
import tech.kood.backend.records.*;
import tech.kood.backend.repositories.BioRepository;
import tech.kood.backend.repositories.ProfileRepository;
import tech.kood.backend.repositories.TagRepository;

import java.util.*;

@Service
public class ProfileBioService {

    private final ProfileRepository profileRep;
    private final BioRepository bioRep;
    private final TagRepository tagRep;
    private final AccessService accessService;

    private static final Set<String> GOAL = Set.of("fun", "fitness","competition","event_training");
    private static final Set<String> LOOKING_FOR = Set.of("regular_partner", "casual_games");
    private static final Set<String> ENV_PREF = Set.of("indoor","outdoor","both");
    private static final Set<String> AVAILABILITY = Set.of("weekday_morning","weekday_evening","weekend");

    public ProfileBioService(ProfileRepository profileRep, BioRepository bioRep, TagRepository tagRep, AccessService accessService) {
        this.profileRep = profileRep;
        this.bioRep = bioRep;
        this.tagRep = tagRep;
        this.accessService = accessService;
    }

    public record NameAndPic(UUID id, String name, String avatar) {}

    public Optional<NameAndPic> getNameAndPic(UUID viewerId, String rawId) {
        UUID target = accessService.requireViewable(viewerId, rawId);
        return profileRep.findById(target).map(profile ->
                new NameAndPic(profile.userId(), profile.displayName(), profile.avatarPath()));
    }

    public Optional<String> getAbout(UUID viewerId, String rawId) {
        UUID target = accessService.requireViewable(viewerId, rawId);
        return profileRep.findById(target).map(Profile::aboutMe);
    }
    public Optional<Bio> getMyBio(UUID viewerId, String rawId) {
        UUID target = accessService.requireViewable(viewerId, rawId);
        return bioRep.findById(target);
    }

    public Optional<BioWithTags> getBioWithTags(UUID viewerId, String rawId) {
        UUID userId = accessService.requireViewable(viewerId, rawId);
        return bioRep.findById(userId).map(bio -> new BioWithTags(bio.userId(), bio.skillLevel(), bio.goal(), bio.lookingFor(), bio.envPref(),
                bio.age(), bio.availability(), tagRep.findByUser(userId)));
    }

    public List<Tag> getAllTags() {
        return tagRep.findAll();
    }

    public List<Tag> getMyTags(UUID userId) {
        return tagRep.findByUser(userId);
    }

    @Transactional
    public void saveProfileSetup(UUID userId, ProfileBioInput in) {
        validateProfileSetup(in);
        profileRep.saveProfile(userId, in.displayName().trim(), in.aboutMe() == null ? "" : in.aboutMe().trim());
        if (bioRep.saveBio(userId, in.bio()) != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Bio could not be saved");
        }
        tagRep.replaceUserTags(userId, in.tagIds());
        profileRep.markCompleted(userId);
    }

    private void validateProfileSetup(ProfileBioInput in) {
        if (in == null) throw bad("Missing profile");
        String name = in.displayName() == null ? "" : in.displayName().trim();
        require(!name.isEmpty() && name.length() <= 50, "Name must be 1-50 characters");
        require(in.aboutMe() == null || in.aboutMe().length() <= 1000, "About me is too long");
        validate(in.bio());
        require(in.tagIds() != null && !in.tagIds().isEmpty() && in.tagIds().size() <= 20, "Choose 1-20 sports");
        require(!in.tagIds().contains(null), "Invalid sport");
        require(tagRep.tagsExist(in.tagIds()), "Unknown sport selected");
    }

    private void validate(BioInput in) {
        if (in == null) throw bad("Missing bio");

        require(in.skillLevel() != null && in.skillLevel() >= 1 && in.skillLevel() <= 5, "Skill level must be 1-5");
        require(in.goal() != null && GOAL.contains(in.goal()), "Invalid goal");
        require(in.lookingFor() != null && LOOKING_FOR.contains(in.lookingFor()), "Invalid looking-for value");
        require(in.envPref() != null && ENV_PREF.contains(in.envPref()), "Invalid environment preference");

        require(in.age() != null && in.age() >= 18 && in.age() <= 120, "Age must be 18-120");
        require(in.ageMin() != null && in.ageMax() != null, "Preferred age range is required");
        require(in.ageMin() >= 18 && in.ageMax() <= 120, "Preferred age range must be within 18-120");
        require(in.ageMin() <= in.ageMax(), "Minimum age cannot be higher than maximum age");

        require(in.lat() != null && !in.lat().isNaN() && in.lat() >= -90 && in.lat() <= 90, "Invalid latitude");
        require(in.lng() != null && !in.lng().isNaN() && in.lng() >= -180 && in.lng() <= 180, "Invalid longitude");
        require(in.maxRadiusKm() != null && in.maxRadiusKm() >= 1 && in.maxRadiusKm() <= 500, "Radius must be 1-500 km");

        require(in.availability() != null && !in.availability().isEmpty(), "Choose at least one availability");
        Set<String> unique = new HashSet<>(in.availability());
        require(unique.size() == in.availability().size(), "Duplicate availability values");
        require(AVAILABILITY.containsAll(unique), "Invalid availability value");
    }

    private void require(boolean condition, String message) {
        if (!condition) throw bad(message);
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
