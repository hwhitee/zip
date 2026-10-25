import { ScenarioDefinition } from '../types/wardsentinel';

export const DEMO_SCENARIOS: ScenarioDefinition[] = [
  {
    id: 'PROGRESSIVE_BED_EXIT',
    title: 'Progressive Bed-Exit Trajectory',
    category: 'SAFETY',
    description: 'Patient sits up (torso 55°), shifts toward bed perimeter (edge proximity 0.88), active lower-body movement.',
    expectedOutcome: 'Deterministic HIGH/CRITICAL alert (risk >0.75), auto-dispatches nearest available nurse.',
  },
  {
    id: 'SAFE_REPOSITIONING',
    title: 'Safe Nurse Repositioning',
    category: 'SAFETY',
    description: 'Patient turns in bed with centered edge proximity (0.15), calm motion, stable vital signs.',
    expectedOutcome: 'LOW risk score (<0.35). Suppressed false alarm — no safety event generated.',
  },
  {
    id: 'SENSOR_DROPOUT',
    title: 'Ambient Sensor Dropout / Hardware Fault',
    category: 'HARDWARE',
    description: 'Bedside telemetry unit loses connectivity (SignalQuality = LOST).',
    expectedOutcome: 'Fail-safe HIGH alert with 100% uncertainty; triggers equipment check without fake 0 score.',
  },
  {
    id: 'NURSE_UNAVAILABLE',
    title: 'Staff Saturation & Charge Nurse Escalation',
    category: 'OPERATIONAL',
    description: 'High-risk event occurs while all primary ward staff nurses are fully occupied.',
    expectedOutcome: 'Automatic fallback escalation to the Charge Nurse on duty.',
  },
];

function generateConstant(val: number, count = 300): number[] {
  return Array(count).fill(val);
}

function generateRamp(start: number, end: number, count = 300): number[] {
  const step = (end - start) / (count - 1);
  return Array.from({ length: count }, (_, i) => parseFloat((start + i * step).toFixed(2)));
}

export function buildScenarioPayload(
  scenarioId: string,
  bedLabel = '04',
  wardId = 'WARD-4B',
  patientId = 'e1f13b19-482d-4c3d-82d2-28682337b588'
) {
  const now = Date.now();
  const startEpochMs = now - 300000;
  const endEpochMs = now;
  const windowId = crypto.randomUUID();

  let signalQuality: 'GOOD' | 'DEGRADED' | 'LOST' = 'GOOD';
  let hr = generateConstant(76);
  let spo2 = generateConstant(97);
  let rr = generateConstant(16);
  let temp = generateConstant(36.8);
  let motion = generateConstant(0.04);
  let noise = generateConstant(42);
  let light = generateConstant(120);

  let torso = generateConstant(10);
  let edge = generateConstant(0.12);
  let lowerMotion = generateConstant(0.02);
  let pressureShift = generateConstant(0.08);
  let occupancy = generateConstant(0.99);

  switch (scenarioId) {
    case 'PROGRESSIVE_BED_EXIT':
      hr = generateRamp(78, 108);
      rr = generateRamp(16, 24);
      motion = generateRamp(0.05, 0.65);
      torso = generateRamp(15, 65); // sitting upright
      edge = generateRamp(0.15, 0.92); // reaching mattress edge
      lowerMotion = generateRamp(0.02, 1.35); // legs swinging over
      pressureShift = generateRamp(0.1, 0.85); // weight transfer to side rail
      occupancy = generateRamp(0.99, 0.55); // partial unloading
      break;

    case 'SAFE_REPOSITIONING':
      hr = generateConstant(74);
      motion = generateRamp(0.04, 0.22);
      torso = generateConstant(12);
      edge = generateConstant(0.20); // stays well within bed center
      lowerMotion = generateConstant(0.08);
      pressureShift = generateRamp(0.1, 0.65); // turning over safely
      occupancy = generateConstant(0.99);
      break;

    case 'SENSOR_DROPOUT':
      signalQuality = 'LOST';
      hr = generateConstant(0);
      spo2 = generateConstant(0);
      rr = generateConstant(0);
      break;

    case 'NURSE_UNAVAILABLE':
      hr = generateRamp(85, 115);
      torso = generateRamp(20, 70);
      edge = generateRamp(0.25, 0.95);
      lowerMotion = generateRamp(0.1, 1.5);
      break;
  }

  return {
    windowId,
    patientId,
    wardId,
    bedLabel,
    startEpochMs,
    endEpochMs,
    samplingHz: 1.0,
    schemaVersion: '1.0.0',
    sequenceNumber: Math.floor(Date.now() / 300000),
    idempotencyKey: windowId,
    sensors: {
      heartRateBpm: hr,
      spo2Pct: spo2,
      respRateBreaths: rr,
      skinTempC: temp,
      motionG: motion,
      ambientNoiseDb: noise,
      lightLux: light,
      torsoElevation: torso,
      edgeProximity: edge,
      lowerBodyMovement: lowerMotion,
      bedPressureShift: pressureShift,
      occupancyConfidence: occupancy,
    },
    metadata: {
      deviceId: `WARD-${wardId}-${bedLabel}`,
      firmwareVer: '2.4.1',
      signalQuality,
    },
  };
}
