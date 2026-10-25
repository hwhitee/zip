export type RiskLevel = 'LOW' | 'ELEVATED' | 'HIGH' | 'CRITICAL';

export type SafetyEventStatus =
  | 'DETECTED'
  | 'ACKNOWLEDGED'
  | 'INVESTIGATING'
  | 'ESCALATED'
  | 'RESOLVED'
  | 'DISMISSED';

export type BedStatus = 'AVAILABLE' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE';

export type StaffRole = 'STAFF_NURSE' | 'CHARGE_NURSE' | 'NURSE_PRACTITIONER' | 'PHYSICIAN';

export type SignalQuality = 'GOOD' | 'DEGRADED' | 'LOST';

export interface BedOverview {
  bedId: string;
  wardId: string;
  bedLabel: string;
  status: BedStatus;
  currentPatientId: string | null;
  patientDisplayName: string | null;
  latestRiskScore: number | null;
  latestRiskLevel: RiskLevel | null;
  activeEventId: string | null;
  activeEventSeverity: RiskLevel | null;
  assignedNurseName: string | null;
  xCoord: number;
  yCoord: number;
}

export interface WardOverview {
  wardId: string;
  totalBeds: number;
  occupiedBeds: number;
  activeEventsCount: number;
  criticalEventsCount: number;
  onShiftStaffCount: number;
  beds: BedOverview[];
  isOfflineFallback?: boolean;
}

export interface ContributingFactor {
  feature_name: string;
  value: number;
  weight: number;
}

export interface SafetyEvent {
  eventId: string;
  patientId: string;
  wardId: string;
  triggeringResultId: string | null;
  severity: RiskLevel;
  status: SafetyEventStatus;
  detectedAt: string;
  acknowledgedAt: string | null;
  resolvedAt: string | null;
  assignedTo: string | null;
  notes: string;
  autoEscalateAfterMs: number | null;
  updatedAt?: string;
}

export interface StructuredExplanation {
  summary: string;
  supporting_evidence: string[];
  limitations: string[];
  missing_data: string[];
  recommended_human_review: string[];
}

export interface InferenceEvidence {
  resultId: string;
  windowId: string;
  patientId: string;
  computedAt: string;
  latencyMs: number;
  riskScore: number;
  riskLevel: RiskLevel;
  contributingFactors: string; // JSON array of ContributingFactor
  geminiExplanation: string | null;
  modelVersion: string;
  modelType: string;
}

export interface Staff {
  staffId: string;
  name: string;
  role: StaffRole;
  onShift: boolean;
  available: boolean;
  primaryWardId: string;
  eligibleWards: string;
  activeAssignmentsCount: number;
  currentX: number;
  currentY: number;
}

export interface PrivacyLedgerEntry {
  entryId: string;
  windowId: string;
  patientToken: string;
  eventType: string;
  canonicalSha256: string;
  prevHash: string;
  dataMinimizationNote: string;
  createdAt: string;
}

export interface PrivacyVerificationResult {
  verified: boolean;
  totalEntriesChecked: number;
  tamperedEntryId: string | null;
  message: string;
  securityLimitationsNotice: string;
  verifiedAt: string;
  isOfflineFallback?: boolean;
}

export interface ScenarioDefinition {
  id: string;
  title: string;
  category: 'SAFETY' | 'HARDWARE' | 'OPERATIONAL';
  description: string;
  expectedOutcome: string;
}
