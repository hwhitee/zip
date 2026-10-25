import React from 'react';
import { BedOverview, RiskLevel } from '../types/wardsentinel';
import { AlertCircle, UserCheck, ShieldAlert, Cpu, Sparkles, CheckCircle2 } from 'lucide-react';

interface BedCardProps {
  bed: BedOverview;
  onSelectEvent?: (eventId: string) => void;
  onInjectScenario?: (bedLabel: string) => void;
}

export const BedCard: React.FC<BedCardProps> = ({ bed, onSelectEvent, onInjectScenario }) => {
  const isOccupied = bed.status === 'OCCUPIED';
  const score = bed.latestRiskScore;

  const getRiskColor = (level: RiskLevel | null) => {
    switch (level) {
      case 'CRITICAL':
        return 'border-rose-500/70 bg-rose-950/20 text-rose-300 shadow-rose-950/40 shadow-lg';
      case 'HIGH':
        return 'border-amber-500/60 bg-amber-950/20 text-amber-300';
      case 'ELEVATED':
        return 'border-yellow-500/50 bg-yellow-950/15 text-yellow-300';
      case 'LOW':
        return 'border-emerald-500/40 bg-emerald-950/15 text-emerald-300';
      default:
        return 'border-slate-800 bg-slate-900/60 text-slate-400';
    }
  };

  const getRiskBadge = (level: RiskLevel | null) => {
    switch (level) {
      case 'CRITICAL':
        return 'bg-rose-500 text-white font-bold animate-pulse';
      case 'HIGH':
        return 'bg-amber-500 text-slate-950 font-semibold';
      case 'ELEVATED':
        return 'bg-yellow-500/90 text-slate-950 font-medium';
      case 'LOW':
        return 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30';
      default:
        return 'bg-slate-800 text-slate-400';
    }
  };

  return (
    <div
      className={`rounded-xl border p-4 transition-all duration-200 relative flex flex-col justify-between ${
        isOccupied ? getRiskColor(bed.latestRiskLevel) : 'border-slate-800 bg-slate-900/40 opacity-75'
      }`}
    >
      {/* Top Header Row */}
      <div className="flex items-start justify-between gap-2 mb-3">
        <div className="flex items-center gap-2">
          <span className="text-base font-bold font-mono text-white bg-slate-800 px-2 py-0.5 rounded border border-slate-700">
            Bed {bed.bedLabel}
          </span>
          <span
            className={`text-[10px] uppercase font-semibold px-2 py-0.5 rounded-full ${
              bed.status === 'OCCUPIED'
                ? 'bg-blue-500/20 text-blue-300 border border-blue-500/30'
                : bed.status === 'AVAILABLE'
                ? 'bg-slate-800 text-slate-300 border border-slate-700'
                : 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
            }`}
          >
            {bed.status}
          </span>
        </div>

        {isOccupied && score !== null && (
          <div className="flex flex-col items-end">
            <span className={`text-[11px] px-2 py-0.5 rounded-full ${getRiskBadge(bed.latestRiskLevel)}`}>
              {bed.latestRiskLevel || 'NORMAL'}
            </span>
            <span className="text-xs font-mono text-slate-300 font-semibold mt-1">
              Risk: {(score * 100).toFixed(0)}%
            </span>
          </div>
        )}
        {isOccupied && score === null && (
          <div className="flex flex-col items-end">
            <span className="text-[11px] px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 font-semibold">
              {bed.latestRiskLevel || 'DEGRADED'}
            </span>
            <span className="text-[10px] font-mono text-amber-400/90 mt-1">
              Score: Degraded / Manual
            </span>
          </div>
        )}
      </div>

      {/* Patient info / Occupancy details */}
      <div className="mb-3">
        {isOccupied ? (
          <div>
            <div className="text-sm font-semibold text-slate-200 truncate">
              {bed.patientDisplayName || `Patient #${bed.bedLabel}`}
            </div>
            <div className="text-xs font-mono text-slate-400 mt-0.5 flex items-center gap-1.5">
              <span>Token: {bed.currentPatientId ? `pt-${bed.currentPatientId.substring(0, 8)}` : 'N/A'}</span>
            </div>
          </div>
        ) : (
          <div className="text-xs text-slate-500 italic py-1">
            Bed unassigned — Telemetry inactive
          </div>
        )}
      </div>

      {/* Sensor stream & Risk Gauge */}
      {isOccupied && score !== null && (
        <div className="mb-3 space-y-1.5">
          <div className="flex justify-between text-[11px] text-slate-400">
            <span>Fall / Egress Score</span>
            <span className="font-mono text-slate-200">{score.toFixed(3)}</span>
          </div>

          <div className="w-full bg-slate-950 rounded-full h-2 overflow-hidden border border-slate-800">
            <div
              className={`h-full transition-all duration-500 rounded-full ${
                score >= 0.8
                  ? 'bg-rose-500'
                  : score >= 0.6
                  ? 'bg-amber-500'
                  : score >= 0.35
                  ? 'bg-yellow-400'
                  : 'bg-emerald-500'
              }`}
              style={{ width: `${Math.min(Math.max(score * 100, 4), 100)}%` }}
            />
          </div>

          <div className="flex items-center justify-between text-[10px] text-slate-400 pt-1">
            <span className="flex items-center gap-1">
              <Cpu className="w-3 h-3 text-indigo-400" />
              1.0 Hz Ambient Streams
            </span>
            <span className="text-emerald-400 flex items-center gap-0.5">
              <CheckCircle2 className="w-3 h-3" />
              Good Quality
            </span>
          </div>
        </div>
      )}

      {isOccupied && score === null && (
        <div className="mb-3 p-2.5 rounded-lg bg-amber-950/25 border border-amber-800/40 text-[11px] text-amber-200/90 flex items-center gap-2">
          <AlertCircle className="w-4 h-4 text-amber-400 shrink-0" />
          <span>Inference degraded or skipped. Deterministic bedside review triggered.</span>
        </div>
      )}

      {/* Active Safety Alert Section */}
      {bed.activeEventId ? (
        <div className="mt-2 p-2 rounded-lg bg-rose-950/40 border border-rose-800/60 flex flex-col gap-1.5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5 text-rose-300 text-xs font-bold">
              <AlertCircle className="w-3.5 h-3.5 text-rose-400 animate-bounce" />
              <span>ACTIVE ALERT ({bed.activeEventSeverity})</span>
            </div>
            {onSelectEvent && (
              <button
                onClick={() => onSelectEvent(bed.activeEventId!)}
                className="text-[10px] text-indigo-400 hover:text-indigo-300 underline font-semibold cursor-pointer"
              >
                Inspect
              </button>
            )}
          </div>

          <div className="flex items-center gap-1.5 text-[11px] text-slate-300">
            <UserCheck className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
            <span className="truncate">
              {bed.assignedNurseName || 'Dispatching nurse...'}
            </span>
          </div>
        </div>
      ) : isOccupied ? (
        <div className="mt-2 py-1 px-2 rounded bg-slate-950/50 border border-slate-800/60 flex items-center justify-between text-[11px] text-slate-400">
          <span className="flex items-center gap-1">
            <Sparkles className="w-3 h-3 text-slate-500" />
            Passive Monitoring
          </span>
          {onInjectScenario && (
            <button
              onClick={() => onInjectScenario(bed.bedLabel)}
              className="text-[10px] text-slate-400 hover:text-amber-400 transition-colors cursor-pointer"
              title="Test trigger"
            >
              Simulate Test
            </button>
          )}
        </div>
      ) : null}
    </div>
  );
};
