"""
Tests for Gemini non-critical explanation service:
- Valid structured output validation
- Invalid output handling & neutral fallback
- Bounded timeout enforcement
- Unavailable provider / missing credential graceful degradation
- Strict safety boundary verification (no diagnoses, no fall claims, no patient identifiers)
"""

import asyncio
import json
from unittest.mock import MagicMock, patch

import pytest
from app.models.schemas import ContributingFactor, RiskLevel, StructuredExplanation
from app.services.gemini_explain import (
    build_explanation_prompt,
    generate_explanation,
    generate_structured_explanation,
    get_neutral_fallback,
)


@pytest.fixture(autouse=True)
def reset_gemini_client():
    import app.services.gemini_explain as ge
    old_client = ge._client
    ge._client = None
    yield
    ge._client = old_client


@pytest.fixture
def sample_factors():
    return [
        ContributingFactor(feature_name="edge_proximity_max", value=0.88, weight=0.142),
        ContributingFactor(feature_name="torso_elevation_mean_deg", value=42.5, weight=0.114),
        ContributingFactor(feature_name="motion_mean_g", value=0.165, weight=0.086),
    ]


@pytest.mark.asyncio
async def test_unavailable_provider_fallback(sample_factors):
    """When GEMINI_API_KEY is absent or client unavailable, returns neutral fallback without raising."""
    with patch("app.services.gemini_explain._get_client", return_value=None):
        result = await generate_structured_explanation(
            risk_level=RiskLevel.HIGH,
            risk_score=0.72,
            factors=sample_factors,
            missing_modalities=["torso_elevation"],
            warnings=["Degraded signal detected"],
        )

        assert isinstance(result, StructuredExplanation)
        assert "Deterministic safety rules" in result.summary or "fallback" in result.summary
        assert len(result.supporting_evidence) >= 1
        assert len(result.limitations) >= 1
        assert "torso_elevation" in result.missing_data
        assert len(result.recommended_human_review) >= 1


@pytest.mark.asyncio
async def test_valid_structured_output(sample_factors):
    """When Gemini returns valid JSON matching schema, parses correctly into all 5 fields."""
    mock_payload = {
        "summary": "Patient demonstrates progressive torso elevation and significant edge proximity indicative of bed-exit movement.",
        "supportingEvidence": [
            "Edge proximity reached 0.88 (+0.142 weight)",
            "Torso elevation inclined to 42.5 degrees",
        ],
        "limitations": [
            "Demonstration score only — not for autonomous clinical decisions",
            "Not a clinically validated probability",
        ],
        "missingData": ["None reported"],
        "recommendedHumanReview": [
            "Visual verification of patient alignment at bedside",
            "Confirm call light is within patient reach",
        ],
    }

    mock_response = MagicMock()
    mock_response.text = json.dumps(mock_payload)

    mock_client = MagicMock()
    mock_client.models.generate_content.return_value = mock_response

    with patch("app.services.gemini_explain._get_client", return_value=mock_client):
        result = await generate_structured_explanation(
            risk_level=RiskLevel.CRITICAL,
            risk_score=0.86,
            factors=sample_factors,
            missing_modalities=[],
            warnings=[],
            timeout_seconds=2.0,
        )

        assert isinstance(result, StructuredExplanation)
        assert "bed-exit" in result.summary
        assert len(result.supporting_evidence) == 2
        assert len(result.limitations) == 2
        assert len(result.recommended_human_review) == 2


@pytest.mark.asyncio
async def test_invalid_output_fallback(sample_factors):
    """When Gemini returns malformed text or invalid JSON, falls back to neutral explanation."""
    mock_response = MagicMock()
    mock_response.text = "NOT_A_VALID_JSON_OBJECT {{{broken"

    mock_client = MagicMock()
    mock_client.models.generate_content.return_value = mock_response

    with patch("app.services.gemini_explain._get_client", return_value=mock_client):
        result = await generate_structured_explanation(
            risk_level=RiskLevel.HIGH,
            risk_score=0.74,
            factors=sample_factors,
            missing_modalities=[],
            warnings=[],
            timeout_seconds=1.0,
        )

        assert isinstance(result, StructuredExplanation)
        assert result.summary != ""
        assert len(result.limitations) >= 1
        assert "Demonstration heuristic score" in result.limitations[0]


@pytest.mark.asyncio
async def test_timeout_fallback(sample_factors):
    """When Gemini API call hangs beyond configured timeout, gracefully returns neutral fallback."""
    mock_client = MagicMock()

    with patch("app.services.gemini_explain._get_client", return_value=mock_client):
        with patch("asyncio.wait_for", side_effect=asyncio.TimeoutError("Bounded timeout reached")):
            result = await generate_structured_explanation(
                risk_level=RiskLevel.HIGH,
                risk_score=0.74,
                factors=sample_factors,
                missing_modalities=[],
                warnings=[],
                timeout_seconds=0.1,
            )

            assert isinstance(result, StructuredExplanation)
            assert "timed out" in result.summary.lower()
            assert len(result.limitations) >= 1


def test_safety_boundary_compliance(sample_factors):
    """Verifies prompt builder excludes patient identifiers and explicitly states safety boundaries."""
    prompt = build_explanation_prompt(
        risk_level=RiskLevel.HIGH,
        risk_score=0.75,
        factors=sample_factors,
        missing_modalities=["spo2_pct"],
        warnings=["Low battery on unit"],
    )

    # Must contain strict safety boundaries
    assert "MANDATORY SAFETY BOUNDARIES" in prompt
    assert "Do NOT diagnose any medical condition" in prompt
    assert "Do NOT claim that a fall has occurred" in prompt
    assert "Do NOT claim that this score is a calibrated clinical probability" in prompt
    assert "Do NOT recommend prescription drugs" in prompt
    assert "Do NOT suggest changing event priority" in prompt

    # Must NOT contain any patient personal identifiers or MRN
    assert "mrn" not in prompt.lower()
    assert "patient_id" not in prompt.lower()
    assert "name" not in prompt.lower()


@pytest.mark.asyncio
async def test_generate_explanation_returns_valid_json_string(sample_factors):
    """generate_explanation helper returns a valid JSON string parsable into StructuredExplanation."""
    with patch("app.services.gemini_explain._get_client", return_value=None):
        raw_json = await generate_explanation(
            risk_level=RiskLevel.HIGH,
            risk_score=0.70,
            factors=sample_factors,
            missing_modalities=["torso_elevation"],
            warnings=[],
        )

        assert isinstance(raw_json, str)
        data = json.loads(raw_json)
        parsed = StructuredExplanation.model_validate(data)
        assert parsed.summary != ""
        assert len(parsed.limitations) >= 1
        assert "torso_elevation" in parsed.missing_data
