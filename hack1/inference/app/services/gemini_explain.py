"""
Non-critical Gemini explanation service for WardSentinel AI.

STRICT SAFETY BOUNDARIES:
- Gemini output is strictly advisory narrative for display to clinical staff.
- Gemini MUST NOT create, suppress, prioritize, assign, acknowledge, escalate,
  resolve, or dismiss events.
- Gemini MUST NOT diagnose a patient, claim a fall has occurred, or claim a score
  is a validated clinical probability.
- Never sends real patient identifiers, MRNs, raw audio/video, or facial embeddings.
- Reads API key strictly from server-side configuration (GEMINI_API_KEY).
- Bounded timeouts and resilient fallbacks ensure deterministic safety workflow
  remains 100% operational when Gemini is disabled, rate-limited, or unavailable.
"""

from __future__ import annotations

import asyncio
import json
import logging
import os
from typing import Any

from app.models.schemas import ContributingFactor, RiskLevel, StructuredExplanation

logger = logging.getLogger(__name__)

_API_KEY: str | None = os.getenv("GEMINI_API_KEY")
_MODEL: str = os.getenv("GEMINI_MODEL", "gemini-3.8-flash")
_TIMEOUT_SECONDS: float = float(os.getenv("GEMINI_TIMEOUT_SECONDS", "3.0"))

_client: Any = None


def _get_client():
    global _client
    if _client is not None:
        return _client
    api_key = os.getenv("GEMINI_API_KEY") or _API_KEY
    if not api_key:
        return None
    try:
        from google import genai
        _client = genai.Client(api_key=api_key)
        return _client
    except ImportError:
        logger.warning(
            "google-genai package not available. "
            "Gemini explanations will default to neutral fallback."
        )
        return None


def get_neutral_fallback(
    missing_modalities: list[str] | None = None,
    warnings: list[str] | None = None,
    custom_summary: str | None = None,
) -> StructuredExplanation:
    """
    Returns a neutral, deterministic non-diagnostic fallback explanation
    when Gemini is disabled, times out, is rate-limited, or fails.
    """
    missing_list = list(missing_modalities) if missing_modalities else []
    if not missing_list:
        missing_list = ["None reported (all sensor streams present)"]

    summary_text = (
        custom_summary
        if custom_summary
        else "Ambient telemetry indicates movement or positional variation from resting baseline. "
             "Deterministic safety rules govern alert triage; AI narrative assistant is operating in fallback mode."
    )

    return StructuredExplanation(
        summary=summary_text,
        supporting_evidence=[
            "Deterministic safety policy thresholds evaluated against 5-minute sliding window",
            "Synthetically derived vital signs and bed-edge proximity features evaluated",
        ],
        limitations=[
            "Demonstration heuristic score computed from synthetic sensor streams",
            "Not a calibrated clinical probability and has not undergone clinical trials",
            "Must not be used as an autonomous medical diagnostic tool",
        ],
        missing_data=missing_list,
        recommended_human_review=[
            "Perform direct visual check at bedside for patient comfort and posture",
            "Verify physical bed-rail engagement and call-light accessibility",
            "Inspect bedside telemetry cable connectivity and signal indicator",
        ],
    )


def build_explanation_prompt(
    risk_level: RiskLevel,
    risk_score: float,
    factors: list[ContributingFactor],
    missing_modalities: list[str],
    warnings: list[str],
) -> str:
    factor_lines = "\n".join(
        f"  - {f.feature_name}: value={f.value:.3f}, weight={f.weight:+.3f}"
        for f in sorted(factors, key=lambda x: abs(x.weight), reverse=True)[:5]
    )
    missing_text = (
        f"Missing sensor streams (excluded from score): {', '.join(missing_modalities)}"
        if missing_modalities
        else "All sensor streams present."
    )
    warning_text = "\n".join(f"  ⚠ {w}" for w in warnings) if warnings else "None."

    return f"""You are a clinical decision-support explanation assistant for an acute hospital ward safety monitoring system.
Explain an ALREADY-CREATED safety alert to a nursing team based strictly on approved derived features.

MANDATORY SAFETY BOUNDARIES (VIOLATIONS STRICTLY FORBIDDEN):
1. Do NOT diagnose any medical condition or disease.
2. Do NOT claim that a fall has occurred (use descriptive phrases like 'pre-fall movement trajectory', 'egress pattern', or 'bedside repositioning').
3. Do NOT claim that this score is a calibrated clinical probability.
4. Do NOT recommend prescription drugs, medical treatments, or clinical interventions.
5. Do NOT suggest changing event priority, altering dispatch decisions, or suppressing alarms.
6. Recommendations must be restricted strictly to observational visual checks by bedside nursing staff.

Event Evidence Summary:
- Triage Severity: {risk_level.value}
- Heuristic Score: {risk_score:.3f} (0=baseline, 1=highest deviation)
- {missing_text}

Top Contributing Derived Features:
{factor_lines}

Active Telemetry Warnings:
{warning_text}

Provide your response in strict adherence to the requested JSON schema containing:
- summary: A concise 1-2 sentence non-diagnostic summary of observed motion or position trends.
- supporting_evidence: 2-4 bullet points identifying the primary sensor features driving the trend.
- limitations: 2-3 explicit disclaimers emphasizing that this is a synthetic demonstration model and not clinical diagnosis.
- missing_data: list of absent or degraded streams.
- recommended_human_review: 2-3 observational bedside checks for nursing staff."""


async def generate_structured_explanation(
    risk_level: RiskLevel,
    risk_score: float,
    factors: list[ContributingFactor],
    missing_modalities: list[str],
    warnings: list[str],
    timeout_seconds: float | None = None,
) -> StructuredExplanation:
    """
    Requests a structured explanation from Gemini with bounded timeouts.
    Gracefully returns get_neutral_fallback on any failure (rate limit, timeout, provider outage).
    """
    timeout = timeout_seconds if timeout_seconds is not None else _TIMEOUT_SECONDS
    client = _get_client()

    if client is None:
        logger.info("Gemini API key not configured or client unavailable. Using neutral fallback.")
        return get_neutral_fallback(missing_modalities, warnings)

    prompt = build_explanation_prompt(
        risk_level, risk_score, factors, missing_modalities, warnings
    )

    async def _call_api() -> str:
        # Run blocking SDK call in executor
        loop = asyncio.get_running_loop()

        def _sync_call() -> str:
            from google.genai import types
            config = types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=StructuredExplanation,
                temperature=0.2,
            )
            response = client.models.generate_content(
                model=_MODEL,
                contents=prompt,
                config=config,
            )
            return response.text or ""

        return await loop.run_in_executor(None, _sync_call)

    try:
        raw_text = await asyncio.wait_for(_call_api(), timeout=timeout)
        parsed = json.loads(raw_text)
        return StructuredExplanation.model_validate(parsed)
    except asyncio.TimeoutError:
        logger.warning("Gemini explanation timed out after %.1fs. Returning neutral fallback.", timeout)
        return get_neutral_fallback(missing_modalities, warnings, "AI explanation timed out. Using deterministic fallback.")
    except Exception as exc:  # noqa: BLE001
        logger.warning("Gemini explanation failed (non-fatal): %s. Returning neutral fallback.", exc)
        return get_neutral_fallback(missing_modalities, warnings)


async def generate_explanation(
    risk_level: RiskLevel,
    risk_score: float,
    factors: list[ContributingFactor],
    missing_modalities: list[str],
    warnings: list[str],
) -> str | None:
    """
    Generates structured explanation and returns its canonical JSON string
    for storage in InferenceResult.gemini_explanation.
    """
    structured = await generate_structured_explanation(
        risk_level=risk_level,
        risk_score=risk_score,
        factors=factors,
        missing_modalities=missing_modalities,
        warnings=warnings,
    )
    return structured.model_dump_json()
