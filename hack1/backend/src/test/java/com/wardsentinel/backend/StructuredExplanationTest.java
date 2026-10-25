package com.wardsentinel.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wardsentinel.backend.dto.StructuredExplanationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredExplanationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Neutral fallback generates all 5 required structured fields")
    void testNeutralFallbackFields() {
        StructuredExplanationDto fallback = StructuredExplanationDto.neutralFallback(List.of("torso_elevation"));

        assertThat(fallback.getSummary()).isNotEmpty();
        assertThat(fallback.getSupportingEvidence()).isNotEmpty();
        assertThat(fallback.getLimitations()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(fallback.getMissingData()).contains("torso_elevation");
        assertThat(fallback.getRecommendedHumanReview()).isNotEmpty();
    }

    @Test
    @DisplayName("Deserializes valid structured Gemini JSON into DTO")
    void testDeserialization() throws Exception {
        String json = """
        {
          "summary": "Patient demonstrates progressive bed-edge proximity.",
          "supporting_evidence": ["Edge proximity 0.88", "Torso elevation 45 deg"],
          "limitations": ["Demonstration only", "Not a calibrated probability"],
          "missing_data": ["None reported"],
          "recommended_human_review": ["Visual bedside check"]
        }
        """;

        StructuredExplanationDto dto = objectMapper.readValue(json, StructuredExplanationDto.class);

        assertThat(dto.getSummary()).isEqualTo("Patient demonstrates progressive bed-edge proximity.");
        assertThat(dto.getSupportingEvidence()).hasSize(2);
        assertThat(dto.getLimitations()).hasSize(2);
        assertThat(dto.getMissingData()).containsExactly("None reported");
        assertThat(dto.getRecommendedHumanReview()).containsExactly("Visual bedside check");
    }
}
