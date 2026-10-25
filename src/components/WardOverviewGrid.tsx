import React, { useState } from 'react';
import { WardOverview } from '../types/wardsentinel';
import { BedCard } from './BedCard';
import { Activity, AlertTriangle, Users, Bed, ShieldAlert } from 'lucide-react';

interface WardOverviewGridProps {
  wardData: WardOverview;
  onSelectEvent: (eventId: string) => void;
  onTriggerScenario: (scenarioId: string, bedLabel: string) => void;
}

export const WardOverviewGrid: React.FC<WardOverviewGridProps> = ({
  wardData,
  onSelectEvent,
  onTriggerScenario,
}) => {
  const [filter, setFilter] = useState<'ALL' | 'ALERTS' | 'OCCUPIED' | 'AVAILABLE'>('ALL');

  const filteredBeds = wardData.beds.filter((bed) => {
    if (filter === 'ALERTS') return bed.activeEventId !== null;
    if (filter === 'OCCUPIED') return bed.status === 'OCCUPIED';
    if (filter === 'AVAILABLE') return bed.status === 'AVAILABLE';
    return true;
  });

  return (
    <div className="space-y-6">
      {/* Ward Metric Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-3.5 flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-blue-950/60 text-blue-400 border border-blue-800/40 flex items-center justify-center shrink-0">
            <Bed className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs text-slate-400">Total Ward Beds</div>
            <div className="text-xl font-bold font-mono text-white">{wardData.totalBeds}</div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-3.5 flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-indigo-950/60 text-indigo-400 border border-indigo-800/40 flex items-center justify-center shrink-0">
            <Activity className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs text-slate-400">Occupied Census</div>
            <div className="text-xl font-bold font-mono text-white">
              {wardData.occupiedBeds} <span className="text-xs font-normal text-slate-400">/ {wardData.totalBeds}</span>
            </div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-3.5 flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-amber-950/60 text-amber-400 border border-amber-800/40 flex items-center justify-center shrink-0">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs text-slate-400">Active Safety Alerts</div>
            <div className="text-xl font-bold font-mono text-amber-300">
              {wardData.activeEventsCount}
            </div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-3.5 flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-rose-950/60 text-rose-400 border border-rose-800/40 flex items-center justify-center shrink-0">
            <ShieldAlert className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs text-slate-400">Critical Priority</div>
            <div className="text-xl font-bold font-mono text-rose-400">
              {wardData.criticalEventsCount}
            </div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-3.5 flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-emerald-950/60 text-emerald-400 border border-emerald-800/40 flex items-center justify-center shrink-0">
            <Users className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs text-slate-400">On-Shift Staff</div>
            <div className="text-xl font-bold font-mono text-emerald-300">
              {wardData.onShiftStaffCount}
            </div>
          </div>
        </div>
      </div>

      {/* Filter and Bed Grid Header */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <h2 className="text-base font-bold text-white tracking-tight">
            Acute Ward Census (12 Monitored Beds)
          </h2>
          {wardData.isOfflineFallback && (
            <span className="text-[10px] px-2 py-0.5 rounded bg-amber-950/80 text-amber-300 border border-amber-800/60 font-semibold">
              OFFLINE DEMO DATA
            </span>
          )}
          <span className="text-xs text-slate-400">
            Showing {filteredBeds.length} of {wardData.beds.length} beds
          </span>
        </div>

        <div className="flex items-center gap-1 bg-slate-900 border border-slate-800 p-1 rounded-lg text-xs">
          <button
            onClick={() => setFilter('ALL')}
            className={`px-2.5 py-1 rounded cursor-pointer transition-colors ${
              filter === 'ALL' ? 'bg-slate-800 text-white font-semibold' : 'text-slate-400 hover:text-white'
            }`}
          >
            All Beds
          </button>
          <button
            onClick={() => setFilter('ALERTS')}
            className={`px-2.5 py-1 rounded cursor-pointer transition-colors flex items-center gap-1 ${
              filter === 'ALERTS' ? 'bg-rose-950 text-rose-300 font-semibold border border-rose-800/60' : 'text-slate-400 hover:text-white'
            }`}
          >
            <span>Alerts</span>
            {wardData.activeEventsCount > 0 && (
              <span className="w-4 h-4 rounded-full bg-rose-500 text-white text-[10px] flex items-center justify-center font-bold">
                {wardData.activeEventsCount}
              </span>
            )}
          </button>
          <button
            onClick={() => setFilter('OCCUPIED')}
            className={`px-2.5 py-1 rounded cursor-pointer transition-colors ${
              filter === 'OCCUPIED' ? 'bg-slate-800 text-white font-semibold' : 'text-slate-400 hover:text-white'
            }`}
          >
            Occupied
          </button>
          <button
            onClick={() => setFilter('AVAILABLE')}
            className={`px-2.5 py-1 rounded cursor-pointer transition-colors ${
              filter === 'AVAILABLE' ? 'bg-slate-800 text-white font-semibold' : 'text-slate-400 hover:text-white'
            }`}
          >
            Available
          </button>
        </div>
      </div>

      {/* 12-Bed Responsive Layout */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        {filteredBeds.map((bed) => (
          <BedCard
            key={bed.bedId}
            bed={bed}
            onSelectEvent={onSelectEvent}
            onInjectScenario={(bedLabel) => onTriggerScenario('PROGRESSIVE_BED_EXIT', bedLabel)}
          />
        ))}
      </div>
    </div>
  );
};
