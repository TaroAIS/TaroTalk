import { render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";

import TraceReplayPage from "../pages/trace/[traceId]";
import { apiGet } from "../lib/api";

jest.mock("next/router", () => ({
  useRouter: () => ({ query: { traceId: "trace-1" } })
}));

jest.mock("../lib/api", () => ({
  apiGet: jest.fn()
}));

jest.mock("../components/Layout", () => ({
  __esModule: true,
  default: ({ children }: { children: ReactNode }) => <div>{children}</div>
}));

const mockedApiGet = apiGet as jest.MockedFunction<typeof apiGet>;

describe("trace replay page", () => {
  beforeEach(() => {
    mockedApiGet.mockReset();
    process.env.NEXT_PUBLIC_INTERNAL_DEBUG = "true";
  });

  it("renders aggregated replay content", async () => {
    mockedApiGet.mockResolvedValue({
      data: {
        events: [
          {
            eventId: "e1",
            eventType: "CHAT_MESSAGE_CREATED",
            sourceService: "chat-service",
            payloadJson: "{\"content\":\"hello\"}"
          }
        ],
        eventTypeCounts: {
          CHAT_MESSAGE_CREATED: 1
        },
        sourceServiceCounts: {
          "chat-service": 1
        },
        causalEdges: [
          {
            cause_event_id: "e1",
            effect_event_id: "e2",
            relation_type: "TRACE_SEQUENCE"
          }
        ],
        banditDecisions: [{ feedId: "f1", score: 0.81 }],
        safetyReport: [{ severity: "warn" }]
      }
    } as any);

    render(<TraceReplayPage />);

    await waitFor(() => {
      expect(mockedApiGet).toHaveBeenCalledWith("/api/v2/traces/trace-1/explain");
    });
    expect((await screen.findAllByText("CHAT_MESSAGE_CREATED")).length).toBeGreaterThanOrEqual(1);
    expect((await screen.findAllByText("chat-service")).length).toBeGreaterThanOrEqual(1);
    expect(await screen.findByText("Causal")).toBeInTheDocument();
    expect(await screen.findByText("Ranking")).toBeInTheDocument();
    expect(await screen.findByText("Safety")).toBeInTheDocument();
  });
});
