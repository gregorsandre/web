package tech.kood.backend.recommendation;

import org.springframework.stereotype.Service;
import tech.kood.backend.NotFoundException;
import tech.kood.backend.profilebio.Bio;
import tech.kood.backend.profilebio.BioRepository;
import tech.kood.backend.profilebio.ProfileRepository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private final BioRepository bioRep;
    private final TagRepository tagRep;

    RecommendationService(BioRepository bioRep, TagRepository tagRep) {
        this.bioRep = bioRep;
        this.tagRep = tagRep;
    }

    public getRecommendationIds(UUID id) {}

    public int scoreMatch(UUID userA, UUID userB) {
        Bio a = bioRep.findById(userA).orElseThrow(() -> new NotFoundException("Bio not found"));
        Bio b = bioRep.findById(userB).orElseThrow(() -> new NotFoundException("Bio not found"));
        int score = 0;
        boolean ageAInRangeB = b.ageMin() <= a.age() && a.age() <= b.ageMax();
        boolean ageBInRangeA = a.ageMin() <= b.age() && b.age() <= a.ageMax();
        if (!ageAInRangeB || !ageBInRangeA) {
            return 0;
        }
        if (Math.abs(a.age() - b.age()) <= 5) {
            score += 2;
        }
        if (!bioRep.isInRange(userA,  userB)) {
            return 0;
        }
        if (!a.lookingFor().equals(b.lookingFor())) {
            return 0;
        }
        List<Tag> userATags = tagRep.findByUser(userA);
        List<Tag> userBTags = tagRep.findByUser(userB);
        List<Tag> commonTags = userATags.stream().filter(userBTags::contains).toList();
        if (commonTags.isEmpty()) {
            return 0;
        }
        if (commonTags.size() >= 2) {
            score += 2;
        }
        if (Math.abs(a.skillLevel() - b.skillLevel()) <= 1) {
            score += 1;
        }
        if (a.skillLevel().equals(b.skillLevel())) {
            score += 1;
        }
        if (a.goal().equals(b.goal())) {
            score += 1;
        }
        if (a.envPref().equals(b.envPref())) {
            score += 1;
        }
        int commonAvailability = a.availability().stream().filter(b.availability()::contains).toList().size();
        if (commonAvailability != 0) {
            score += 1;
        }
        if (commonAvailability >= 2) {
            score += 1;
        }
        return score;
    }
}
