package tech.grastone.fz.matching.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class LowkeyCompatibilityResultDto {
    private int score;
    private String matchGrade;
    private List<String> explanations;
    private Map<String, Integer> breakdown;
    private int freshnessScore;

    public String getMatchExplanation() {
        return explanations == null || explanations.isEmpty()
                ? "Nearby and available now"
                : String.join("\n", explanations);
    }
}
