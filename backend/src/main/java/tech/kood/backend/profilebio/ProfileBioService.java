package tech.kood.backend.profilebio;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProfileBioService {

    private final ProfileRepository profileRep;
    private final BioRepository bioRep;

    public ProfileBioService(ProfileRepository profileRep, BioRepository bioRep) {
        this.profileRep = profileRep;
        this.bioRep = bioRep;
    }

    public record NameAndPic(UUID id, String name, String avatar) {}

    public Optional<NameAndPic> getNameAndPic(UUID id) {
        return profileRep.findById(id).map(profile ->
                new NameAndPic(profile.userId(), profile.displayName(), profile.avatarPath()));
    }

    public Optional<String> getAbout(UUID id) {
        return profileRep.findById(id).map(Profile::aboutMe);
    }

    public Optional<Bio> getBio(UUID id) {
        return bioRep.findById(id);
    }
}
