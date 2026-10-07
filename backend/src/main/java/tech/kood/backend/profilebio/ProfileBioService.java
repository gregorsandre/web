package tech.kood.backend.profilebio;

import org.springframework.stereotype.Service;
import tech.kood.backend.access.AccessService;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileBioService {

    private final ProfileRepository profileRep;
    private final BioRepository bioRep;
    private final AccessService accessService;

    public ProfileBioService(ProfileRepository profileRep, BioRepository bioRep, AccessService accessService) {
        this.profileRep = profileRep;
        this.bioRep = bioRep;
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

    public Optional<Bio> getBio(UUID viewerId, String rawId) {
        UUID target = accessService.requireViewable(viewerId, rawId);
        return bioRep.findById(target);
    }
}
