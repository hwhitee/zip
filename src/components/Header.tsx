import React from 'react';
import { Activity, ShieldCheck, RefreshCw, Radio, AlertTriangle } from 'lucide-react';
import { ConnectionStatus } from '../services/websocket';

interface HeaderProps {
  connectionStatus: ConnectionStatus;
  activeTab: string;
  onTabChange: (tab: string) => void;
  activeAlertsCount: number;
  criticalAlertsCount: number;
  onRefresh: () => void;
  isRefreshing: boolean;
  isLiveBackend: boolean;
}

export const Header: React.FC<HeaderProps> = ({
  connectionStatus,
  activeTab,
  onTabChange,
  activeAlertsCount,
  criticalAlertsCount,
  onRefresh,
  isRefreshing,
  isLiveBackend,
}) => {
  return (
    <header className="bg-slate-900 border-b border-slate-800 text-slate-100 sticky top-0 z-40 backdrop-blur-md bg-opacity-95">
      {/* Top Disclaimer & Status Strip */}
      <div className="bg-slate-950 px-4 py-1.5 text-xs flex flex-wrap items-center justify-between border-b border-slate-800/80 gap-2">
        <div className="flex items-center gap-2 text-slate-400">
          <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-semibold bg-indigo-950/80 text-indigo-400 border border-indigo-800/50">
            HACKATHON PROTOTYPE
          </span>
          {isLiveBackend ? (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-950/80 text-emerald-300 border border-emerald-700/60">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              BACKEND CONNECTED (SPRING BOOT + POSTGRES)
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-950/90 text-amber-300 border border-amber-700/60">
              <AlertTriangle className="w-3 h-3 text-amber-400" />
              OFFLINE / DEMO DATA ONLY (BACKEND UNREACHABLE)
            </span>
          )}
          <span className="text-slate-500">•</span>
          <span className="text-slate-400">
            Synthetic ambient sensors only — Heuristic demo score (not clinically validated)
          </span>
        </div>

        <div className="flex items-center gap-3">
          {/* Connection status badge */}
          <div className="flex items-center gap-1.5">
            <span
              className={`w-2 h-2 rounded-full ${
                connectionStatus === 'CONNECTED'
                  ? 'bg-emerald-400 animate-pulse'
                  : connectionStatus === 'CONNECTING' || connectionStatus === 'RECONNECTING'
                  ? 'bg-amber-400 animate-ping'
                  : 'bg-slate-500'
              }`}
            />
            <span className="text-[11px] font-mono text-slate-400">
              {connectionStatus === 'CONNECTED'
                ? 'WebSocket Live'
                : connectionStatus === 'RECONNECTING'
                ? 'Reconnecting WS...'
                : 'REST Standby'}
            </span>
          </div>

          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="flex items-center gap-1 text-slate-400 hover:text-slate-200 transition-colors cursor-pointer disabled:opacity-50"
            title="Refresh state from backend"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-indigo-400' : ''}`} />
            <span className="text-[11px]">Sync</span>
          </button>
        </div>
      </div>

      {/* Main Nav Header */}
      <div className="max-w-7xl mx-auto px-4 py-3 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 via-blue-600 to-indigo-800 flex items-center justify-center shadow-lg shadow-indigo-950/50 border border-indigo-400/30">
            <Activity className="w-6 h-6 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-bold tracking-tight text-white flex items-center gap-2">
                WardSentinel AI
              </h1>
              <span className="text-xs px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700 font-mono">
                Ward 4B (Acute Care)
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Autonomous Ambient Fall-Risk &amp; Bed-Exit Safety Orchestration
            </p>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav className="flex items-center gap-1 bg-slate-950/80 p-1 rounded-xl border border-slate-800">
          <button
            onClick={() => onTabChange('overview')}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-all cursor-pointer ${
              activeTab === 'overview'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
            }`}
          >
            Ward Overview (12 Beds)
          </button>

          <button
            onClick={() => onTabChange('events')}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-all flex items-center gap-1.5 cursor-pointer ${
              activeTab === 'events'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
            }`}
          >
            <span>Safety Queue</span>
            {activeAlertsCount > 0 && (
              <span
                className={`text-[10px] px-1.5 py-0.2 rounded-full font-bold ${
                  criticalAlertsCount > 0
                    ? 'bg-rose-500 text-white animate-pulse'
                    : 'bg-amber-500 text-slate-950'
                }`}
              >
                {activeAlertsCount}
              </span>
            )}
          </button>

          <button
            onClick={() => onTabChange('staff')}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-all cursor-pointer ${
              activeTab === 'staff'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
            }`}
          >
            Nurse Workload
          </button>

          <button
            onClick={() => onTabChange('privacy')}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-all flex items-center gap-1 cursor-pointer ${
              activeTab === 'privacy'
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
            }`}
          >
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
            <span>Privacy Ledger</span>
          </button>

          <button
            onClick={() => onTabChange('scenarios')}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-all flex items-center gap-1 cursor-pointer ${
              activeTab === 'scenarios'
                ? 'bg-amber-600/90 text-white shadow-sm'
                : 'text-amber-400/90 hover:text-amber-300 hover:bg-slate-900'
            }`}
          >
            <Radio className="w-3.5 h-3.5" />
            <span>Demo Scenarios</span>
          </button>
        </nav>
      </div>
    </header>
  );
};
