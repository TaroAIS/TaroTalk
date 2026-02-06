import { useState } from "react";
import Layout from "../components/Layout";
import { apiGet, apiPost } from "../lib/api";
import { useSessionUser } from "../lib/useSessionUser";

interface FeedItem {
  feedId: string;
  authorId: string;
  content: string;
  createdAt: string;
  likeCount?: number;
  commentCount?: number;
  likedByViewer?: boolean;
}

export default function Feed() {
  const { user } = useSessionUser();
  const [feeds, setFeeds] = useState<FeedItem[]>([]);
  const [authorId, setAuthorId] = useState("");
  const [viewerId, setViewerId] = useState(user?.userId || "");
  const [content, setContent] = useState("");
  const [visibility, setVisibility] = useState("");
  const [limit, setLimit] = useState("20");
  const [status, setStatus] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const loadFeeds = async () => {
    try {
      setLoading(true);
      setStatus(null);
      const params = new URLSearchParams();
      if (viewerId) {
        params.set("viewerId", viewerId);
      }
      if (visibility) {
        params.set("visibility", visibility);
      }
      if (limit) {
        params.set("limit", limit);
      }
      const path = `/api/feeds${params.toString() ? `?${params.toString()}` : ""}`;
      const res = await apiGet<any>(path);
      setFeeds(res.data || []);
      if (!res.data || res.data.length === 0) {
        setStatus("No visible feeds yet.");
      }
    } catch (error) {
      setStatus("Failed to load feeds.");
    } finally {
      setLoading(false);
    }
  };

  const postFeed = async () => {
    try {
      setStatus(null);
      await apiPost("/api/feeds", { authorId, content });
      setContent("");
      loadFeeds();
    } catch (error) {
      setStatus("Failed to post feed.");
    }
  };

  const likeFeed = async (feedId: string) => {
    if (!viewerId) {
      setStatus("Please set viewer ID to like.");
      return;
    }
    try {
      await apiPost(`/api/feeds/${feedId}/like`, { userId: viewerId });
      setStatus("Liked.");
    } catch (error) {
      setStatus("Failed to like feed.");
    }
  };

  return (
    <Layout>
      <div className="grid grid-2">
        <div className="card">
          <h2 className="section-title">Share an update</h2>
          <div style={{ display: "grid", gap: 12 }}>
            <input
              className="input"
              placeholder="Author ID"
              value={authorId}
              onChange={(e) => setAuthorId(e.target.value)}
            />
            <input
              className="input"
              placeholder="Viewer ID (for visibility)"
              value={viewerId}
              onChange={(e) => setViewerId(e.target.value)}
            />
            <div className="grid grid-2">
              <select
                className="input"
                value={visibility}
                onChange={(e) => setVisibility(e.target.value)}
              >
                <option value="">Default visibility</option>
                <option value="contact">Contact</option>
                <option value="relationship">Relationship</option>
                <option value="hybrid">Hybrid</option>
              </select>
              <input
                className="input"
                placeholder="Limit"
                value={limit}
                onChange={(e) => setLimit(e.target.value)}
              />
            </div>
            <textarea
              className="input"
              placeholder="What's on your mind?"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              style={{ minHeight: 120 }}
            />
            <button className="btn-primary" onClick={postFeed}>
              Post
            </button>
            {status && <p className="hint">{status}</p>}
          </div>
        </div>
        <div className="card">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <h2 className="section-title">Feed</h2>
            <button className="btn-secondary" onClick={loadFeeds}>
              {loading ? "Loading..." : "Refresh"}
            </button>
          </div>
          {status && <p className="hint" style={{ marginTop: 8 }}>{status}</p>}
          {!status && feeds.length === 0 && (
            <p className="empty-state" style={{ marginTop: 12 }}>No feeds yet.</p>
          )}
          <div className="list">
            {feeds.map((feed) => (
              <div key={feed.feedId} className="list-item">
                <div>
                  <div style={{ fontWeight: 600 }}>{feed.authorId}</div>
                  <div style={{ color: "var(--color-muted)", fontSize: 12 }}>{feed.createdAt}</div>
                  <div style={{ color: "var(--color-muted)", fontSize: 12 }}>
                    👍 {feed.likeCount ?? 0} · 💬 {feed.commentCount ?? 0}
                  </div>
                </div>
                <div style={{ maxWidth: 240 }}>{feed.content}</div>
                <button
                  className="btn-secondary"
                  disabled={Boolean(feed.likedByViewer)}
                  onClick={() => likeFeed(feed.feedId)}
                >
                  {feed.likedByViewer ? "Liked" : "Like"}
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Layout>
  );
}
