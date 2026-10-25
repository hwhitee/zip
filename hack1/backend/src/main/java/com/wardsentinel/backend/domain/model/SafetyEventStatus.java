package com.wardsentinel.backend.domain.model;

public enum SafetyEventStatus {
    DETECTED,
    ACKNOWLEDGED,
    INVESTIGATING,
    ESCALATED,
    RESOLVED,
    DISMISSED;

    public boolean canTransitionTo(SafetyEventStatus target) {
        if (this == target) return false;
        return switch (this) {
            case DETECTED -> target == ACKNOWLEDGED || target == DISMISSED || target == ESCALATED;
            case ACKNOWLEDGED -> target == INVESTIGATING || target == ESCALATED || target == RESOLVED;
            case INVESTIGATING -> target == ESCALATED || target == RESOLVED;
            case ESCALATED -> target == ACKNOWLEDGED || target == RESOLVED;
            case RESOLVED, DISMISSED -> false;
        };
    }
}
