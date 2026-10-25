package com.wardsentinel.backend.service;

import com.wardsentinel.backend.domain.model.RiskLevel;
import com.wardsentinel.backend.domain.model.SignalQuality;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Versioned Deterministic Safety Policy Engine.
 *
 * CRITICAL SAFETY INVARIANT:
 * Gemini LLM output is strictly for clinical explanation display.
 * LLM narratives NEVER determine alert priority, dispatching, escalation timers,
 * acknowledgement, or resolution. All safety classification is strictly governed
 * by deterministic rules in this engine.
 */
@Service
public class SafetyPolicyEngine {

    @Value("${safety-policy.version:v1.0-deterministic}")
    private String policyVersion;

    @Value("${safety-policy.low-threshold:0.35}")
    private double lowThreshold;

    @Value("${safety-policy.elevated-threshold:0.60}")
    private double elevatedThreshold;

    @Value("${safety-policy.high-threshold:0.80}")
    private double highThreshold;

    @Value("${safety-policy.auto-escalate-delay-ms:120000}")
    private int autoEscalateDelayMs;

    public record PolicyDecision(
            RiskLevel severity,
            boolean requiresSafetyEvent,
            Integer autoEscalateAfterMs,
            String policyReason
    ) {}

    public PolicyDecision evaluate(Double riskScore, SignalQuality signalQuality, Double uncertainty) {
        return evaluate(riskScore, signalQuality, uncertainty, "HEURISTIC");
    }

    public PolicyDecision evaluate(Double riskScore, SignalQuality signalQuality, Double uncertainty, String modelType) {
        // If inference failed or was skipped, represent explicitly as a degraded-system condition.
        // Never invent a usable risk score or silently classify an unavailable inference as safe.
        if ("SKIPPED".equalsIgnoreCase(modelType) || (riskScore == null && signalQuality != SignalQuality.LOST)) {
            return new PolicyDecision(
                    RiskLevel.HIGH,
                    true,
                    autoEscalateDelayMs,
                    "Inference scoring engine unavailable / degraded. Operational safety review required."
            );
        }

        if (signalQuality == SignalQuality.LOST) {
            return new PolicyDecision(
                    RiskLevel.HIGH,
                    true,
                    autoEscalateDelayMs,
                    "Hardware signal lost. Deterministic fail-safe alert triggered."
            );
        }

        double score = (riskScore != null) ? riskScore : 0.50;

        if (score >= highThreshold) {
            return new PolicyDecision(
                    RiskLevel.CRITICAL,
                    true,
                    autoEscalateDelayMs,
                    "Risk score exceeds critical threshold (" + score + " >= " + highThreshold + ")"
            );
        }

        if (score >= elevatedThreshold) {
            return new PolicyDecision(
                    RiskLevel.HIGH,
                    true,
                    autoEscalateDelayMs * 2,
                    "Risk score exceeds high threshold (" + score + " >= " + elevatedThreshold + ")"
            );
        }

        if (score >= lowThreshold) {
            return new PolicyDecision(
                    RiskLevel.ELEVATED,
                    false,
                    null,
                    "Passive highlight - score in elevated range (" + score + ")"
            );
        }

        return new PolicyDecision(
                RiskLevel.LOW,
                false,
                null,
                "Normal baseline risk score (" + score + ")"
        );
    }

    public String getPolicyVersion() {
        return policyVersion;
    }
}
