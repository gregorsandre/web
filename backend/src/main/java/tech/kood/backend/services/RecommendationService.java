package tech.kood.backend.services;

import org.springframework.stereotype.Service;
import tech.kood.backend.NotFoundException;
import tech.kood.backend.records.Bio;
import tech.kood.backend.repositories.BioRepository;
import tech.kood.backend.records.Tag;
import tech.kood.backend.repositories.TagRepository;
import tech.kood.backend.records.Recommendation;

import java.util.*;

@Service
public class RecommendationService {
    private final BioRepository bioRep;
    private final TagRepository tagRep;

    RecommendationService(BioRepository bioRep, TagRepository tagRep) {
        this.bioRep = bioRep;
        this.tagRep = tagRep;
    }

    public List<UUID> getRecommendationIds(UUID id) {
        Bio me = bioRep.findById(id).orElseThrow(() -> new NotFoundException("Bio not found"));
        List<Bio> potentialRecommendations = bioRep.findRecommendations(id);
        List<Recommendation> recommendations = new ArrayList<>();
        for (Bio user : potentialRecommendations) {
            int match = scoreMatch(me, user);
            if (match >= 4) {
                double distance = haversine(user.lat(), user.lng(), me.lat(), me.lng());
                Recommendation scoredCandidate = new Recommendation(user.userId(), match, distance);
                recommendations.add(scoredCandidate);
            }
        }
        recommendations.sort(Comparator.comparingInt(Recommendation::score).reversed()
                .thenComparingDouble(Recommendation::distance)
                .thenComparing(Recommendation::userId));
        List<UUID> bestRecommendations = new ArrayList<>();
        for (int i = 0; i < 10 && i < recommendations.size(); i++) {
            bestRecommendations.add(recommendations.get(i).userId());
        }
        return bestRecommendations;
    }

    public Integer scoreMatch(Bio a, Bio b) {
        int score = 0;
        if (Math.abs(a.age() - b.age()) <= 5) {
            score += 2;
        }
        List<Tag> userATags = tagRep.findByUser(a.userId());
        List<Tag> userBTags = tagRep.findByUser(b.userId());
        List<Tag> commonTags = userATags.stream().filter(userBTags::contains).toList();
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
        if (a.envPref().equals(b.envPref()) || a.envPref().contains(b.envPref()) || b.envPref().contains(a.envPref())) {
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

    private Double haversine(double lat1, double lng1, double lat2, double lng2) {
        // distance between latitudes and longitudes
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);

        // convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // apply formulae
        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.pow(Math.sin(dLon / 2), 2) *
                        Math.cos(lat1) *
                        Math.cos(lat2);
        double rad = 6371;
        double c = 2 * Math.asin(Math.sqrt(a));
        return rad * c;
    }
}
