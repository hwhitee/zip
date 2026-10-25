import React, { useState } from 'react';
import { PrivacyLedgerEntry, PrivacyVerificationResult } from '../types/wardsentinel';
import { ShieldCheck, CheckCircle, AlertTriangle, RefreshCw, Lock, Link, Info } from 'lucide-react';
import { apiService } from '../services/api';

interface PrivacyLedgerModalProps {
  entries: PrivacyLedgerEntry[];
  onRefresh: () => void;
  isLiveBackend: boolean;
}

export const PrivacyLedgerModal: React.FC<PrivacyLedgerModalProps> = ({ entries, onRefresh, isLiveBackend }) => {
  const [verification, setVerification] = useState<PrivacyVerificationResult | null>(null);
  const [isVerifying, setIsVerifying] = useState(false);

  const handleVerify = async () => {
    setIsVerifying(true);
    try {
      const res = await apiService.verifyPrivacyLedger();
      setVerification(res);
    } catch (e: any) {
      // NEVER claim verification succeeded on failure!
      setVerification({
        verified: false,
        totalEntriesChecked: 0,
        tamperedEntryId: null,
        message: `Cryptographic Verification Failed: Backend unreachable or offline (${e?.message || 'Connection refused'}). Offline mock data cannot authenticate hash chain integrity.`,
        securityLimitationsNotice: 'OFFLINE SAFETY BOUNDARY: Cryptographic audit certification strictly requires database access to compute iterative SHA-256 links. Verification cannot be confirmed offline.',
        verifiedAt: new Date().toISOString(),
      });
    } finally {
      setIsVerifying(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header & Verification Trigger */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-6 h-6 text-emerald-400" />
            <h2 className="text-base font-bold text-white tracking-tight">
              Auditable Privacy Ledger &amp; Cryptographic Hash Chain
            </h2>
          </div>
          <p className="text-xs text-slate-400 mt-1 max-w-2xl">
            Strict Data Minimization Guarantee: Zero raw audio streams, zero RGB video, and zero identifiable facial embeddings are collected or stored.
          </p>
        </div>

        <button
          onClick={handleVerify}
          disabled={isVerifying}
          className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs shadow-md shadow-indigo-950/40 flex items-center gap-2 cursor-pointer disabled:opacity-50 transition-all"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isVerifying ? 'animate-spin' : ''}`} />
          <span>{isVerifying ? 'Verifying Chain...' : 'Verify Cryptographic Integrity'}</span>
        </button>
      </div>

      {/* Verification Status Card */}
      {verification && (
        <div
          className={`p-4 rounded-xl border flex flex-col gap-2 ${
            verification.verified
              ? 'bg-emerald-950/30 border-emerald-800/60 text-emerald-200'
              : 'bg-rose-950/30 border-rose-800/60 text-rose-200'
          }`}
        >
          <div className="flex items-center gap-2 font-bold text-sm">
            {verification.verified ? (
              <CheckCircle className="w-5 h-5 text-emerald-400 shrink-0" />
            ) : (
              <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0" />
            )}
            <span>{verification.message}</span>
          </div>

          <div className="text-xs text-slate-300 font-mono mt-1">
            Checked: {verification.totalEntriesChecked} entries • Verified at: {new Date(verification.verifiedAt).toLocaleTimeString()}
          </div>

          {/* Security Limitation Notice */}
          <div className="mt-2 p-3 rounded-lg bg-slate-950/80 border border-slate-800/80 text-[11px] text-slate-400 leading-relaxed flex items-start gap-2">
            <Info className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
            <div>
              <strong className="text-slate-300">SECURITY BOUNDARY NOTICE:</strong> {verification.securityLimitationsNotice}
            </div>
          </div>
        </div>
      )}

      {/* Offline Demo Warning Strip */}
      {!isLiveBackend && (
        <div className="bg-amber-950/40 border border-amber-800/60 rounded-xl p-3 text-xs text-amber-300 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0" />
            <span>
              <strong>OFFLINE DEMO MODE:</strong> Showing local mock entries. Cryptographic SHA-256 verification requires connection to Spring Boot backend on port 8080.
            </span>
          </div>
          <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-amber-900/60 text-amber-200 uppercase font-semibold">
            Unverified Local State
          </span>
        </div>
      )}

      {/* Ledger Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-lg">
        <div className="px-4 py-3 border-b border-slate-800 flex justify-between items-center bg-slate-950/40">
          <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">
            Ledger Records ({entries.length} Entries) {isLiveBackend ? '(PostgreSQL Verified)' : '(Offline Demo)'}
          </span>
          <span className="text-[11px] text-slate-500 font-mono">
            Genesis Hash: 0000000000000000...
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950/80 text-slate-400 uppercase text-[10px] tracking-wider border-b border-slate-800">
              <tr>
                <th className="py-2.5 px-3">Timestamp</th>
                <th className="py-2.5 px-3">Patient Token</th>
                <th className="py-2.5 px-3">Audit Event</th>
                <th className="py-2.5 px-3 font-mono">SHA-256 Hash</th>
                <th className="py-2.5 px-3 font-mono">Previous Hash Link</th>
                <th className="py-2.5 px-3">Minimization Guarantee</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono text-slate-300">
              {entries.map((entry) => (
                <tr key={entry.entryId} className="hover:bg-slate-800/40 transition-colors">
                  <td className="py-2.5 px-3 text-[11px] whitespace-nowrap text-slate-400">
                    {new Date(entry.createdAt).toLocaleTimeString()}
                  </td>
                  <td className="py-2.5 px-3 text-[11px] font-bold text-indigo-300">
                    {entry.patientToken}
                  </td>
                  <td className="py-2.5 px-3 text-[11px] font-sans">
                    <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 text-[10px] border border-slate-700">
                      {entry.eventType}
                    </span>
                  </td>
                  <td className="py-2.5 px-3 text-[11px] text-emerald-400 truncate max-w-[130px]" title={entry.canonicalSha256}>
                    {entry.canonicalSha256.substring(0, 16)}...
                  </td>
                  <td className="py-2.5 px-3 text-[11px] text-slate-500 truncate max-w-[130px]" title={entry.prevHash}>
                    <span className="flex items-center gap-1">
                      <Link className="w-3 h-3 text-slate-600" />
                      {entry.prevHash.substring(0, 14)}...
                    </span>
                  </td>
                  <td className="py-2.5 px-3 text-[11px] font-sans text-slate-400 max-w-xs truncate" title={entry.dataMinimizationNote}>
                    {entry.dataMinimizationNote}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
