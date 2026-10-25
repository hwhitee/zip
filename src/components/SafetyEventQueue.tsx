import React, { useState } from 'react';
import { SafetyEvent, SafetyEventStatus } from '../types/wardsentinel';
import {
  AlertTriangle,
  CheckCircle,
  Clock,
  UserCheck,
  ArrowUpRight,
  Eye,
  ShieldAlert,
  HelpCircle,
} from 'lucide-react';

interface SafetyEventQueueProps {
  events: SafetyEvent[];
  onSelectEvent: (eventId: string) => void;
  onAcknowledge: (eventId: string) => void;
  onInvestigate: (eventId: string) => void;
  onEscalate: (eventId: string) => void;
  onResolve: (eventId: string) => void;
}

export const SafetyEventQueue: React.FC<SafetyEventQueueProps> = ({
  events,
  onSelectEvent,
  onAcknowledge,
  onInvestigate,
  onEscalate,
  onResolve,
}) => {
  const [statusFilter, setStatusFilter] = useState<'ACTIVE' | 'RESOLVED' | 'ALL'>('ACTIVE');

  const filteredEvents = events.filter((e) => {
    if (statusFilter === 'ACTIVE') {
      return e.status === 'DETECTED' || e.status === 'ACKNOWLEDGED' || e.status === 'INVESTIGATING' || e.status === 'ESCALATED';
    }
    if (statusFilter === 'RESOLVED') {
      return e.status === 'RESOLVED' || e.status === 'DISMISSED';
    }
    return true;
  });

  const getStatusBadge = (status: SafetyEventStatus) => {
    switch (status) {
      case 'DETECTED':
        return 'bg-rose-500/20 text-rose-300 border-rose-500/30 animate-pulse';
      case 'ACKNOWLEDGED':
        return 'bg-amber-500/20 text-amber-300 border-amber-500/30';
      case 'INVESTIGATING':
        return 'bg-indigo-500/20 text-indigo-300 border-indigo-500/30';
      case 'ESCALATED':
        return 'bg-purple-500/20 text-purple-300 border-purple-500/30';
      case 'RESOLVED':
        return 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30';
      case 'DISMISSED':
        return 'bg-slate-700 text-slate-400 border-slate-600';
    }
  };

  const getSeverityBadge = (severity: string) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-rose-600 text-white font-bold';
      case 'HIGH':
        return 'bg-amber-500 text-slate-950 font-bold';
      case 'ELEVATED':
        return 'bg-yellow-400 text-slate-950';
      default:
        return 'bg-slate-700 text-slate-300';
    }
  };

  return (
    <div className="space-y-4">
      {/* Queue Header & Filters */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-slate-900 border border-slate-800 rounded-xl p-4">
        <div>
          <h2 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-rose-400" />
            Prioritized Clinical Safety Queue
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Deterministic triage policy • Auto-dispatched nursing assignments with escalation tracking
          </p>
        </div>

        <div className="flex items-center gap-1 bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs">
          <button
            onClick={() => setStatusFilter('ACTIVE')}
            className={`px-3 py-1 rounded cursor-pointer transition-colors ${
              statusFilter === 'ACTIVE'
                ? 'bg-indigo-600 text-white font-semibold'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Active Alerts
          </button>
          <button
            onClick={() => setStatusFilter('RESOLVED')}
            className={`px-3 py-1 rounded cursor-pointer transition-colors ${
              statusFilter === 'RESOLVED'
                ? 'bg-indigo-600 text-white font-semibold'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Resolved
          </button>
          <button
            onClick={() => setStatusFilter('ALL')}
            className={`px-3 py-1 rounded cursor-pointer transition-colors ${
              statusFilter === 'ALL'
                ? 'bg-indigo-600 text-white font-semibold'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            All Events
          </button>
        </div>
      </div>

      {/* Events Table / List */}
      {filteredEvents.length === 0 ? (
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-8 text-center text-slate-400">
          <CheckCircle className="w-8 h-8 text-emerald-400 mx-auto mb-2 opacity-80" />
          <p className="font-semibold text-slate-200 text-sm">No alerts matching filter</p>
          <p className="text-xs text-slate-500 mt-1">All monitored patients in Ward 4B currently within resting baseline.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredEvents.map((event) => {
            let notesList: any[] = [];
            try {
              if (event.notes && event.notes.startsWith('[')) {
                notesList = JSON.parse(event.notes);
              }
            } catch {}

            const latestNote = notesList.length > 0 ? notesList[notesList.length - 1] : null;

            return (
              <div
                key={event.eventId}
                className={`bg-slate-900/90 border rounded-xl p-4 transition-all ${
                  event.severity === 'CRITICAL'
                    ? 'border-rose-700/80 shadow-md shadow-rose-950/30'
                    : event.severity === 'HIGH'
                    ? 'border-amber-700/60'
                    : 'border-slate-800'
                }`}
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  {/* Left Column: Severity, Bed, Patient */}
                  <div className="flex items-start gap-3">
                    <span className={`text-[11px] px-2.5 py-1 rounded-md uppercase tracking-wider ${getSeverityBadge(event.severity)}`}>
                      {event.severity}
                    </span>

                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-bold text-white text-sm">
                          Ward {event.wardId}
                        </span>
                        <span className="text-slate-500">•</span>
                        <span className="text-xs font-mono text-slate-300">
                          Patient pt-{event.patientId.substring(0, 8)}
                        </span>
                        <span className={`text-[10px] px-2 py-0.5 rounded-full border font-semibold ${getStatusBadge(event.status)}`}>
                          {event.status}
                        </span>
                      </div>

                      <div className="flex items-center gap-3 text-xs text-slate-400 mt-1">
                        <span className="flex items-center gap-1 font-mono">
                          <Clock className="w-3.5 h-3.5 text-slate-500" />
                          Detected {new Date(event.detectedAt).toLocaleTimeString()}
                        </span>

                        <span className="flex items-center gap-1">
                          <UserCheck className="w-3.5 h-3.5 text-indigo-400" />
                          Assigned: <strong className="text-slate-200">{event.assignedTo || 'Unassigned'}</strong>
                        </span>
                      </div>

                      {/* Audit Note snippet */}
                      {latestNote && (
                        <div className="text-xs text-slate-400 bg-slate-950/70 border border-slate-800/80 rounded px-2 py-1 mt-2 font-mono">
                          <span className="text-indigo-400 font-semibold">{latestNote.author}:</span> {latestNote.text}
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Right Column: FSM Action Buttons */}
                  <div className="flex flex-wrap items-center gap-2 self-center">
                    <button
                      onClick={() => onSelectEvent(event.eventId)}
                      className="px-2.5 py-1.5 text-xs font-semibold rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 flex items-center gap-1 cursor-pointer transition-colors"
                    >
                      <Eye className="w-3.5 h-3.5 text-indigo-400" />
                      <span>Evidence</span>
                    </button>

                    {event.status === 'DETECTED' && (
                      <button
                        onClick={() => onAcknowledge(event.eventId)}
                        className="px-3 py-1.5 text-xs font-bold rounded-lg bg-amber-500 hover:bg-amber-400 text-slate-950 shadow-sm cursor-pointer transition-all"
                      >
                        Acknowledge
                      </button>
                    )}

                    {event.status === 'ACKNOWLEDGED' && (
                      <>
                        <button
                          onClick={() => onInvestigate(event.eventId)}
                          className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white cursor-pointer transition-all"
                        >
                          Investigate
                        </button>
                        <button
                          onClick={() => onEscalate(event.eventId)}
                          className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-purple-600/90 hover:bg-purple-500 text-white cursor-pointer transition-all flex items-center gap-1"
                        >
                          <ArrowUpRight className="w-3.5 h-3.5" />
                          <span>Escalate</span>
                        </button>
                      </>
                    )}

                    {(event.status === 'INVESTIGATING' || event.status === 'ESCALATED') && (
                      <button
                        onClick={() => onResolve(event.eventId)}
                        className="px-3 py-1.5 text-xs font-bold rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white shadow-sm cursor-pointer transition-all flex items-center gap-1"
                      >
                        <CheckCircle className="w-3.5 h-3.5" />
                        <span>Resolve Alert</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
