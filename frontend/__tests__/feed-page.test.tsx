import { render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import userEvent from "@testing-library/user-event";

import Feed from "../pages/feed";
import { apiGet, apiPost } from "../lib/api";
import { useSessionUser } from "../lib/useSessionUser";

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

describe("feed page", () => {
  beforeEach(() => {
    mockedApiGet.mockReset();
    mockedApiPost.mockReset();
    mockedUseSessionUser.mockReset();
  });

  it("reloads feed after like action", async () => {
    mockedUseSessionUser.mockReturnValue({
      user: { userId: "u-viewer", nickname: "viewer" },
      setUser: jest.fn(),
      updateUser: jest.fn(),
      clearUser: jest.fn()
    });

    mockedApiGet
      .mockResolvedValueOnce({
        data: [
          {
            feedId: "f1",
            authorId: "u-author",
            content: "hello feed",
            createdAt: "2026-02-10T10:00:00Z",
            likeCount: 0,
            commentCount: 0,
            likedByViewer: false
          }
        ]
      } as any)
      .mockResolvedValueOnce({
        data: [
          {
            feedId: "f1",
            authorId: "u-author",
            content: "hello feed",
            createdAt: "2026-02-10T10:00:00Z",
            likeCount: 1,
            commentCount: 0,
            likedByViewer: true
          }
        ]
      } as any);

    mockedApiPost.mockResolvedValue({ data: { event_id: "evt-1" } } as any);

    render(<Feed />);

    const refreshButton = screen.getByRole("button", { name: "Refresh" });
    await userEvent.click(refreshButton);

    expect(await screen.findByText("Like")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Like" }));

    await waitFor(() => {
      expect(mockedApiPost).toHaveBeenCalledWith("/api/v2/feeds/f1/like", { userId: "u-viewer", action: "LIKE" });
    });

    await waitFor(() => {
      expect(mockedApiGet).toHaveBeenCalledTimes(2);
    });

    expect(await screen.findByRole("button", { name: "Unlike" })).toBeInTheDocument();
  });
});
