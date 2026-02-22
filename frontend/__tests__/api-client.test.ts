import { apiGet, apiPost } from "../lib/api";

describe("api client headers", () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    localStorage.clear();
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ ok: true })
    } as Response);
  });

  afterEach(() => {
    global.fetch = originalFetch;
    jest.clearAllMocks();
    localStorage.clear();
  });

  it("injects bearer token and trace id when token exists", async () => {
    localStorage.setItem("tarotalk_token", "token-abc");

    await apiPost("/api/demo", { hello: "world" });

    const [, init] = (global.fetch as jest.Mock).mock.calls[0];
    const headers = new Headers((init as RequestInit).headers);
    expect(headers.get("Authorization")).toBe("Bearer token-abc");
    expect(headers.get("X-Trace-Id")).toMatch(/^web-/);
  });

  it("keeps request without authorization when no token", async () => {
    await apiGet("/api/demo");

    const [, init] = (global.fetch as jest.Mock).mock.calls[0];
    const headers = new Headers((init as RequestInit).headers);
    expect(headers.get("Authorization")).toBeNull();
    expect(headers.get("X-Trace-Id")).toMatch(/^web-/);
  });
});
