package com.wardsentinel.backend;

import com.wardsentinel.backend.domain.model.RiskLevel;
import com.wardsentinel.backend.domain.model.SignalQuality;
import com.wardsentinel.backend.service.SafetyPolicyEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SafetyPolicyEngineTest {

    private SafetyPolicyEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SafetyPolicyEngine();
        ReflectionTestUtils.setField(engine, "policyVersion", "v1.0-deterministic");
        ReflectionTestUtils.setField(engine, "lowThreshold", 0.35);
        ReflectionTestUtils.setField(engine, "elevatedThreshold", 0.60);
        ReflectionTestUtils.setField(engine, "highThreshold", 0.80);
        ReflectionTestUtils.setField(engine, "autoEscalateDelayMs", 120000);
    }

    @Test
    @DisplayName("Score < 0.35 yields LOW risk without safety event")
    void testLowRiskBaseline() {
        var decision = engine.evaluate(0.20, SignalQuality.GOOD, 0.05);
        assertThat(decision.severity()).isEqualTo(RiskLevel.LOW);
        assertThat(decision.requiresSafetyEvent()).isFalse();
        assertThat(decision.autoEscalateAfterMs()).isNull();
    }

    @Test
    @DisplayName("Score in 0.35-0.59 yields ELEVATED risk without active alert")
    void testElevatedRisk() {
        var decision = engine.evaluate(0.48, SignalQuality.GOOD, 0.08);
        assertThat(decision.severity()).isEqualTo(RiskLevel.ELEVATED);
        assertThat(decision.requiresSafetyEvent()).isFalse();
        assertThat(decision.autoEscalateAfterMs()).isNull();
    }

    @Test
    @DisplayName("Score in 0.60-0.79 yields HIGH risk with safety event and auto-escalate timer")
    void testHighRiskEvent() {
        var decision = engine.evaluate(0.72, SignalQuality.GOOD, 0.10);
        assertThat(decision.severity()).isEqualTo(RiskLevel.HIGH);
        assertThat(decision.requiresSafetyEvent()).isTrue();
        assertThat(decision.autoEscalateAfterMs()).isEqualTo(240000);
    }

    @Test
    @DisplayName("Score >= 0.80 yields CRITICAL risk with urgent auto-escalate timer")
    void testCriticalRiskEvent() {
        var decision = engine.evaluate(0.89, SignalQuality.GOOD, 0.12);
        assertThat(decision.severity()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(decision.requiresSafetyEvent()).isTrue();
        assertThat(decision.autoEscalateAfterMs()).isEqualTo(120000);
    }

    @Test
    @DisplayName("Lost signal triggers deterministic fail-safe HIGH event")
    void testLostSignalFailSafe() {
        var decision = engine.evaluate(0.50, SignalQuality.LOST, 1.0);
        assertThat(decision.severity()).isEqualTo(RiskLevel.HIGH);
        assertThat(decision.requiresSafetyEvent()).isTrue();
    }

    @Test
    @DisplayName("Inference failure (modelType=SKIPPED) triggers HIGH operational safety event without score fabrication")
    void testInferenceSkippedTriggersHighOperationalSafetyEvent() {
        var decision = engine.evaluate(null, SignalQuality.GOOD, 1.0, "SKIPPED");
        assertThat(decision.severity()).isEqualTo(RiskLevel.HIGH);
        assertThat(decision.requiresSafetyEvent()).isTrue();
        assertThat(decision.policyReason()).contains("Inference scoring engine unavailable");
    }

    @Test
    @DisplayName("Null risk score without LOST signal triggers operational safety event")
    void testNullRiskScoreTriggersHighOperationalSafetyEvent() {
        var decision = engine.evaluate(null, SignalQuality.GOOD, 1.0, "HEURISTIC");
        assertThat(decision.severity()).isEqualTo(RiskLevel.HIGH);
        assertThat(decision.requiresSafetyEvent()).isTrue();
    }
}
