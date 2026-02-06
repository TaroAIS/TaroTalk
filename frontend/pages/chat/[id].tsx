import { useRouter } from "next/router";
import { useEffect, useRef, useState } from "react";
import Layout from "../../components/Layout";
import { apiGet, apiPost } from "../../lib/api";
import { useSessionUser } from "../../lib/useSessionUser";

interface Message {
  messageId: string;
  senderId: string;
  content: string;
  sentAt: string;
}

export default function ChatRoom() {
  const router = useRouter();
  const { id } = router.query;
  const { user } = useSessionUser();
  const [userId, setUserId] = useState(user?.userId || "");
  const [content, setContent] = useState("");
  const [personaSummary, setPersonaSummary] = useState("");
  const [generateAiReply, setGenerateAiReply] = useState(true);
  const [messages, setMessages] = useState<Message[]>([]);
  const [status, setStatus] = useState<string | null>(null);
  const [typingUsers, setTypingUsers] = useState<string[]>([]);
  const socketRef = useRef<WebSocket | null>(null);
  const typingTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (!id) {
      return;
    }
    const loadMessages = async () => {
      const res = await apiGet<any>(`/api/conversations/${id}/messages`);
      setMessages(res.data?.items || []);
    };
    loadMessages();

    const wsBase = process.env.NEXT_PUBLIC_WS_BASE || "ws://localhost:8084";
    const socket = new WebSocket(`${wsBase}/ws/chat?conversationId=${id}`);
    socket.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        if (msg && msg.type) {
          if (msg.type === "typing") {
            const typingUser = msg.data?.userId;
            if (typingUser) {
              setTypingUsers((prev) => {
                if (msg.data.typing) {
                  return prev.includes(typingUser) ? prev : [...prev, typingUser];
                }
                return prev.filter((id) => id !== typingUser);
              });
            }
          }
          if (msg.type === "read") {
            // ignore for now, placeholder
          }
          return;
        }
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

  useEffect(() => {
    const loadPersona = async () => {
      if (!userId) {
        return;
      }
      try {
        const res = await apiGet<any>(`/api/personas/${userId}`);
        if (res.data?.summary) {
          setPersonaSummary(res.data.summary);
        } else if (res.data?.description) {
          setPersonaSummary(res.data.description);
        }
      } catch (error) {
        // ignore
      }
    };
    loadPersona();
  }, [userId]);

  const sendMessage = async () => {
    if (!id) {
      return;
    }
    if (!userId) {
      setStatus("Please enter your user ID.");
      return;
    }
    try {
      await apiPost(`/api/conversations/${id}/messages`, {
        senderId: userId,
        content,
        type: "text",
        generateAiReply,
        personaSummary
      });
      setContent("");
      setStatus(null);
    } catch (error) {
      setStatus("Failed to send message.");
    }
  };

  const publishTyping = async (typing: boolean) => {
    if (!id || !userId) {
      return;
    }
    try {
      await apiPost(`/api/conversations/${id}/typing`, { userId, typing });
    } catch (error) {
      // ignore
    }
  };

  const onChangeContent = (value: string) => {
    setContent(value);
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }
    publishTyping(true);
    typingTimeoutRef.current = setTimeout(() => {
      publishTyping(false);
    }, 800);
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
              placeholder="Persona summary (optional)"
              value={personaSummary}
              onChange={(e) => setPersonaSummary(e.target.value)}
              style={{ minHeight: 80 }}
            />
            <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <input
                type="checkbox"
                checked={generateAiReply}
                onChange={(e) => setGenerateAiReply(e.target.checked)}
              />
              Enable AI reply
            </label>
          </div>
          <div style={{ display: "grid", gap: 12, marginBottom: 12 }}>
            <textarea
              className="input"
              placeholder="Type a message..."
              value={content}
              onChange={(e) => onChangeContent(e.target.value)}
              style={{ minHeight: 120 }}
            />
            <button className="btn-primary" onClick={sendMessage}>
              Send
            </button>
          </div>
          {status && <p className="hint">{status}</p>}
        </div>
        <div className="card">
          <h2 className="section-title">Messages</h2>
          {typingUsers.length > 0 && (
            <p className="hint">
              {typingUsers.join(", ")} typing...
            </p>
          )}
          {messages.length === 0 && (
            <p className="empty-state">No messages yet.</p>
          )}
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
