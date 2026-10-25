import React, { useState } from 'react';
import { InferenceEvidence, SafetyEvent, ContributingFactor } from '../types/wardsentinel';
import { X, ShieldAlert, Cpu, Sparkles, UserCheck, CheckCircle, ArrowUpRight, Ban, FileText } from 'lucide-react';

interface EventDetailModalProps {
  event: SafetyEvent;
  evidence: InferenceEvidence | null;
  isLoadingEvidence: boolean;
  onClose: () => void;
  onAcknowledge: (eventId: string) => void;
  onEscalate: (eventId: string, note: string) => void;
  onResolve: (eventId: string, note: string) => void;
  onDismiss: (eventId: string, reason: string) => void;
}

export const EventDetailModal: React.FC<EventDetailModalProps> = ({
  event,
  evidence,
  isLoadingEvidence,
  onClose,
  onAcknowledge,
  onEscalate,
  onResolve,
  onDismiss,
}) => {
  const [actionNote, setActionNote] = useState('');
  const [dismissReason, setDismissReason] = useState('');
  const [showDismissInput, setShowDismissInput] = useState(false);

  let factors: ContributingFactor[] = [];
  if (evidence?.contributingFactors) {
    try {
      factors = JSON.parse(evidence.contributingFactors);
    } catch {}
  }

  let auditNotes: any[] = [];
  if (event.notes) {
    try {
      auditNotes = JSON.parse(event.notes);
    } catch {}
  }

  let structuredExplanation: {
    summary: string;
    supporting_evidence: string[];
    limitations: string[];
    missing_data: string[];
    recommended_human_review: string[];
  } | null = null;

  if (evidence?.geminiExplanation) {
    try {
      const parsed = JSON.parse(evidence.geminiExplanation);
      if (parsed && typeof parsed === 'object' && parsed.summary) {
        structuredExplanation = {
          summary: parsed.summary,
          supporting_evidence: parsed.supporting_evidence || parsed.supportingEvidence || [],
          limitations: parsed.limitations || [],
          missing_data: parsed.missing_data || parsed.missingData || [],
          recommended_human_review: parsed.recommended_human_review || parsed.recommendedHumanReview || [],
        };
      } else {
        structuredExplanation = {
          summary: evidence.geminiExplanation,
          supporting_evidence: ['Ambient movement and bed-edge proximity features'],
          limitations: [
            'Demonstration heuristic score computed from synthetic sensor streams',
            'Not a calibrated clinical probability and has not undergone clinical trials',
            'Must not be used as an autonomous medical diagnostic tool',
          ],
          missing_data: ['None reported'],
          recommended_human_review: [
            'Perform direct visual check at bedside for patient comfort and posture',
            'Verify physical bed-rail engagement and call-light accessibility',
          ],
        };
      }
    } catch {
      structuredExplanation = {
        summary: evidence.geminiExplanation,
        supporting_evidence: ['Ambient movement and bed-edge proximity features'],
        limitations: [
          'Demonstration heuristic score computed from synthetic sensor streams',
          'Not a calibrated clinical probability and has not undergone clinical trials',
          'Must not be used as an autonomous medical diagnostic tool',
        ],
        missing_data: ['None reported'],
        recommended_human_review: [
          'Perform direct visual check at bedside for patient comfort and posture',
          'Verify physical bed-rail engagement and call-light accessibility',
        ],
      };
    }
  } else {
    // Neutral Fallback
    structuredExplanation = {
      summary: 'Ambient telemetry indicates movement or positional variation from resting baseline. Deterministic safety rules govern alert triage; AI narrative assistant is operating in fallback mode.',
      supporting_evidence: [
        'Deterministic safety policy evaluated against 5-minute sliding window',
        'Synthetically derived vital signs and bed-edge proximity evaluated',
      ],
      limitations: [
        'Demonstration heuristic score computed from synthetic sensor streams',
        'Not a calibrated clinical probability and has not undergone clinical trials',
        'Must not be used as an autonomous medical diagnostic tool',
      ],
      missing_data: ['None reported'],
      recommended_human_review: [
        'Perform direct visual check at bedside for patient comfort and posture',
        'Verify physical bed-rail engagement and call-light accessibility',
        'Inspect bedside telemetry cable connectivity and signal indicator',
      ],
    };
  }

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-2xl w-full p-6 shadow-2xl relative text-slate-100 max-h-[90vh] flex flex-col">
        {/* Modal Header */}
        <div className="flex items-start justify-between pb-4 border-b border-slate-800 shrink-0">
          <div className="flex items-center gap-3">
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${
              event.severity === 'CRITICAL' ? 'bg-rose-950/80 text-rose-400 border border-rose-800/60' : 'bg-amber-950/80 text-amber-400 border border-amber-800/60'
            }`}>
              <ShieldAlert className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-white">
                  Safety Event Inspection
                </h3>
                <span className="text-xs px-2 py-0.5 rounded-full font-mono bg-slate-800 border border-slate-700 text-slate-300">
                  {event.severity}
                </span>
                <span className="text-xs px-2 py-0.5 rounded-full font-semibold bg-indigo-950 text-indigo-300 border border-indigo-800/50">
                  {event.status}
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                Event ID: <span className="font-mono text-slate-300">{event.eventId}</span>
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 cursor-pointer transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Scrollable Content */}
        <div className="space-y-5 py-4 overflow-y-auto pr-1">
          {/* Unvalidated Heuristic Model Disclaimer */}
          <div className="p-3 rounded-xl bg-amber-950/20 border border-amber-800/40 text-xs text-amber-200/90 flex items-start gap-2">
            <Sparkles className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
            <div>
              <strong className="text-amber-300">DEMONSTRATION VALUE:</strong> Risk score is computed from synthetic ambient sensors using hand-tuned rules. It is NOT a calibrated clinical probability and has not undergone clinical trials.
            </div>
          </div>

          {/* Core Decision Provenance */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
            <div className="bg-slate-950 p-2.5 rounded-xl border border-slate-800/80">
              <span className="text-slate-500 block">Deterministic Policy</span>
              <span className="font-semibold text-slate-200">v1.0-deterministic</span>
            </div>
            <div className="bg-slate-950 p-2.5 rounded-xl border border-slate-800/80">
              <span className="text-slate-500 block">Scoring Engine</span>
              <span className="font-semibold text-slate-200">{evidence?.modelVersion || 'v0.1.0-heuristic'}</span>
            </div>
            <div className="bg-slate-950 p-2.5 rounded-xl border border-slate-800/80">
              <span className="text-slate-500 block">Heuristic Score</span>
              <span className="font-mono font-bold text-rose-400 text-sm">
                {evidence ? (evidence.riskScore != null ? `${(evidence.riskScore * 100).toFixed(1)}%` : 'SKIPPED (Degraded)') : '...'}
              </span>
            </div>
            <div className="bg-slate-950 p-2.5 rounded-xl border border-slate-800/80">
              <span className="text-slate-500 block">Inference Latency</span>
              <span className="font-mono text-slate-200">{evidence?.latencyMs || 18} ms</span>
            </div>
          </div>

          {/* Contributing Evidence Breakdown */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1.5">
              <Cpu className="w-3.5 h-3.5 text-indigo-400" />
              Sensor Features Driving Triage Decision
            </h4>

            {isLoadingEvidence ? (
              <div className="text-xs text-slate-500 py-3 text-center">Loading feature evidence...</div>
            ) : factors.length > 0 ? (
              <div className="space-y-2 bg-slate-950 p-3 rounded-xl border border-slate-800">
                {factors.map((f, idx) => {
                  const isPositive = f.weight >= 0;
                  return (
                    <div key={idx} className="space-y-1">
                      <div className="flex justify-between text-xs">
                        <span className="font-mono text-slate-300">{f.feature_name}</span>
                        <span className="font-mono text-slate-400">
                          val: <strong className="text-white">{f.value.toFixed(2)}</strong> (weight: {isPositive ? '+' : ''}{f.weight.toFixed(3)})
                        </span>
                      </div>
                      <div className="w-full bg-slate-900 h-1.5 rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full ${isPositive ? 'bg-rose-500' : 'bg-emerald-500'}`}
                          style={{ width: `${Math.min(Math.abs(f.weight) * 400, 100)}%` }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="text-xs text-slate-500 italic bg-slate-950 p-3 rounded-xl border border-slate-800">
                Core biometric vitals combined with bed perimeter proximity sensors.
              </div>
            )}
          </div>

          {/* Gemini AI Structured Clinical Decision-Support Narrative */}
          {structuredExplanation && (
            <div className="space-y-3 bg-slate-950/90 border border-purple-900/40 rounded-xl p-4">
              <div className="flex items-center justify-between border-b border-purple-900/30 pb-2">
                <h4 className="text-xs font-bold uppercase tracking-wider text-purple-300 flex items-center gap-1.5">
                  <Sparkles className="w-4 h-4 text-purple-400" />
                  Gemini Clinical Decision-Support Explanation (Advisory Only)
                </h4>
                <span className="text-[10px] px-2 py-0.5 rounded bg-purple-950 text-purple-300 border border-purple-800/50 font-mono">
                  Non-Critical AI
                </span>
              </div>

              {/* 1. Summary */}
              <div>
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block mb-1">
                  Summary:
                </span>
                <p className="text-xs text-slate-200 leading-relaxed font-sans bg-slate-900/80 p-2.5 rounded-lg border border-slate-800">
                  {structuredExplanation.summary}
                </p>
              </div>

              {/* 2. Supporting Evidence & 3. Missing Data */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block mb-1">
                    Supporting Derived Evidence:
                  </span>
                  <ul className="list-disc list-inside space-y-1 text-slate-300 bg-slate-900/60 p-2 rounded-lg border border-slate-800 text-[11px]">
                    {structuredExplanation.supporting_evidence.map((item, idx) => (
                      <li key={idx} className="leading-snug">{item}</li>
                    ))}
                  </ul>
                </div>

                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block mb-1">
                    Missing / Degraded Modalities:
                  </span>
                  <div className="bg-slate-900/60 p-2 rounded-lg border border-slate-800 flex flex-wrap gap-1.5">
                    {structuredExplanation.missing_data.map((item, idx) => (
                      <span key={idx} className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700 font-mono">
                        {item}
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              {/* 4. Recommended Human Review */}
              <div>
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block mb-1">
                  Recommended Bedside Human Review:
                </span>
                <div className="space-y-1 bg-slate-900/80 p-2.5 rounded-lg border border-slate-800 text-xs">
                  {structuredExplanation.recommended_human_review.map((item, idx) => (
                    <div key={idx} className="flex items-start gap-1.5 text-slate-300 text-[11px]">
                      <span className="text-emerald-400 font-bold shrink-0">✓</span>
                      <span>{item}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* 5. Limitations & Safety Boundaries */}
              <div className="p-2.5 rounded-lg bg-amber-950/20 border border-amber-900/40 text-[11px] text-amber-200/90 space-y-1">
                <span className="text-[10px] font-bold uppercase tracking-wider text-amber-400 block">
                  Mandatory Safety Boundaries &amp; Limitations:
                </span>
                <ul className="list-disc list-inside space-y-0.5">
                  {structuredExplanation.limitations.map((lim, idx) => (
                    <li key={idx}>{lim}</li>
                  ))}
                </ul>
                <div className="text-[10px] text-amber-400/80 pt-1 font-semibold">
                  Gemini does not diagnose patients, claim falls occurred, or modify alert priority.
                </div>
              </div>
            </div>
          )}

          {/* Audit Notes Timeline */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1.5">
              <FileText className="w-3.5 h-3.5 text-slate-400" />
              Auditable Event Log
            </h4>
            <div className="space-y-1.5 max-h-36 overflow-y-auto">
              {auditNotes.map((n, i) => (
                <div key={i} className="text-xs p-2 rounded-lg bg-slate-950 border border-slate-800/80 font-mono">
                  <span className="text-indigo-400 font-semibold">{n.author || 'System'}:</span>{' '}
                  <span className="text-slate-300">{n.text}</span>
                  <span className="text-[10px] text-slate-500 block mt-0.5">{new Date(n.ts).toLocaleTimeString()}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Action Controls Footer */}
        <div className="pt-4 border-t border-slate-800 shrink-0 flex flex-col gap-3">
          {event.status === 'DETECTED' && (
            <button
              onClick={() => {
                onAcknowledge(event.eventId);
                onClose();
              }}
              className="w-full py-2.5 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-sm shadow-md cursor-pointer transition-all flex items-center justify-center gap-2"
            >
              <UserCheck className="w-4 h-4" />
              <span>Acknowledge Bedside Response (Nurse Sarah Jenkins)</span>
            </button>
          )}

          {(event.status === 'ACKNOWLEDGED' || event.status === 'INVESTIGATING' || event.status === 'ESCALATED') && (
            <div className="space-y-2">
              <input
                type="text"
                placeholder="Add clinical observation or resolution note..."
                value={actionNote}
                onChange={(e) => setActionNote(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500"
              />

              <div className="flex flex-wrap gap-2 justify-end">
                <button
                  onClick={() => {
                    onEscalate(event.eventId, actionNote || 'Escalated by primary nurse for physician review');
                    onClose();
                  }}
                  className="px-3 py-2 text-xs font-semibold rounded-xl bg-purple-600/90 hover:bg-purple-500 text-white cursor-pointer transition-all flex items-center gap-1.5"
                >
                  <ArrowUpRight className="w-3.5 h-3.5" />
                  <span>Escalate to Charge Nurse</span>
                </button>

                <button
                  onClick={() => {
                    onResolve(event.eventId, actionNote || 'Patient safely repositioned in bed. Side rails secured.');
                    onClose();
                  }}
                  className="px-4 py-2 text-xs font-bold rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white shadow-sm cursor-pointer transition-all flex items-center gap-1.5"
                >
                  <CheckCircle className="w-4 h-4" />
                  <span>Resolve &amp; Close Alert</span>
                </button>
              </div>
            </div>
          )}

          {/* Dismiss option */}
          <div className="flex justify-between items-center text-xs text-slate-500 pt-1">
            {!showDismissInput ? (
              <button
                onClick={() => setShowDismissInput(true)}
                className="text-slate-500 hover:text-slate-400 underline cursor-pointer"
              >
                False Positive Override / Dismiss
              </button>
            ) : (
              <div className="flex items-center gap-2 w-full">
                <input
                  type="text"
                  placeholder="Reason for dismissal..."
                  value={dismissReason}
                  onChange={(e) => setDismissReason(e.target.value)}
                  className="grow bg-slate-950 border border-slate-800 rounded px-2 py-1 text-xs text-slate-300"
                />
                <button
                  onClick={() => {
                    if (dismissReason) {
                      onDismiss(event.eventId, dismissReason);
                      onClose();
                    }
                  }}
                  className="px-2 py-1 bg-rose-900/60 text-rose-300 text-xs rounded hover:bg-rose-800 cursor-pointer"
                >
                  Confirm Override
                </button>
              </div>
            )}
            <button onClick={onClose} className="hover:text-slate-300 cursor-pointer ml-auto">
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
