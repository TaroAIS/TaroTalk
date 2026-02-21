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

export default function TraceReplayPage() {
  const router = useRouter();
  const { traceId } = router.query;
  const [events, setEvents] = useState<TraceEvent[]>([]);
  const [eventTypeCounts, setEventTypeCounts] = useState<Record<string, number>>({});
  const [sourceServiceCounts, setSourceServiceCounts] = useState<Record<string, number>>({});
  const [status, setStatus] = useState<string>("");

  useEffect(() => {
    if (typeof traceId !== "string" || !traceId.trim()) {
      return;
    }
    const load = async () => {
      try {
        setStatus("Loading trace...");
        const res = await apiGet<any>(`/api/v2/traces/${traceId}/replay/aggregate`);
        const data = res?.data || {};
        setEvents(Array.isArray(data.events) ? data.events : []);
        setEventTypeCounts(data.eventTypeCounts || {});
        setSourceServiceCounts(data.sourceServiceCounts || {});
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
          <h2 className="section-title">Trace Replay</h2>
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
        </div>
      </div>
    </Layout>
  );
}

