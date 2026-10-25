import React, { useEffect, useState, useCallback, useRef } from 'react';
import {
  BedOverview,
  InferenceEvidence,
  PrivacyLedgerEntry,
  SafetyEvent,
  Staff,
  WardOverview,
} from './types/wardsentinel';
import { apiService, INITIAL_FALLBACK_BEDS, INITIAL_FALLBACK_EVENTS, INITIAL_FALLBACK_STAFF } from './services/api';
import { webSocketService, ConnectionStatus } from './services/websocket';
import { Header } from './components/Header';
import { WardOverviewGrid } from './components/WardOverviewGrid';
import { SafetyEventQueue } from './components/SafetyEventQueue';
import { EventDetailModal } from './components/EventDetailModal';
import { NurseWorkloadPanel } from './components/NurseWorkloadPanel';
import { PrivacyLedgerModal } from './components/PrivacyLedgerModal';
import { DemoScenarioPanel } from './components/DemoScenarioPanel';
import { AlertCircle, RefreshCw, WifiOff } from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<'overview' | 'events' | 'staff' | 'privacy' | 'scenarios'>('overview');
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('DISCONNECTED');
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isLiveBackend, setIsLiveBackend] = useState(false);

  // Core Data States
  const [wardData, setWardData] = useState<WardOverview>({
    wardId: 'WARD-4B',
    totalBeds: 12,
    occupiedBeds: 10,
    activeEventsCount: 2,
    criticalEventsCount: 1,
    onShiftStaffCount: 4,
    beds: INITIAL_FALLBACK_BEDS,
    isOfflineFallback: true,
  });

  const [events, setEvents] = useState<SafetyEvent[]>(INITIAL_FALLBACK_EVENTS);
  const [staffList, setStaffList] = useState<Staff[]>(INITIAL_FALLBACK_STAFF);
  const [privacyEntries, setPrivacyEntries] = useState<PrivacyLedgerEntry[]>([]);

  // Selected Event Modal State
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);
  const [selectedEvidence, setSelectedEvidence] = useState<InferenceEvidence | null>(null);
  const [isLoadingEvidence, setIsLoadingEvidence] = useState(false);

  // Notification Toast State
  const [bannerNotice, setBannerNotice] = useState<string | null>(null);
  const processedAlertIdsRef = useRef<Set<string>>(new Set());

  // ── Fetch all data from backend ──────────────────────────────────────────────
  const loadWardData = useCallback(async () => {
    setIsRefreshing(true);
    try {
      const [overview, evs, staff, ledger] = await Promise.all([
        apiService.getWardOverview('WARD-4B'),
        apiService.listEvents('WARD-4B'),
        apiService.getStaffAvailability(),
        apiService.getPrivacyLedger(),
      ]);

      setWardData(overview);
      setEvents(evs);
      setStaffList(staff);
      setPrivacyEntries(ledger);
      setIsLiveBackend(!overview.isOfflineFallback);
    } catch (err) {
      console.warn('Backend sync failed, maintaining current state:', err);
      setIsLiveBackend(false);
    } finally {
      setIsRefreshing(false);
    }
  }, []);

  // ── WebSocket & Initialization ───────────────────────────────────────────────
  useEffect(() => {
    loadWardData();

    // Connect to STOMP WebSocket
    webSocketService.connect('WARD-4B');

    const unsubStatus = webSocketService.onStatus((status) => {
      setConnectionStatus(status);
      if (status === 'CONNECTED') {
        // Refresh state after reconnect
        loadWardData();
      }
    });

    // Real-time Safety Event Handler (with duplicate suppression)
    const unsubEvents = webSocketService.onEvent((incomingEvent) => {
      const key = `${incomingEvent.eventId}-${incomingEvent.status}`;
      if (processedAlertIdsRef.current.has(key)) return;
      processedAlertIdsRef.current.add(key);

      setEvents((prev) => {
        const index = prev.findIndex((e) => e.eventId === incomingEvent.eventId);
        if (index >= 0) {
          const updated = [...prev];
          updated[index] = incomingEvent;
          return updated;
        } else {
          return [incomingEvent, ...prev];
        }
      });

      // Show toast banner
      setBannerNotice(`Alert update: ${incomingEvent.severity} on Bed ${incomingEvent.wardId}`);
      setTimeout(() => setBannerNotice(null), 6000);

      // Refresh overview counts
      apiService.getWardOverview('WARD-4B').then(setWardData).catch(() => {});
    });

    // Real-time Risk Score Stream Handler
    const unsubRisk = webSocketService.onRisk((riskData) => {
      if (riskData.patient_id) {
        setWardData((prev) => ({
          ...prev,
          beds: prev.beds.map((b) =>
            b.currentPatientId === riskData.patient_id
              ? {
                  ...b,
                  latestRiskScore: riskData.risk_score ?? b.latestRiskScore,
                  latestRiskLevel: riskData.risk_level ?? b.latestRiskLevel,
                }
              : b
          ),
        }));
      }
    });

    return () => {
      unsubStatus();
      unsubEvents();
      unsubRisk();
      webSocketService.disconnect();
    };
  }, [loadWardData]);

  // ── Load Evidence when modal opens ─────────────────────────────────────────
  useEffect(() => {
    if (!selectedEventId) {
      setSelectedEvidence(null);
      return;
    }
    setIsLoadingEvidence(true);
    apiService
      .getEventEvidence(selectedEventId)
      .then(setSelectedEvidence)
      .catch((e) => console.warn('Could not load evidence:', e))
      .finally(() => setIsLoadingEvidence(false));
  }, [selectedEventId]);

  // ── Action Handlers ────────────────────────────────────────────────────────
  const handleAcknowledge = async (eventId: string) => {
    try {
      const updated = await apiService.acknowledgeEvent(eventId, 'Nurse Sarah Jenkins, RN', 'Nurse responding at bedside');
      setEvents((prev) => prev.map((e) => (e.eventId === eventId ? updated : e)));
      loadWardData();
      setBannerNotice(`Event ${eventId.substring(0, 8)} acknowledged by Nurse Sarah Jenkins.`);
      setTimeout(() => setBannerNotice(null), 4000);
    } catch (err: any) {
      // Never claim action succeeded or persisted if the request failed!
      setBannerNotice(`Persistence Error: Acknowledge request rejected or backend unreachable (${err.message || 'Offline'}). Action was NOT saved.`);
      setTimeout(() => setBannerNotice(null), 6000);
    }
  };

  const handleInvestigate = async (eventId: string) => {
    try {
      const updated = await apiService.investigateEvent(eventId, 'Primary nurse conducting neurological fall-risk evaluation');
      setEvents((prev) => prev.map((e) => (e.eventId === eventId ? updated : e)));
      loadWardData();
      setBannerNotice(`Event ${eventId.substring(0, 8)} marked INVESTIGATING.`);
      setTimeout(() => setBannerNotice(null), 4000);
    } catch (err: any) {
      setBannerNotice(`Persistence Error: Investigate request failed (${err.message || 'Offline'}). Action was NOT saved.`);
      setTimeout(() => setBannerNotice(null), 6000);
    }
  };

  const handleEscalate = async (eventId: string, note?: string) => {
    try {
      const updated = await apiService.escalateEvent(
        eventId,
        note || 'Escalated by primary nurse to Charge Nurse',
        'Charge Nurse Elena Rostova, BSN'
      );
      setEvents((prev) => prev.map((e) => (e.eventId === eventId ? updated : e)));
      loadWardData();
      setBannerNotice(`Event ${eventId.substring(0, 8)} ESCALATED to Charge Nurse Elena Rostova.`);
      setTimeout(() => setBannerNotice(null), 4000);
    } catch (err: any) {
      setBannerNotice(`Persistence Error: Escalation failed (${err.message || 'Offline'}). Action was NOT saved.`);
      setTimeout(() => setBannerNotice(null), 6000);
    }
  };

  const handleResolve = async (eventId: string, note?: string) => {
    try {
      const updated = await apiService.resolveEvent(
        eventId,
        note || 'Patient safely assisted. Egress sensors calibrated and bed perimeter verified.',
        'Nurse Sarah Jenkins, RN'
      );
      setEvents((prev) => prev.map((e) => (e.eventId === eventId ? updated : e)));
      loadWardData();
      setBannerNotice(`Event ${eventId.substring(0, 8)} RESOLVED.`);
      setTimeout(() => setBannerNotice(null), 4000);
    } catch (err: any) {
      setBannerNotice(`Persistence Error: Resolution failed (${err.message || 'Offline'}). Action was NOT saved.`);
      setTimeout(() => setBannerNotice(null), 6000);
    }
  };

  const handleDismiss = async (eventId: string, reason: string) => {
    try {
      const updated = await apiService.dismissEvent(eventId, reason);
      setEvents((prev) => prev.map((e) => (e.eventId === eventId ? updated : e)));
      loadWardData();
      setBannerNotice(`Event ${eventId.substring(0, 8)} DISMISSED (false-positive override).`);
      setTimeout(() => setBannerNotice(null), 4000);
    } catch (err: any) {
      setBannerNotice(`Persistence Error: Dismissal failed (${err.message || 'Offline'}). Action was NOT saved.`);
      setTimeout(() => setBannerNotice(null), 6000);
    }
  };

  const selectedEvent = events.find((e) => e.eventId === selectedEventId) || null;

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      {/* Header */}
      <Header
        connectionStatus={connectionStatus}
        activeTab={activeTab}
        onTabChange={(tab: any) => setActiveTab(tab)}
        activeAlertsCount={wardData.activeEventsCount}
        criticalAlertsCount={wardData.criticalEventsCount}
        onRefresh={loadWardData}
        isRefreshing={isRefreshing}
        isLiveBackend={isLiveBackend}
      />

      {/* Reconnecting / Offline Notice Banner */}
      {(!isLiveBackend || connectionStatus === 'DISCONNECTED') && (
        <div className="bg-slate-900 border-b border-amber-900/60 px-4 py-2 text-xs text-amber-300 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <WifiOff className="w-4 h-4 text-amber-400 shrink-0" />
            <span>
              <strong>System Mode:</strong> {!isLiveBackend ? 'Offline Fallback Active (Local synthetic data). Backend API not detected.' : 'Live WebSocket reconnecting to /ws. REST sync operational.'}
            </span>
          </div>
          <button
            onClick={() => {
              webSocketService.connect('WARD-4B');
              loadWardData();
            }}
            className="text-[11px] font-semibold underline text-amber-400 hover:text-amber-200 cursor-pointer"
          >
            Retry Connection &amp; Sync
          </button>
        </div>
      )}

      {/* Floating Alert Toast */}
      {bannerNotice && (
        <div className="fixed bottom-5 right-5 z-50 bg-rose-950 border border-rose-700 text-rose-100 px-4 py-3 rounded-xl shadow-2xl flex items-center gap-3 animate-slideIn">
          <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />
          <span className="text-xs font-semibold">{bannerNotice}</span>
        </div>
      )}

      {/* Main Content Area */}
      <main className="max-w-7xl w-full mx-auto px-4 py-6 grow">
        {activeTab === 'overview' && (
          <WardOverviewGrid
            wardData={wardData}
            onSelectEvent={setSelectedEventId}
            onTriggerScenario={(scenarioId, bedLabel) => {
              setActiveTab('scenarios');
            }}
          />
        )}

        {activeTab === 'events' && (
          <SafetyEventQueue
            events={events}
            onSelectEvent={setSelectedEventId}
            onAcknowledge={handleAcknowledge}
            onInvestigate={handleInvestigate}
            onEscalate={(id) => handleEscalate(id)}
            onResolve={(id) => handleResolve(id)}
          />
        )}

        {activeTab === 'staff' && <NurseWorkloadPanel staffList={staffList} />}

        {activeTab === 'privacy' && (
          <PrivacyLedgerModal entries={privacyEntries} onRefresh={loadWardData} isLiveBackend={isLiveBackend} />
        )}

        {activeTab === 'scenarios' && (
          <DemoScenarioPanel onScenarioInjected={loadWardData} />
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-900 bg-slate-950/80 px-4 py-4 text-xs text-slate-500 text-center">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-2">
          <span>WardSentinel AI • Clinical Decision-Support Architecture</span>
          <span className="text-slate-600">
            PostgreSQL 16 • Flyway Migrations • Spring WebSocket STOMP • Python FastAPI Heuristic Engine
          </span>
        </div>
      </footer>

      {/* Event Detail Inspection Modal */}
      {selectedEvent && (
        <EventDetailModal
          event={selectedEvent}
          evidence={selectedEvidence}
          isLoadingEvidence={isLoadingEvidence}
          onClose={() => setSelectedEventId(null)}
          onAcknowledge={handleAcknowledge}
          onEscalate={handleEscalate}
          onResolve={handleResolve}
          onDismiss={handleDismiss}
        />
      )}
    </div>
  );
}
