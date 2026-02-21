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
        }
      }
    } as any);

    render(<TraceReplayPage />);

    await waitFor(() => {
      expect(mockedApiGet).toHaveBeenCalledWith("/api/v2/traces/trace-1/replay/aggregate");
    });
    expect((await screen.findAllByText("CHAT_MESSAGE_CREATED")).length).toBeGreaterThanOrEqual(1);
    expect((await screen.findAllByText("chat-service")).length).toBeGreaterThanOrEqual(1);
  });
});
