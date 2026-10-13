package tech.kood.backend.services;

import org.springframework.stereotype.Service;
import tech.kood.backend.access.AccessService;
import tech.kood.backend.records.Bio;
import tech.kood.backend.repositories.BioRepository;
import tech.kood.backend.records.BioWithTags;
import tech.kood.backend.records.Profile;
import tech.kood.backend.repositories.ProfileRepository;
import tech.kood.backend.repositories.TagRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileBioService {

    private final ProfileRepository profileRep;
    private final BioRepository bioRep;
    private final TagRepository tagRep;
    private final AccessService accessService;

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
                bio.age(), bio.availability(), tagRep.findByUser(viewerId)));
    }
}
