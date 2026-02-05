import { useRouter } from "next/router";
import { useEffect, useRef, useState } from "react";
import Layout from "../../components/Layout";
import { apiGet, apiPost } from "../../lib/api";

interface Message {
  messageId: string;
  senderId: string;
  content: string;
  sentAt: string;
}

export default function ChatRoom() {
  const router = useRouter();
  const { id } = router.query;
  const [userId, setUserId] = useState("");
  const [content, setContent] = useState("");
  const [messages, setMessages] = useState<Message[]>([]);
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    if (!id) {
      return;
    }
    const loadMessages = async () => {
      const res = await apiGet<any>(`/api/conversations/${id}/messages`);
      setMessages(res.data?.items || []);
    };
    loadMessages();

    const socket = new WebSocket(`ws://localhost:8084/ws/chat?conversationId=${id}`);
    socket.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        setMessages((prev) => [msg, ...prev]);
      } catch (e) {
        // ignore
      }
    };
    socketRef.current = socket;

    return () => {
      socket.close();
    };
  }, [id]);

  const sendMessage = async () => {
    if (!id) {
      return;
    }
    await apiPost(`/api/conversations/${id}/messages`, {
      senderId: userId,
      content,
      type: "text"
    });
    setContent("");
  };

  return (
    <Layout>
      <div className="grid grid-2">
        <div className="card">
          <h2 className="section-title">Conversation {id}</h2>
          <div style={{ display: "flex", gap: 12, marginBottom: 12 }}>
            <input
              className="input"
              placeholder="Your user ID"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
            />
          </div>
          <div style={{ display: "grid", gap: 12, marginBottom: 12 }}>
            <textarea
              className="input"
              placeholder="Type a message..."
              value={content}
              onChange={(e) => setContent(e.target.value)}
              style={{ minHeight: 120 }}
            />
            <button className="btn-primary" onClick={sendMessage}>
              Send
            </button>
          </div>
        </div>
        <div className="card">
          <h2 className="section-title">Messages</h2>
          <div className="list">
            {messages.map((message) => (
              <div key={message.messageId} className="list-item">
                <div>
                  <div style={{ fontWeight: 600 }}>{message.senderId}</div>
                  <div style={{ color: "var(--color-muted)", fontSize: 12 }}>{message.sentAt}</div>
                </div>
                <div style={{ maxWidth: 240 }}>{message.content}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Layout>
  );
}
