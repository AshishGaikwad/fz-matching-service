package tech.grastone.fz.matching.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tech.grastone.fz.matching.dto.LowkeyCompatibilityResultDto;
import tech.grastone.fz.matching.dto.PreferencesDto;
import tech.grastone.fz.matching.dto.UserDto;
import tech.grastone.fz.matching.entity.LowkeyDiscoveryHistoryEntity;
import tech.grastone.fz.matching.entity.LowkeySessionEntity;
import tech.grastone.fz.matching.enums.LookingFor;

@Component
public class LowkeyCompatibilityEngine {

    public LowkeyCompatibilityResultDto score(
            UserDto viewer,
            PreferencesDto viewerPreference,
            LowkeySessionEntity viewerSession,
            UserDto candidate,
            PreferencesDto candidatePreference,
            LowkeySessionEntity candidateSession,
            LowkeyDiscoveryHistoryEntity history,
            double distanceKm
    ) {
        List<String> explanations = new ArrayList<>();
        Map<String, Integer> breakdown = new LinkedHashMap<>();

        int locationAccuracy = locationAccuracyScore(candidateSession.getLocationAccuracyMeters());
        int distance = distanceScore(distanceKm, viewerSession.getRadiusKm());
        int age = ageScore(viewer, candidate, viewerPreference, candidatePreference);
        int lookingFor = lookingForScore(viewer, viewerSession, viewerPreference, candidate, candidateSession, candidatePreference);
        String viewerProfession = firstNonBlank(
                viewerPreference == null ? null : viewerPreference.getProfession(),
                viewer.getProfession()
        );
        String candidateProfession = firstNonBlank(
                candidatePreference == null ? null : candidatePreference.getProfession(),
                candidate.getProfession()
        );
        int profession = professionScore(viewerProfession, candidateProfession);
        int interestLifestyle = interestLifestyleScore(viewerPreference, candidatePreference);
        int freshness = freshnessScore(history);

        breakdown.put("locationAccuracy", locationAccuracy);
        breakdown.put("distance", distance);
        breakdown.put("ageCompatibility", age);
        breakdown.put("lookingForIntent", lookingFor);
        breakdown.put("professionSimilarity", profession);
        breakdown.put("interestAndLifestyle", interestLifestyle);
        breakdown.put("discoveryFreshness", freshness);

        int total = breakdown.values().stream().mapToInt(Integer::intValue).sum();
        int score = Math.min(100, Math.max(1, total));

        if (distanceKm <= 1.5) {
            explanations.add("Close by (" + formatDistance(distanceKm) + ")");
        } else if (distance >= 14) {
            explanations.add("Within your nearby radius");
        }

        if (age >= 16) {
            explanations.add("Similar age range");
        }

        Set<LookingFor> sharedIntent = sharedIntent(
                viewer,
                viewerSession,
                viewerPreference,
                candidate,
                candidateSession,
                candidatePreference
        );
        if (!sharedIntent.isEmpty()) {
            explanations.add("Both looking for " + label(sharedIntent.iterator().next()));
        }

        if (profession >= 4 && candidateProfession != null) {
            explanations.add("Similar professional energy");
        }

        if (explanations.isEmpty()) {
            explanations.add("Nearby and open to connecting");
        }

        LowkeyCompatibilityResultDto result = new LowkeyCompatibilityResultDto();
        result.setScore(score);
        result.setMatchGrade(matchGrade(score));
        result.setExplanations(explanations.stream().limit(4).toList());
        result.setBreakdown(breakdown);
        result.setFreshnessScore(freshness);
        return result;
    }

    private int locationAccuracyScore(Integer accuracyMeters) {
        if (accuracyMeters == null || accuracyMeters <= 0) {
            return 9;
        }
        if (accuracyMeters <= 50) {
            return 15;
        }
        if (accuracyMeters <= 100) {
            return 13;
        }
        if (accuracyMeters <= 250) {
            return 10;
        }
        if (accuracyMeters <= 600) {
            return 7;
        }
        return 4;
    }

    private int distanceScore(double distanceKm, Integer radiusKm) {
        int radius = Math.max(1, radiusKm == null ? 25 : radiusKm);
        double normalized = Math.min(1.0, Math.max(0.0, distanceKm) / radius);
        return (int) Math.round((1.0 - normalized) * 25);
    }

    private int ageScore(
            UserDto viewer,
            UserDto candidate,
            PreferencesDto viewerPreference,
            PreferencesDto candidatePreference
    ) {
        int viewerAge = age(viewer.getDob());
        int candidateAge = age(candidate.getDob());
        if (viewerAge <= 0 || candidateAge <= 0) {
            return 10;
        }

        boolean candidateInsideViewerRange = insideRange(candidateAge, viewerPreference);
        boolean viewerInsideCandidateRange = insideRange(viewerAge, candidatePreference);
        if (candidateInsideViewerRange && viewerInsideCandidateRange) {
            return 20;
        }

        int diff = Math.abs(viewerAge - candidateAge);
        if (diff <= 2) {
            return 18;
        }
        if (diff <= 5) {
            return 16;
        }
        if (diff <= 8) {
            return 12;
        }
        if (diff <= 12) {
            return 8;
        }
        return 4;
    }

    private int lookingForScore(
            UserDto viewer,
            LowkeySessionEntity viewerSession,
            PreferencesDto viewerPreference,
            UserDto candidate,
            LowkeySessionEntity candidateSession,
            PreferencesDto candidatePreference
    ) {
        Set<LookingFor> viewerIntent = lookingFor(viewer, viewerSession, viewerPreference);
        Set<LookingFor> candidateIntent = lookingFor(candidate, candidateSession, candidatePreference);
        if (viewerIntent.isEmpty() || candidateIntent.isEmpty()) {
            return 8;
        }

        Set<LookingFor> shared = new HashSet<>(viewerIntent);
        shared.retainAll(candidateIntent);
        if (!shared.isEmpty()) {
            return 15;
        }

        if (hasCasualOverlap(viewerIntent, candidateIntent)) {
            return 9;
        }
        return 4;
    }

    private int professionScore(String viewerProfession, String candidateProfession) {
        if (blank(viewerProfession) || blank(candidateProfession)) {
            return 2;
        }

        Set<String> viewerTokens = professionTokens(viewerProfession);
        Set<String> candidateTokens = professionTokens(candidateProfession);
        Set<String> shared = new HashSet<>(viewerTokens);
        shared.retainAll(candidateTokens);
        if (!shared.isEmpty()) {
            return 5;
        }

        return sameSector(viewerProfession, candidateProfession) ? 4 : 1;
    }

    private int interestLifestyleScore(PreferencesDto viewerPreference, PreferencesDto candidatePreference) {
        if (viewerPreference == null || candidatePreference == null) {
            return 2;
        }

        int score = 0;
        if (sameMeaningful(viewerPreference.getLifestyle(), candidatePreference.getLifestyle())) {
            score += 2;
        }
        if (sameMeaningful(viewerPreference.getPersonality(), candidatePreference.getPersonality())) {
            score += 1;
        }
        if (sameMeaningful(viewerPreference.getSmoking(), candidatePreference.getSmoking())) {
            score += 1;
        }
        if (sameMeaningful(viewerPreference.getDrinking(), candidatePreference.getDrinking())) {
            score += 1;
        }
        if (sameMeaningful(viewerPreference.getReligion(), candidatePreference.getReligion())) {
            score += 1;
        }
        return Math.min(5, score);
    }

    private int freshnessScore(LowkeyDiscoveryHistoryEntity history) {
        if (history == null) {
            return 5;
        }

        long hoursSinceSeen = Duration.between(history.getLastSeenAt(), LocalDateTime.now()).toHours();
        int exposureCount = history.getExposureCount() == null ? 0 : history.getExposureCount();
        if (hoursSinceSeen >= 48) {
            return 4;
        }
        if (hoursSinceSeen >= 24 && exposureCount <= 2) {
            return 3;
        }
        if (hoursSinceSeen >= 6 && exposureCount <= 1) {
            return 2;
        }
        return 0;
    }

    private Set<LookingFor> sharedIntent(
            UserDto viewer,
            LowkeySessionEntity viewerSession,
            PreferencesDto viewerPreference,
            UserDto candidate,
            LowkeySessionEntity candidateSession,
            PreferencesDto candidatePreference
    ) {
        Set<LookingFor> shared = new HashSet<>(lookingFor(viewer, viewerSession, viewerPreference));
        shared.retainAll(lookingFor(candidate, candidateSession, candidatePreference));
        return shared;
    }

    private Set<LookingFor> lookingFor(UserDto user, LowkeySessionEntity session, PreferencesDto preference) {
        Set<LookingFor> sessionIntent = parseLookingFor(session.getLookingForValues());
        if (!sessionIntent.isEmpty()) {
            return sessionIntent;
        }
        if (preference != null && preference.getLookingFor() != null && !preference.getLookingFor().isEmpty()) {
            return preference.getLookingFor();
        }
        return user.getLookingFor() == null ? Set.of() : user.getLookingFor();
    }

    private Set<LookingFor> parseLookingFor(String value) {
        if (blank(value)) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .map(this::parseIntent)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private Set<LookingFor> parseIntent(String value) {
        try {
            return Set.of(LookingFor.valueOf(value));
        } catch (IllegalArgumentException ignored) {
            return Set.of();
        }
    }

    private boolean hasCasualOverlap(Set<LookingFor> a, Set<LookingFor> b) {
        Set<LookingFor> casual = Set.of(
                LookingFor.FRIEND,
                LookingFor.CASUAL_CHAT,
                LookingFor.COFFEE_MEETUP,
                LookingFor.NETWORKING
        );
        Set<LookingFor> relationship = Set.of(
                LookingFor.DATING,
                LookingFor.RELATIONSHIP,
                LookingFor.LONG_TERM_RELATIONSHIP
        );
        return intersects(a, casual) && intersects(b, casual)
                || intersects(a, relationship) && intersects(b, relationship);
    }

    private boolean intersects(Set<LookingFor> a, Set<LookingFor> b) {
        return a.stream().anyMatch(b::contains);
    }

    private Set<String> professionTokens(String profession) {
        return Arrays.stream(profession.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toSet());
    }

    private boolean sameSector(String a, String b) {
        return sector(a).equals(sector(b));
    }

    private String sector(String profession) {
        String value = profession.toLowerCase(Locale.ROOT);
        if (containsAny(value, "software", "developer", "engineer", "tech", "data", "product", "designer")) {
            return "technology";
        }
        if (containsAny(value, "doctor", "nurse", "medical", "health", "therapist")) {
            return "health";
        }
        if (containsAny(value, "student", "teacher", "professor", "education")) {
            return "education";
        }
        if (containsAny(value, "founder", "business", "manager", "sales", "marketing", "finance")) {
            return "business";
        }
        if (containsAny(value, "artist", "creator", "music", "writer", "film", "photo")) {
            return "creative";
        }
        return "other";
    }

    private boolean containsAny(String value, String... needles) {
        return Arrays.stream(needles).anyMatch(value::contains);
    }

    private boolean sameMeaningful(Enum<?> a, Enum<?> b) {
        if (a == null || b == null || !a.equals(b)) {
            return false;
        }
        return !"NONE".equals(a.name()) && !"ANY".equals(a.name());
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }

    private boolean insideRange(int age, PreferencesDto preference) {
        if (preference == null || preference.getMinAge() <= 0 || preference.getMaxAge() <= 0) {
            return true;
        }
        return age >= preference.getMinAge() && age <= preference.getMaxAge();
    }

    private int age(LocalDate dob) {
        if (dob == null) {
            return 0;
        }
        return (int) ChronoUnit.YEARS.between(dob, LocalDate.now());
    }

    private String matchGrade(int score) {
        if (score >= 90) {
            return "A+";
        }
        if (score >= 80) {
            return "A";
        }
        if (score >= 65) {
            return "B";
        }
        return "C";
    }

    private String formatDistance(double distanceKm) {
        if (distanceKm < 1) {
            return Math.round(distanceKm * 1000) + " m";
        }
        return String.format(Locale.ROOT, "%.1f km", distanceKm);
    }

    private String label(LookingFor value) {
        String text = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
