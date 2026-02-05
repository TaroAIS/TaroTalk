import { useState } from "react";
import Layout from "../components/Layout";
import { apiGet, apiPost } from "../lib/api";

interface FeedItem {
  feedId: string;
  authorId: string;
  content: string;
  createdAt: string;
}

export default function Feed() {
  const [feeds, setFeeds] = useState<FeedItem[]>([]);
  const [authorId, setAuthorId] = useState("");
  const [content, setContent] = useState("");

  const loadFeeds = async () => {
    const res = await apiGet<any>("/api/feeds");
    setFeeds(res.data || []);
  };

  const postFeed = async () => {
    await apiPost("/api/feeds", { authorId, content });
    setContent("");
    loadFeeds();
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
          </div>
        </div>
        <div className="card">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <h2 className="section-title">Feed</h2>
            <button className="btn-secondary" onClick={loadFeeds}>
              Refresh
            </button>
          </div>
          <div className="list">
            {feeds.map((feed) => (
              <div key={feed.feedId} className="list-item">
                <div>
                  <div style={{ fontWeight: 600 }}>{feed.authorId}</div>
                  <div style={{ color: "var(--color-muted)", fontSize: 12 }}>{feed.createdAt}</div>
                </div>
                <div style={{ maxWidth: 240 }}>{feed.content}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Layout>
  );
}
