import { act, render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";

import ChatRoom from "../pages/chat/[id]";
import { apiGet, apiPost } from "../lib/api";
import { useSessionUser } from "../lib/useSessionUser";

jest.mock("next/router", () => ({
  useRouter: () => ({ query: { id: "conv-1" } })
}));

jest.mock("../lib/api", () => ({
  apiGet: jest.fn(),
  apiPost: jest.fn()
}));

jest.mock("../lib/useSessionUser", () => ({
  useSessionUser: jest.fn()
}));

jest.mock("../components/Layout", () => ({
  __esModule: true,
  default: ({ children }: { children: ReactNode }) => <div>{children}</div>
}));

const mockedApiGet = apiGet as jest.MockedFunction<typeof apiGet>;
const mockedApiPost = apiPost as jest.MockedFunction<typeof apiPost>;
const mockedUseSessionUser = useSessionUser as jest.MockedFunction<typeof useSessionUser>;

type MessageEventLike = { data: string };

class MockWebSocket {
  static instances: MockWebSocket[] = [];
  url: string;
  onmessage: ((event: MessageEventLike) => void) | null = null;
  onerror: (() => void) | null = null;
  close = jest.fn();

  constructor(url: string) {
    this.url = url;
    MockWebSocket.instances.push(this);
  }

  emit(data: string) {
    if (this.onmessage) {
      this.onmessage({ data });
    }
  }
}

describe("chat page", () => {
  beforeEach(() => {
    mockedApiGet.mockReset();
    mockedApiPost.mockReset();
    mockedUseSessionUser.mockReset();
    MockWebSocket.instances = [];
    Object.defineProperty(window, "WebSocket", {
      writable: true,
      value: MockWebSocket
    });
  });

  it("renders contact-group role label for ai messages", async () => {
    mockedUseSessionUser.mockReturnValue({
      user: { userId: "u-self", nickname: "me" },
      setUser: jest.fn(),
      updateUser: jest.fn(),
      clearUser: jest.fn()
    });

    mockedApiGet.mockImplementation(async (path: string) => {
      if (path.startsWith("/api/conversations/conv-1/messages")) {
        return {
          data: {
            items: [
              {
                messageId: "m1",
                senderId: "u-friend",
                content: "hello from friend",
                sentAt: "2026-02-10T08:00:00Z"
              }
            ]
          }
        } as any;
      }
      if (path.startsWith("/api/contacts?userId=u-self")) {
        return {
          data: {
            items: [
              {
                contactId: "c1",
                contactUserId: "u-friend",
                groupName: "friend",
                blocked: false
              }
            ]
          }
        } as any;
      }
      if (path.startsWith("/api/personas/u-self")) {
        return { data: { summary: "persona" } } as any;
      }
      return { data: { items: [] } } as any;
    });

    render(<ChatRoom />);

    expect(await screen.findByText("Friend")).toBeInTheDocument();
    expect(screen.getByText("hello from friend")).toHaveClass("chat-bubble");
  });

  it("ignores connected and malformed websocket frames", async () => {
    mockedUseSessionUser.mockReturnValue({
      user: { userId: "u-self", nickname: "me" },
      setUser: jest.fn(),
      updateUser: jest.fn(),
      clearUser: jest.fn()
    });

    mockedApiGet.mockImplementation(async (path: string) => {
      if (path.startsWith("/api/conversations/conv-1/messages")) {
        return { data: { items: [] } } as any;
      }
      if (path.startsWith("/api/contacts?userId=u-self")) {
        return { data: { items: [] } } as any;
      }
      if (path.startsWith("/api/personas/u-self")) {
        return { data: { summary: "persona" } } as any;
      }
      return { data: { items: [] } } as any;
    });

    render(<ChatRoom />);

    await waitFor(() => {
      expect(MockWebSocket.instances.length).toBe(1);
    });

    const socket = MockWebSocket.instances[0];

    act(() => {
      socket.emit("connected");
      socket.emit("not-json");
      socket.emit(
        JSON.stringify({
          messageId: "m1",
          senderId: "u-other",
          content: "first",
          sentAt: "2026-02-10T09:00:00Z"
        })
      );
      socket.emit(
        JSON.stringify({
          messageId: "m1",
          senderId: "u-other",
          content: "updated",
          sentAt: "2026-02-10T09:00:01Z"
        })
      );
    });

    expect(await screen.findByText("updated")).toBeInTheDocument();
    expect(screen.queryByText("first")).not.toBeInTheDocument();
    expect(screen.queryByText("No messages yet.")).not.toBeInTheDocument();
  });
});
