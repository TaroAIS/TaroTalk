import { useRouter } from "next/router";
import { useEffect, useMemo, useRef, useState } from "react";
import Layout from "../../components/Layout";
import { apiGet, apiPost } from "../../lib/api";
import { useSessionUser } from "../../lib/useSessionUser";

interface ChatMessage {
  messageId: string;
  senderId: string;
  content: string;
  sentAt: string;
  type?: string;
  readAt?: string | null;
  replyToMessageId?: string | null;
}

interface ChatMessageView extends ChatMessage {
  senderRole?: string;
  senderLabel: string;
  isSelf: boolean;
  sentAtLabel: string;
}

type ContactRoleMap = Record<string, string>;

interface ContactItem {
  contactUserId: string;
  groupName?: string;
}

const ROLE_LABELS: Record<string, string> = {
  "self-agent": "Self Agent",
  friend: "Friend",
  mentor: "Mentor",
  rival: "Rival",
  advertiser: "Advertiser"
};

function normalizeRole(groupName?: string): string | undefined {
  if (!groupName) {
    return undefined;
  }
  const normalized = groupName.trim().toLowerCase();
  if (!normalized) {
    return undefined;
  }
  if (normalized === "self") {
    return "self-agent";
  }
  return normalized;
}

function roleBase(role?: string): string {
  if (!role) {
    return "default";
  }
  if (role === "self-agent") {
    return "self-agent";
  }
  if (role.startsWith("friend")) {
    return "friend";
  }
  if (role.startsWith("mentor")) {
    return "mentor";
  }
  if (role.startsWith("rival")) {
    return "rival";
  }
  if (role.startsWith("advertiser")) {
    return "advertiser";
  }
  return "default";
}

function shortId(id: string): string {
  return id ? id.slice(0, 8) : "unknown";
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

function sortMessages(items: ChatMessage[]): ChatMessage[] {
  return [...items].sort((left, right) => {
    const leftTime = left.sentAt ? Date.parse(left.sentAt) : 0;
    const rightTime = right.sentAt ? Date.parse(right.sentAt) : 0;
    return leftTime - rightTime;
  });
}

function toMessage(value: unknown): ChatMessage | null {
  if (!value || typeof value !== "object") {
    return null;
  }
  const row = value as Record<string, unknown>;
  if (typeof row.messageId !== "string" || typeof row.senderId !== "string") {
    return null;
  }
  const content = typeof row.content === "string" ? row.content : "";
  const sentAt = typeof row.sentAt === "string" ? row.sentAt : new Date().toISOString();
  return {
    messageId: row.messageId,
    senderId: row.senderId,
    content,
    sentAt,
    type: typeof row.type === "string" ? row.type : undefined,
    readAt: typeof row.readAt === "string" ? row.readAt : null,
    replyToMessageId: typeof row.replyToMessageId === "string" ? row.replyToMessageId : null
  };
}

function upsertMessage(messages: ChatMessage[], incoming: ChatMessage): ChatMessage[] {
  const index = messages.findIndex((item) => item.messageId === incoming.messageId);
  if (index === -1) {
    return sortMessages([...messages, incoming]);
  }
  const next = [...messages];
  next[index] = { ...next[index], ...incoming };
  return sortMessages(next);
}

export default function ChatRoom() {
  const router = useRouter();
  const { id } = router.query;
  const { user, updateUser } = useSessionUser();
  const [userId, setUserId] = useState(user?.userId || "");
  const [content, setContent] = useState("");
  const [personaSummary, setPersonaSummary] = useState("");
  const [generateAiReply, setGenerateAiReply] = useState(true);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [contactRoleMap, setContactRoleMap] = useState<ContactRoleMap>({});
  const [typingUsers, setTypingUsers] = useState<string[]>([]);
  const [formStatus, setFormStatus] = useState<string | null>(null);
  const [socketStatus, setSocketStatus] = useState<string | null>(null);
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (user?.userId && !userId) {
      setUserId(user.userId);
    }
  }, [user?.userId, userId]);

  useEffect(() => {
    if (!userId) {
      setContactRoleMap({});
      return;
    }
    let canceled = false;
    const loadRoleMap = async () => {
      try {
        const res = await apiGet<any>(`/api/contacts?userId=${encodeURIComponent(userId)}`);
        if (canceled) {
          return;
        }
        const nextMap: ContactRoleMap = {};
        const items = Array.isArray(res.data?.items) ? res.data.items : [];
        items.forEach((contact: ContactItem) => {
          if (!contact || !contact.contactUserId) {
            return;
          }
          const role = normalizeRole(contact.groupName);
          if (role) {
            nextMap[contact.contactUserId] = role;
          }
        });
        setContactRoleMap(nextMap);
      } catch (error) {
        if (!canceled) {
          setSocketStatus("Unable to load contact roles.");
        }
      }
    };
    loadRoleMap();
    return () => {
      canceled = true;
    };
  }, [userId]);

  useEffect(() => {
    if (!userId) {
      setPersonaSummary("");
      return;
    }
    let canceled = false;
    const loadPersona = async () => {
      try {
        const res = await apiGet<any>(`/api/personas/${userId}`);
        if (canceled) {
          return;
        }
        const summary = res.data?.summary || res.data?.description || "";
        setPersonaSummary(summary);
        updateUser({ userId, personaSummary: summary });
      } catch (error) {
        // ignore
      }
    };
    loadPersona();
    return () => {
      canceled = true;
    };
  }, [updateUser, userId]);

  useEffect(() => {
    if (typeof id !== "string") {
      return;
    }

    let canceled = false;

    const loadMessages = async () => {
      try {
        const res = await apiGet<any>(`/api/conversations/${id}/messages`);
        if (canceled) {
          return;
        }
        const items = Array.isArray(res.data?.items) ? res.data.items : [];
        const nextMessages = items
          .map((item: unknown) => toMessage(item))
          .filter((item: ChatMessage | null): item is ChatMessage => item !== null);
        setMessages(sortMessages(nextMessages));
      } catch (error) {
        if (!canceled) {
          setSocketStatus("Failed to load messages.");
        }
      }
    };

    loadMessages();

    const wsBase = process.env.NEXT_PUBLIC_WS_BASE || "ws://localhost:8084";
    const socket = new WebSocket(`${wsBase}/ws/chat?conversationId=${id}`);

    socket.onmessage = (event) => {
      if (typeof event.data !== "string" || event.data === "connected") {
        return;
      }
      try {
        const payload = JSON.parse(event.data) as Record<string, unknown>;
        if (payload && typeof payload.type === "string") {
          if (payload.type === "typing") {
            const typingEvent = payload.data as Record<string, unknown> | undefined;
            const typingUser = typeof typingEvent?.userId === "string" ? typingEvent.userId : "";
            if (typingUser && typingUser !== userId) {
              setTypingUsers((previous) => {
                const isTyping = Boolean(typingEvent?.typing);
                if (isTyping) {
                  return previous.includes(typingUser) ? previous : [...previous, typingUser];
                }
                return previous.filter((item) => item !== typingUser);
              });
            }
          }
          if (payload.type === "read") {
            return;
          }
          return;
        }
        const incoming = toMessage(payload);
        if (!incoming) {
          return;
        }
        setMessages((previous) => upsertMessage(previous, incoming));
      } catch (error) {
        // ignore malformed payload
      }
    };

    socket.onerror = () => {
      if (!canceled) {
        setSocketStatus("WebSocket disconnected. Refresh to retry.");
      }
    };

    return () => {
      canceled = true;
      socket.close();
    };
  }, [id, userId]);

  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
    };
  }, []);

  const messageViews = useMemo<ChatMessageView[]>(() => {
    return messages.map((message) => {
      const isSelf = Boolean(userId) && message.senderId === userId;
      const senderRole = isSelf ? "self-agent" : contactRoleMap[message.senderId];
      const roleName = roleBase(senderRole);
      const senderLabel = isSelf
        ? "You"
        : (ROLE_LABELS[roleName] || `AI-${shortId(message.senderId)}`);
      return {
        ...message,
        senderRole,
        senderLabel,
        isSelf,
        sentAtLabel: formatSentAt(message.sentAt)
      };
    });
  }, [contactRoleMap, messages, userId]);

  const typingLabels = useMemo(() => {
    return typingUsers
      .filter((typingUser) => typingUser !== userId)
      .map((typingUser) => {
        const mappedRole = roleBase(contactRoleMap[typingUser]);
        return ROLE_LABELS[mappedRole] || `AI-${shortId(typingUser)}`;
      });
  }, [contactRoleMap, typingUsers, userId]);

  const publishTyping = async (typing: boolean) => {
    if (typeof id !== "string" || !userId) {
      return;
    }
    try {
      await apiPost(`/api/conversations/${id}/typing`, { userId, typing });
    } catch (error) {
      // ignore
    }
  };

  const sendMessage = async () => {
    if (typeof id !== "string") {
      setFormStatus("Missing conversation ID.");
      return;
    }
    if (!userId.trim()) {
      setFormStatus("Please enter your user ID.");
      return;
    }
    const trimmed = content.trim();
    if (!trimmed) {
      setFormStatus("Please type a message.");
      return;
    }
    try {
      await apiPost(`/api/conversations/${id}/messages`, {
        senderId: userId,
        content: trimmed,
        type: "text",
        generateAiReply,
        personaSummary
      });
      setContent("");
      setFormStatus(null);
      publishTyping(false);
    } catch (error) {
      setFormStatus("Failed to send message.");
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

  const onChangeUserId = (value: string) => {
    setUserId(value);
    updateUser({ userId: value });
  };

  return (
    <Layout>
      <div className="grid grid-2">
        <div className="card">
          <h2 className="section-title">Conversation {typeof id === "string" ? id : ""}</h2>
          <div style={{ display: "flex", gap: 12, marginBottom: 12 }}>
            <input
              className="input"
              placeholder="Your user ID"
              value={userId}
              onChange={(event) => onChangeUserId(event.target.value)}
            />
          </div>
          <div style={{ display: "grid", gap: 12, marginBottom: 12 }}>
            <textarea
              className="input"
              placeholder="Persona summary (optional)"
              value={personaSummary}
              onChange={(event) => setPersonaSummary(event.target.value)}
              style={{ minHeight: 80 }}
            />
            <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <input
                type="checkbox"
                checked={generateAiReply}
                onChange={(event) => setGenerateAiReply(event.target.checked)}
              />
              Enable AI reply
            </label>
          </div>
          <div style={{ display: "grid", gap: 12, marginBottom: 12 }}>
            <textarea
              className="input"
              placeholder="Type a message..."
              value={content}
              onChange={(event) => onChangeContent(event.target.value)}
              style={{ minHeight: 120 }}
            />
            <button className="btn-primary" onClick={sendMessage}>
              Send
            </button>
          </div>
          {formStatus && <p className="hint">{formStatus}</p>}
        </div>
        <div className="card">
          <h2 className="section-title">Messages</h2>
          {socketStatus && <p className="hint">{socketStatus}</p>}
          {typingLabels.length > 0 && (
            <p className="hint">{typingLabels.join(", ")} typing...</p>
          )}
          {messageViews.length === 0 && (
            <p className="empty-state">No messages yet.</p>
          )}
          <div className="chat-list">
            {messageViews.map((message) => {
              const currentRole = roleBase(message.senderRole);
              return (
                <div
                  key={message.messageId}
                  className={`chat-row ${message.isSelf ? "self" : "other"}`}
                >
                  <div className="chat-meta">
                    <span className={`role-badge role-${currentRole}`}>{message.senderLabel}</span>
                    <span className="chat-time">{message.sentAtLabel}</span>
                  </div>
                  <div className={`chat-bubble ${message.isSelf ? "self" : "other"}`}>
                    {message.content}
                  </div>
                  {message.replyToMessageId && (
                    <div className="chat-sub">Reply to: {message.replyToMessageId}</div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </Layout>
  );
}
