import React from 'react';
import { Staff } from '../types/wardsentinel';
import { Users, UserCheck, Shield, MapPin, Briefcase } from 'lucide-react';

interface NurseWorkloadPanelProps {
  staffList: Staff[];
}

export const NurseWorkloadPanel: React.FC<NurseWorkloadPanelProps> = ({ staffList }) => {
  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
            <Users className="w-5 h-5 text-indigo-400" />
            Clinical Nursing Staff &amp; Real-Time Workload
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Hard constraints verification: on-shift, available, ward eligibility with workload balancing
          </p>
        </div>

        <div className="flex items-center gap-2 text-xs">
          <span className="px-2.5 py-1 rounded-full bg-emerald-950 text-emerald-300 border border-emerald-800/50 flex items-center gap-1.5 font-medium">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            {staffList.filter((s) => s.onShift && s.available).length} Available for Dispatch
          </span>
        </div>
      </div>

      {/* Staff Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {staffList.map((nurse) => {
          const maxCapacity = 3;
          const loadPercent = Math.min((nurse.activeAssignmentsCount / maxCapacity) * 100, 100);

          return (
            <div
              key={nurse.staffId}
              className={`bg-slate-900/90 border rounded-xl p-4 flex flex-col justify-between transition-all ${
                !nurse.available
                  ? 'border-slate-800 opacity-60'
                  : nurse.role === 'CHARGE_NURSE'
                  ? 'border-purple-800/60 shadow-sm'
                  : 'border-slate-800 hover:border-slate-700'
              }`}
            >
              <div>
                {/* Header */}
                <div className="flex items-start justify-between gap-2 mb-2">
                  <div className="flex items-center gap-2">
                    <div className="w-8 h-8 rounded-full bg-indigo-950 text-indigo-300 border border-indigo-800/50 flex items-center justify-center font-bold text-xs">
                      {nurse.name.split(' ')[1]?.[0] || nurse.name[0]}
                    </div>
                    <div>
                      <h4 className="text-xs font-bold text-white leading-tight">{nurse.name}</h4>
                      <span className="text-[10px] text-slate-400 font-mono flex items-center gap-1">
                        <Shield className="w-2.5 h-2.5 text-slate-500" />
                        {nurse.role.replace('_', ' ')}
                      </span>
                    </div>
                  </div>

                  <span
                    className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${
                      nurse.onShift && nurse.available
                        ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                        : nurse.onShift
                        ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                        : 'bg-slate-800 text-slate-400'
                    }`}
                  >
                    {nurse.onShift && nurse.available ? 'AVAILABLE' : nurse.onShift ? 'BUSY' : 'OFF SHIFT'}
                  </span>
                </div>

                {/* Details */}
                <div className="space-y-1.5 text-xs text-slate-400 py-2 border-t border-slate-800/80 my-2">
                  <div className="flex justify-between items-center text-[11px]">
                    <span className="flex items-center gap-1 text-slate-500">
                      <Briefcase className="w-3 h-3" />
                      Eligible Wards:
                    </span>
                    <span className="font-mono text-slate-300">{nurse.eligibleWards}</span>
                  </div>

                  <div className="flex justify-between items-center text-[11px]">
                    <span className="flex items-center gap-1 text-slate-500">
                      <MapPin className="w-3 h-3" />
                      Current Location:
                    </span>
                    <span className="font-mono text-slate-300">
                      ({nurse.currentX.toFixed(1)}m, {nurse.currentY.toFixed(1)}m)
                    </span>
                  </div>
                </div>
              </div>

              {/* Workload Bar */}
              <div className="pt-2 border-t border-slate-800">
                <div className="flex justify-between text-[11px] mb-1">
                  <span className="text-slate-400">Active Alert Load</span>
                  <span className="font-mono font-bold text-slate-200">
                    {nurse.activeAssignmentsCount} / {maxCapacity} beds
                  </span>
                </div>

                <div className="w-full bg-slate-950 rounded-full h-1.5 overflow-hidden border border-slate-800">
                  <div
                    className={`h-full rounded-full transition-all duration-300 ${
                      nurse.activeAssignmentsCount >= 3
                        ? 'bg-rose-500'
                        : nurse.activeAssignmentsCount >= 2
                        ? 'bg-amber-500'
                        : 'bg-indigo-500'
                    }`}
                    style={{ width: `${loadPercent}%` }}
                  />
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
