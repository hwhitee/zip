package com.wardsentinel.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class StructuredExplanationDto {

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("supporting_evidence")
    private List<String> supportingEvidence;

    @JsonProperty("limitations")
    private List<String> limitations;

    @JsonProperty("missing_data")
    private List<String> missingData;

    @JsonProperty("recommended_human_review")
    private List<String> recommendedHumanReview;

    public StructuredExplanationDto() {}

    public StructuredExplanationDto(
            String summary,
            List<String> supportingEvidence,
            List<String> limitations,
            List<String> missingData,
            List<String> recommendedHumanReview
    ) {
        this.summary = summary;
        this.supportingEvidence = supportingEvidence != null ? supportingEvidence : List.of();
        this.limitations = limitations != null ? limitations : List.of();
        this.missingData = missingData != null ? missingData : List.of();
        this.recommendedHumanReview = recommendedHumanReview != null ? recommendedHumanReview : List.of();
    }

    public static StructuredExplanationDto neutralFallback(List<String> missing) {
        return new StructuredExplanationDto(
                "Ambient telemetry indicates motion or positional deviation from resting baseline. "
                        + "Deterministic safety rules govern alert triage; AI narrative assistant is operating in fallback mode.",
                List.of(
                        "Deterministic safety policy thresholds evaluated against 5-minute sliding window",
                        "Synthetically derived vital signs and bed-edge proximity features evaluated"
                ),
                List.of(
                    "Demonstration heuristic score computed from synthetic sensor streams",
                    "Not a calibrated clinical probability and has not undergone clinical trials",
                    "Must not be used as an autonomous medical diagnostic tool"
                ),
                missing != null && !missing.isEmpty() ? missing : List.of("None reported (all sensor streams present)"),
                List.of(
                    "Perform direct visual check at bedside for patient comfort and posture",
                    "Verify physical bed-rail engagement and call-light accessibility",
                    "Inspect bedside telemetry cable connectivity and signal indicator"
                )
        );
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getSupportingEvidence() { return supportingEvidence; }
    public void setSupportingEvidence(List<String> supportingEvidence) { this.supportingEvidence = supportingEvidence; }

    public List<String> getLimitations() { return limitations; }
    public void setLimitations(List<String> limitations) { this.limitations = limitations; }

    public List<String> getMissingData() { return missingData; }
    public void setMissingData(List<String> missingData) { this.missingData = missingData; }

    public List<String> getRecommendedHumanReview() { return recommendedHumanReview; }
    public void setRecommendedHumanReview(List<String> recommendedHumanReview) { this.recommendedHumanReview = recommendedHumanReview; }
}
