import { useRouter } from "next/router";
import { useEffect, useState } from "react";
import Layout from "../../components/Layout";
import { apiGet } from "../../lib/api";

interface TraceEvent {
  eventId: string;
  eventType: string;
  sourceService?: string;
  createdAt?: string;
  payloadJson?: string;
}

interface CausalEdge {
  cause_event_id?: string;
  effect_event_id?: string;
  relation_type?: string;
  confidence?: number;
}

interface ExplainPayload {
  events?: TraceEvent[];
  eventTypeCounts?: Record<string, number>;
  sourceServiceCounts?: Record<string, number>;
  causalEdges?: CausalEdge[];
  directorTrace?: unknown[];
  toolCalls?: unknown[];
  stateEffects?: unknown[];
  banditDecisions?: unknown[];
  driftDecisions?: unknown[];
  safetyReport?: unknown[];
}

export default function TraceReplayPage() {
  const router = useRouter();
  const { traceId } = router.query;
  const [events, setEvents] = useState<TraceEvent[]>([]);
  const [eventTypeCounts, setEventTypeCounts] = useState<Record<string, number>>({});
  const [sourceServiceCounts, setSourceServiceCounts] = useState<Record<string, number>>({});
  const [causalEdges, setCausalEdges] = useState<CausalEdge[]>([]);
  const [directorTrace, setDirectorTrace] = useState<unknown[]>([]);
  const [toolCalls, setToolCalls] = useState<unknown[]>([]);
  const [stateEffects, setStateEffects] = useState<unknown[]>([]);
  const [banditDecisions, setBanditDecisions] = useState<unknown[]>([]);
  const [driftDecisions, setDriftDecisions] = useState<unknown[]>([]);
  const [safetyReport, setSafetyReport] = useState<unknown[]>([]);
  const [status, setStatus] = useState<string>("");

  useEffect(() => {
    if (typeof traceId !== "string" || !traceId.trim()) {
      return;
    }
    const load = async () => {
      try {
        setStatus("Loading trace...");
        const res = await apiGet<ExplainPayload>(`/api/v2/traces/${traceId}/explain`);
        const data = (res?.data || {}) as ExplainPayload;
        setEvents(Array.isArray(data.events) ? data.events : []);
        setEventTypeCounts(data.eventTypeCounts || {});
        setSourceServiceCounts(data.sourceServiceCounts || {});
        setCausalEdges(Array.isArray(data.causalEdges) ? data.causalEdges : []);
        setDirectorTrace(Array.isArray(data.directorTrace) ? data.directorTrace : []);
        setToolCalls(Array.isArray(data.toolCalls) ? data.toolCalls : []);
        setStateEffects(Array.isArray(data.stateEffects) ? data.stateEffects : []);
        setBanditDecisions(Array.isArray(data.banditDecisions) ? data.banditDecisions : []);
        setDriftDecisions(Array.isArray(data.driftDecisions) ? data.driftDecisions : []);
        setSafetyReport(Array.isArray(data.safetyReport) ? data.safetyReport : []);
        setStatus("");
      } catch (error) {
        setStatus("Failed to load trace replay.");
      }
    };
    load();
  }, [traceId]);

  return (
    <Layout>
      <div className="grid grid-2">
        <div className="card">
          <h2 className="section-title">Timeline</h2>
          <p className="hint">Trace ID: {typeof traceId === "string" ? traceId : ""}</p>
          {status && <p className="hint">{status}</p>}
          {!status && events.length === 0 && <p className="empty-state">No events.</p>}
          <div className="list">
            {events.map((event) => (
              <div key={event.eventId} className="list-item">
                <div>
                  <div style={{ fontWeight: 600 }}>{event.eventType}</div>
                  <div className="hint">{event.sourceService || "unknown service"}</div>
                </div>
                <div style={{ maxWidth: 320, overflowWrap: "anywhere" }}>{event.payloadJson || "{}"}</div>
              </div>
            ))}
          </div>
        </div>
        <div className="card">
          <h2 className="section-title">Summary</h2>
          <h3 style={{ marginTop: 8 }}>By Event Type</h3>
          <div className="list">
            {Object.keys(eventTypeCounts).length === 0 && <p className="empty-state">No counts.</p>}
            {Object.entries(eventTypeCounts).map(([key, value]) => (
              <div key={key} className="list-item">
                <span>{key}</span>
                <span>{value}</span>
              </div>
            ))}
          </div>
          <h3 style={{ marginTop: 16 }}>By Source Service</h3>
          <div className="list">
            {Object.keys(sourceServiceCounts).length === 0 && <p className="empty-state">No counts.</p>}
            {Object.entries(sourceServiceCounts).map(([key, value]) => (
              <div key={key} className="list-item">
                <span>{key}</span>
                <span>{value}</span>
              </div>
            ))}
          </div>
          <h3 style={{ marginTop: 16 }}>Explain Channels</h3>
          <div className="list">
            <div className="list-item">
              <span>director_trace</span>
              <span>{directorTrace.length}</span>
            </div>
            <div className="list-item">
              <span>tool_calls</span>
              <span>{toolCalls.length}</span>
            </div>
            <div className="list-item">
              <span>state_effects</span>
              <span>{stateEffects.length}</span>
            </div>
            <div className="list-item">
              <span>drift_decisions</span>
              <span>{driftDecisions.length}</span>
            </div>
          </div>
        </div>
        <div className="card">
          <h2 className="section-title">Causal</h2>
          <div className="list">
            {causalEdges.length === 0 && <p className="empty-state">No causal edges.</p>}
            {causalEdges.map((edge, index) => (
              <div key={`${edge.cause_event_id || "cause"}-${index}`} className="list-item">
                <span>{edge.relation_type || "TRACE_SEQUENCE"}</span>
                <span>{edge.cause_event_id} -&gt; {edge.effect_event_id}</span>
              </div>
            ))}
          </div>
        </div>
        <div className="card">
          <h2 className="section-title">Ranking</h2>
          <div className="list">
            {banditDecisions.length === 0 && <p className="empty-state">No bandit decisions.</p>}
            {banditDecisions.map((decision, index) => (
              <div key={`bandit-${index}`} className="list-item">
                <span>decision #{index + 1}</span>
                <span style={{ maxWidth: 320, overflowWrap: "anywhere" }}>{JSON.stringify(decision)}</span>
              </div>
            ))}
          </div>
          <h2 className="section-title" style={{ marginTop: 16 }}>Safety</h2>
          <div className="list">
            {safetyReport.length === 0 && <p className="empty-state">No safety report.</p>}
            {safetyReport.map((row, index) => (
              <div key={`safety-${index}`} className="list-item">
                <span>report #{index + 1}</span>
                <span style={{ maxWidth: 320, overflowWrap: "anywhere" }}>{JSON.stringify(row)}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Layout>
  );
}

