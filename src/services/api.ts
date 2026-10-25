import {
  BedOverview,
  InferenceEvidence,
  PrivacyLedgerEntry,
  PrivacyVerificationResult,
  SafetyEvent,
  Staff,
  WardOverview,
} from '../types/wardsentinel';

const BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '');

// ── Fallback 12 Synthetic Beds for Ward 4B ─────────────────────────────────────
export const INITIAL_FALLBACK_BEDS: BedOverview[] = Array.from({ length: 12 }, (_, i) => {
  const num = (i + 1).toString().padStart(2, '0');
  const isOccupied = i !== 7 && i !== 10;
  const riskScores = [0.12, 0.18, 0.82, 0.44, 0.22, 0.15, 0.68, null, 0.28, 0.10, null, 0.38];
  const riskScore = isOccupied ? riskScores[i] : null;

  let riskLevel: BedOverview['latestRiskLevel'] = null;
  if (riskScore !== null) {
    if (riskScore >= 0.8) riskLevel = 'CRITICAL';
    else if (riskScore >= 0.6) riskLevel = 'HIGH';
    else if (riskScore >= 0.35) riskLevel = 'ELEVATED';
    else riskLevel = 'LOW';
  }

  const patientNames = [
    'Patient pt-4b-01 (Post-Op)',
    'Patient pt-4b-02 (Orthopedic)',
    'Patient pt-4b-03 (High Fall-Risk)',
    'Patient pt-4b-04 (Telemetry)',
    'Patient pt-4b-05 (Observation)',
    'Patient pt-4b-06 (General)',
    'Patient pt-4b-07 (Neuro Check)',
    null,
    'Patient pt-4b-09 (Rehab)',
    'Patient pt-4b-10 (Step-Down)',
    null,
    'Patient pt-4b-12 (Geriatric)',
  ];

  return {
    bedId: `bed-uuid-${num}`,
    wardId: 'WARD-4B',
    bedLabel: num,
    status: isOccupied ? 'OCCUPIED' : (i === 7 ? 'AVAILABLE' : 'CLEANING'),
    currentPatientId: isOccupied ? `pt-4b-${num}` : null,
    patientDisplayName: patientNames[i],
    latestRiskScore: riskScore,
    latestRiskLevel: riskLevel,
    activeEventId: i === 2 ? 'event-init-03' : (i === 6 ? 'event-init-07' : null),
    activeEventSeverity: i === 2 ? 'CRITICAL' : (i === 6 ? 'HIGH' : null),
    assignedNurseName: i === 2 ? 'Nurse Sarah Jenkins, RN' : (i === 6 ? 'Nurse John Davis, RN' : null),
    xCoord: (i % 6) * 4.0,
    yCoord: Math.floor(i / 6) * 6.0,
  };
});

export const INITIAL_FALLBACK_STAFF: Staff[] = [
  {
    staffId: 'staff-1',
    name: 'Nurse Sarah Jenkins, RN',
    role: 'STAFF_NURSE',
    onShift: true,
    available: true,
    primaryWardId: 'WARD-4B',
    eligibleWards: 'WARD-4B, WARD-4A',
    activeAssignmentsCount: 1,
    currentX: 4.2,
    currentY: 2.1,
  },
  {
    staffId: 'staff-2',
    name: 'Nurse John Davis, RN',
    role: 'STAFF_NURSE',
    onShift: true,
    available: true,
    primaryWardId: 'WARD-4B',
    eligibleWards: 'WARD-4B',
    activeAssignmentsCount: 1,
    currentX: 16.0,
    currentY: 5.5,
  },
  {
    staffId: 'staff-3',
    name: 'Charge Nurse Elena Rostova, BSN',
    role: 'CHARGE_NURSE',
    onShift: true,
    available: true,
    primaryWardId: 'WARD-4B',
    eligibleWards: 'WARD-4B, ICU',
    activeAssignmentsCount: 0,
    currentX: 10.0,
    currentY: 0.5,
  },
  {
    staffId: 'staff-4',
    name: 'Nurse Michael Chang, RN',
    role: 'STAFF_NURSE',
    onShift: true,
    available: false,
    primaryWardId: 'WARD-4B',
    eligibleWards: 'WARD-4B',
    activeAssignmentsCount: 2,
    currentX: 8.0,
    currentY: 6.0,
  },
];

export const INITIAL_FALLBACK_EVENTS: SafetyEvent[] = [
  {
    eventId: 'event-init-03',
    patientId: 'pt-4b-03',
    wardId: 'WARD-4B',
    triggeringResultId: 'result-init-03',
    severity: 'CRITICAL',
    status: 'DETECTED',
    detectedAt: new Date(Date.now() - 4 * 60 * 1000).toISOString(),
    acknowledgedAt: null,
    resolvedAt: null,
    assignedTo: 'Nurse Sarah Jenkins, RN',
    notes: JSON.stringify([
      { ts: new Date(Date.now() - 4 * 60 * 1000).toISOString(), author: 'DispatchEngine', text: 'Auto-assigned to Nurse Sarah Jenkins (Workload: 1, Proximity: 2.1m)' }
    ]),
    autoEscalateAfterMs: 120000,
  },
  {
    eventId: 'event-init-07',
    patientId: 'pt-4b-07',
    wardId: 'WARD-4B',
    triggeringResultId: 'result-init-07',
    severity: 'HIGH',
    status: 'ACKNOWLEDGED',
    detectedAt: new Date(Date.now() - 14 * 60 * 1000).toISOString(),
    acknowledgedAt: new Date(Date.now() - 11 * 60 * 1000).toISOString(),
    resolvedAt: null,
    assignedTo: 'Nurse John Davis, RN',
    notes: JSON.stringify([
      { ts: new Date(Date.now() - 14 * 60 * 1000).toISOString(), author: 'DispatchEngine', text: 'Dispatched to primary nurse John Davis' },
      { ts: new Date(Date.now() - 11 * 60 * 1000).toISOString(), author: 'Nurse John Davis, RN', text: 'ACKNOWLEDGED: En route to Bed 07' }
    ]),
    autoEscalateAfterMs: 240000,
  },
];

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`HTTP ${res.status}: ${text || res.statusText}`);
  }
  return res.json();
}

export const apiService = {
  async getWardOverview(wardId = 'WARD-4B'): Promise<WardOverview> {
    try {
      const res = await fetch(`${BASE_URL}/api/v1/wards/${wardId}/overview`, {
        headers: { Accept: 'application/json' },
      });
      const data = await handleResponse<WardOverview>(res);
      return { ...data, isOfflineFallback: false };
    } catch (err) {
      // Fallback response for offline demo
      return {
        wardId,
        totalBeds: INITIAL_FALLBACK_BEDS.length,
        occupiedBeds: INITIAL_FALLBACK_BEDS.filter(b => b.status === 'OCCUPIED').length,
        activeEventsCount: 2,
        criticalEventsCount: 1,
        onShiftStaffCount: INITIAL_FALLBACK_STAFF.length,
        beds: INITIAL_FALLBACK_BEDS,
        isOfflineFallback: true,
      };
    }
  },

  async listEvents(wardId = 'WARD-4B'): Promise<SafetyEvent[]> {
    try {
      const res = await fetch(`${BASE_URL}/api/v1/events?wardId=${wardId}`);
      return await handleResponse<SafetyEvent[]>(res);
    } catch {
      return INITIAL_FALLBACK_EVENTS;
    }
  },

  async getEventEvidence(eventId: string): Promise<InferenceEvidence | null> {
    try {
      const res = await fetch(`${BASE_URL}/api/v1/events/${eventId}/evidence`);
      return await handleResponse<InferenceEvidence>(res);
    } catch {
      // Synthetic fallback evidence
      return {
        resultId: `res-${eventId}`,
        windowId: crypto.randomUUID(),
        patientId: 'pt-4b-03',
        computedAt: new Date().toISOString(),
        latencyMs: 22,
        riskScore: 0.84,
        riskLevel: 'CRITICAL',
        contributingFactors: JSON.stringify([
          { feature_name: 'edge_proximity_max', value: 0.91, weight: 0.15 },
          { feature_name: 'torso_elevation_mean_deg', value: 58.0, weight: 0.12 },
          { feature_name: 'lower_body_movement_mean_g', value: 1.15, weight: 0.09 },
          { feature_name: 'occupancy_confidence_mean', value: 0.62, weight: 0.08 },
        ]),
        geminiExplanation: 'Bedside telemetry analysis: Sharp increase in bed-edge proximity and torso incline indicates pre-fall egress attempt. (Synthetic demonstration model - not for clinical decision-making).',
        modelVersion: 'v0.1.0-heuristic',
        modelType: 'HEURISTIC',
      };
    }
  },

  async acknowledgeEvent(eventId: string, assignedTo: string, note?: string): Promise<SafetyEvent> {
    const res = await fetch(`${BASE_URL}/api/v1/events/${eventId}/acknowledge`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ assignedTo, note: note || 'Nurse acknowledged alert via clinical console' }),
    });
    return handleResponse<SafetyEvent>(res);
  },

  async investigateEvent(eventId: string, note: string): Promise<SafetyEvent> {
    const res = await fetch(`${BASE_URL}/api/v1/events/${eventId}/investigate?note=${encodeURIComponent(note)}`, {
      method: 'POST',
    });
    return handleResponse<SafetyEvent>(res);
  },

  async escalateEvent(eventId: string, note: string, escalatedTo: string): Promise<SafetyEvent> {
    const res = await fetch(`${BASE_URL}/api/v1/events/${eventId}/escalate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ note, escalatedTo }),
    });
    return handleResponse<SafetyEvent>(res);
  },

  async resolveEvent(eventId: string, note: string, resolvedBy: string): Promise<SafetyEvent> {
    const res = await fetch(`${BASE_URL}/api/v1/events/${eventId}/resolve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ note, resolvedBy }),
    });
    return handleResponse<SafetyEvent>(res);
  },

  async dismissEvent(eventId: string, reason: string): Promise<SafetyEvent> {
    const res = await fetch(`${BASE_URL}/api/v1/events/${eventId}/dismiss?reason=${encodeURIComponent(reason)}`, {
      method: 'POST',
    });
    return handleResponse<SafetyEvent>(res);
  },

  async assignEvent(eventId: string, staffId: string, assignedBy: string): Promise<SafetyEvent> {
    const res = await fetch(`${BASE_URL}/api/v1/events/${eventId}/assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ staffId, assignedBy }),
    });
    return handleResponse<SafetyEvent>(res);
  },

  async getStaffAvailability(): Promise<Staff[]> {
    try {
      const res = await fetch(`${BASE_URL}/api/v1/staff/availability`);
      return await handleResponse<Staff[]>(res);
    } catch {
      return INITIAL_FALLBACK_STAFF;
    }
  },

  async getPrivacyLedger(): Promise<PrivacyLedgerEntry[]> {
    try {
      const res = await fetch(`${BASE_URL}/api/v1/privacy/ledger`);
      return await handleResponse<PrivacyLedgerEntry[]>(res);
    } catch {
      return [
        {
          entryId: 'led-offline-demo-1',
          windowId: crypto.randomUUID(),
          patientToken: 'pt-4b-03',
          eventType: 'WINDOW_INGESTION_AND_INFERENCE',
          canonicalSha256: '8f921ab02c89f1d072834b5c7e419a3b8d01f92e741c84f69324029bc417ea92',
          prevHash: '0000000000000000000000000000000000000000000000000000000000000000',
          dataMinimizationNote: '[OFFLINE DEMO DATA] Ingested 5-minute ambient window. Zero raw audio/video collected.',
          createdAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
        },
      ];
    }
  },

  async verifyPrivacyLedger(): Promise<PrivacyVerificationResult> {
    // CRITICAL: Never claim ledger verification succeeded unless backend confirms it!
    const res = await fetch(`${BASE_URL}/api/v1/privacy/ledger/verify`, {
      method: 'POST',
    });
    const result = await handleResponse<PrivacyVerificationResult>(res);
    return { ...result, isOfflineFallback: false };
  },

  async submitTelemetryWindow(payload: any): Promise<any> {
    const res = await fetch(`${BASE_URL}/api/v1/telemetry/windows`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return handleResponse<any>(res);
  },
};
