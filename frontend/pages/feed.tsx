import { useEffect, useState } from "react";
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

function formatSentAt(sentAt?: string): string {
  if (!sentAt) {
    return "";
  }
  const date = new Date(sentAt);
  if (Number.isNaN(date.getTime())) {
    return sentAt;
  }
  return date.toLocaleString();
}

export default function Feed() {
  const { user, updateUser } = useSessionUser();
  const [feeds, setFeeds] = useState<FeedItem[]>([]);
  const [authorId, setAuthorId] = useState(user?.userId || "");
  const [viewerId, setViewerId] = useState(user?.userId || "");
  const [content, setContent] = useState("");
  const [visibility, setVisibility] = useState("");
  const [limit, setLimit] = useState("20");
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionStatus, setActionStatus] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!user?.userId) {
      return;
    }
    setAuthorId((previous) => previous || user.userId);
    setViewerId((previous) => previous || user.userId);
  }, [user?.userId]);

  const loadFeeds = async () => {
    try {
      setLoading(true);
      setLoadError(null);
      const params = new URLSearchParams();
      if (viewerId.trim()) {
        params.set("viewerId", viewerId.trim());
      }
      if (visibility) {
        params.set("visibility", visibility);
      }
      if (limit) {
        params.set("limit", limit);
      }
      const path = `/api/feeds${params.toString() ? `?${params.toString()}` : ""}`;
      const res = await apiGet<any>(path);
      const nextFeeds = Array.isArray(res.data) ? res.data : [];
      setFeeds(nextFeeds);
    } catch (error) {
      setLoadError("Failed to load feeds.");
    } finally {
      setLoading(false);
    }
  };

  const postFeed = async () => {
    const author = authorId.trim();
    const text = content.trim();
    if (!author) {
      setActionStatus("Please enter author ID.");
      return;
    }
    if (!text) {
      setActionStatus("Please enter feed content.");
      return;
    }
    try {
      await apiPost("/api/feeds", { authorId: author, content: text });
      setContent("");
      setActionStatus("Posted.");
      await loadFeeds();
    } catch (error) {
      setActionStatus("Failed to post feed.");
    }
  };

  const likeFeed = async (feedId: string) => {
    const viewer = viewerId.trim();
    if (!viewer) {
      setActionStatus("Please set viewer ID to like.");
      return;
    }
    try {
      await apiPost(`/api/feeds/${feedId}/like`, { userId: viewer });
      setActionStatus("Liked.");
      await loadFeeds();
    } catch (error) {
      setActionStatus("Failed to like feed.");
    }
  };

  const onChangeViewerId = (value: string) => {
    setViewerId(value);
    updateUser({ userId: value });
    if (!authorId) {
      setAuthorId(value);
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
              onChange={(event) => setAuthorId(event.target.value)}
            />
            <input
              className="input"
              placeholder="Viewer ID (for visibility)"
              value={viewerId}
              onChange={(event) => onChangeViewerId(event.target.value)}
            />
            <div className="grid grid-2">
              <select
                className="input"
                value={visibility}
                onChange={(event) => setVisibility(event.target.value)}
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
                onChange={(event) => setLimit(event.target.value)}
              />
            </div>
            <textarea
              className="input"
              placeholder="What's on your mind?"
              value={content}
              onChange={(event) => setContent(event.target.value)}
              style={{ minHeight: 120 }}
            />
            <button className="btn-primary" onClick={postFeed}>
              Post
            </button>
            {actionStatus && <p className="hint">{actionStatus}</p>}
          </div>
        </div>
        <div className="card">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <h2 className="section-title">Feed</h2>
            <button className="btn-secondary" onClick={loadFeeds}>
              {loading ? "Loading..." : "Refresh"}
            </button>
          </div>
          {loadError && <p className="hint" style={{ marginTop: 8 }}>{loadError}</p>}
          {!loading && !loadError && feeds.length === 0 && (
            <p className="empty-state" style={{ marginTop: 12 }}>No feeds yet.</p>
          )}
          <div className="list">
            {feeds.map((feed) => (
              <div key={feed.feedId} className="list-item">
                <div>
                  <div style={{ fontWeight: 600 }}>{feed.authorId}</div>
                  <div style={{ color: "var(--color-muted)", fontSize: 12 }}>
                    {formatSentAt(feed.createdAt)}
                  </div>
                  <div style={{ color: "var(--color-muted)", fontSize: 12 }}>
                    👍 {feed.likeCount ?? 0} · 💬 {feed.commentCount ?? 0}
                  </div>
                </div>
                <div style={{ maxWidth: 240 }}>{feed.content}</div>
                <button
                  className="btn-secondary"
                  disabled={Boolean(feed.likedByViewer) || loading}
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
