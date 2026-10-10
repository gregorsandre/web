package tech.kood.backend.recommendation;

import org.springframework.stereotype.Service;
import tech.kood.backend.profilebio.BioRepository;
import tech.kood.backend.profilebio.ProfileRepository;

@Service
public class RecommendationService {
    private final BioRepository bioRep;
    private final ProfileRepository profileRep;
    private final TagRepository tagRep;

    RecommendationService(BioRepository bioRep, ProfileRepository profileRep, TagRepository tagRep) {
        this.bioRep = bioRep;
        this.profileRep = profileRep;
        this.tagRep = tagRep;
    }
}
