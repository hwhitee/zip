import React, { useState } from 'react';
import { DEMO_SCENARIOS, buildScenarioPayload } from '../services/scenarios';
import { Play, Sparkles, CheckCircle, AlertTriangle, Radio, Send, RefreshCw } from 'lucide-react';
import { apiService } from '../services/api';

interface DemoScenarioPanelProps {
  onScenarioInjected: () => void;
}

export const DemoScenarioPanel: React.FC<DemoScenarioPanelProps> = ({ onScenarioInjected }) => {
  const [selectedBed, setSelectedBed] = useState('04');
  const [activeScenarioId, setActiveScenarioId] = useState<string>('PROGRESSIVE_BED_EXIT');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [lastResult, setLastResult] = useState<any | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleRunScenario = async (scenarioId: string) => {
    setIsSubmitting(true);
    setErrorMessage(null);
    setLastResult(null);

    const payload = buildScenarioPayload(
      scenarioId,
      selectedBed,
      'WARD-4B',
      `e1f13b19-482d-4c3d-82d2-${selectedBed.padStart(12, '0')}`
    );

    try {
      const response = await apiService.submitTelemetryWindow(payload);
      setLastResult({
        scenario: scenarioId,
        bed: selectedBed,
        riskScore: response.risk_score ?? response.riskScore,
        riskLevel: response.risk_level ?? response.riskLevel,
        latencyMs: response.latency_ms ?? response.latencyMs,
        modelVersion: response.model_version ?? response.modelVersion,
        explanation: response.gemini_explanation ?? response.geminiExplanation,
        timestamp: new Date().toLocaleTimeString(),
      });
      onScenarioInjected();
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to submit telemetry window to backend');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Intro Header */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Radio className="w-5 h-5 text-amber-400" />
            <h2 className="text-base font-bold text-white tracking-tight">
              Clinical Demo Scenarios &amp; Live Telemetry Injection
            </h2>
          </div>
          <p className="text-xs text-slate-400 mt-1 max-w-2xl">
            Simulates real-world acute ward events by submitting 300-point derived sensor windows to the backend API (`POST /api/v1/telemetry/windows`).
          </p>
        </div>

        {/* Bed Target Selector */}
        <div className="flex items-center gap-2 bg-slate-950 p-2 rounded-xl border border-slate-800 text-xs">
          <span className="text-slate-400 font-medium">Target Bed:</span>
          <select
            value={selectedBed}
            onChange={(e) => setSelectedBed(e.target.value)}
            className="bg-slate-900 border border-slate-700 text-white font-mono rounded px-2.5 py-1 text-xs cursor-pointer focus:outline-none focus:border-indigo-500"
          >
            {Array.from({ length: 12 }, (_, i) => {
              const label = (i + 1).toString().padStart(2, '0');
              return (
                <option key={label} value={label}>
                  Bed {label}
                </option>
              );
            })}
          </select>
        </div>
      </div>

      {/* Scenarios Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {DEMO_SCENARIOS.map((sc) => {
          const isSelected = activeScenarioId === sc.id;
          return (
            <div
              key={sc.id}
              className={`bg-slate-900 border rounded-xl p-4 flex flex-col justify-between transition-all ${
                isSelected
                  ? 'border-indigo-500 bg-slate-900/90 shadow-lg shadow-indigo-950/30'
                  : 'border-slate-800 hover:border-slate-700'
              }`}
            >
              <div>
                <div className="flex items-center justify-between gap-2 mb-2">
                  <span className="text-xs font-bold uppercase tracking-wider px-2 py-0.5 rounded bg-slate-800 text-indigo-300 border border-slate-700">
                    {sc.category}
                  </span>
                  <span className="text-xs font-mono text-slate-500">ID: {sc.id}</span>
                </div>

                <h3 className="text-sm font-bold text-white mb-1">{sc.title}</h3>
                <p className="text-xs text-slate-300 leading-relaxed mb-3">{sc.description}</p>

                <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-400 mb-4">
                  <strong className="text-slate-300">Expected Triage:</strong> {sc.expectedOutcome}
                </div>
              </div>

              <button
                onClick={() => {
                  setActiveScenarioId(sc.id);
                  handleRunScenario(sc.id);
                }}
                disabled={isSubmitting}
                className="w-full py-2 px-3 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 transition-all shadow-md"
              >
                {isSubmitting && activeScenarioId === sc.id ? (
                  <>
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    <span>Transmitting 300-Sample Window...</span>
                  </>
                ) : (
                  <>
                    <Play className="w-3.5 h-3.5 fill-current" />
                    <span>Inject into Bed {selectedBed}</span>
                  </>
                )}
              </button>
            </div>
          );
        })}
      </div>

      {/* Response Display Box */}
      {lastResult && (
        <div className="bg-slate-900 border border-emerald-800/60 rounded-xl p-4 space-y-2 animate-fadeIn">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-emerald-400 flex items-center gap-1.5">
              <CheckCircle className="w-4 h-4" />
              Scenario Injected &amp; Scored via Python Engine
            </span>
            <span className="text-xs font-mono text-slate-400">{lastResult.timestamp}</span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs pt-1 font-mono">
            <div className="bg-slate-950 p-2 rounded border border-slate-800">
              <span className="text-slate-500 block text-[10px]">Bed Target</span>
              <span className="text-white font-bold">Bed {lastResult.bed}</span>
            </div>
            <div className="bg-slate-950 p-2 rounded border border-slate-800">
              <span className="text-slate-500 block text-[10px]">Computed Score</span>
              <span className="text-amber-400 font-bold">{(lastResult.riskScore * 100).toFixed(1)}%</span>
            </div>
            <div className="bg-slate-950 p-2 rounded border border-slate-800">
              <span className="text-slate-500 block text-[10px]">Triage Level</span>
              <span className="text-white font-bold">{lastResult.riskLevel}</span>
            </div>
            <div className="bg-slate-950 p-2 rounded border border-slate-800">
              <span className="text-slate-500 block text-[10px]">Processing Latency</span>
              <span className="text-slate-300">{lastResult.latencyMs} ms</span>
            </div>
          </div>

          {lastResult.explanation && (
            <div className="text-xs text-slate-300 bg-slate-950 p-2.5 rounded border border-slate-800 leading-relaxed font-sans">
              <strong className="text-purple-400">Diagnostic Summary:</strong> {lastResult.explanation}
            </div>
          )}
        </div>
      )}

      {errorMessage && (
        <div className="bg-rose-950/40 border border-rose-800/80 rounded-xl p-4 text-xs text-rose-300 flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 text-rose-400 shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}
    </div>
  );
};
