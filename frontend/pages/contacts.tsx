import { useState } from "react";
import Layout from "../components/Layout";
import { apiGet, apiPost } from "../lib/api";
import { useRouter } from "next/router";
import { useSessionUser } from "../lib/useSessionUser";

interface Contact {
  contactId: string;
  contactUserId: string;
  groupName?: string;
  blocked: boolean;
}

export default function Contacts() {
  const router = useRouter();
  const { user, updateUser } = useSessionUser();
  const [userId, setUserId] = useState(user?.userId || "");
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [loadStatus, setLoadStatus] = useState<string | null>(null);
  const [actionStatus, setActionStatus] = useState<string | null>(null);
  const [creatingContactId, setCreatingContactId] = useState<string | null>(null);

  const loadContacts = async () => {
    const ownerId = userId.trim();
    if (!ownerId) {
      setLoadStatus("Please enter your user ID.");
      return;
    }
    try {
      setLoadStatus("Loading...");
      const res = await apiGet<any>(`/api/contacts?userId=${encodeURIComponent(ownerId)}`);
      setContacts(res.data?.items || []);
      setLoadStatus(null);
    } catch (error) {
      setLoadStatus("Failed to load contacts.");
    }
  };

  const startChat = async (contactUserId: string, contactId: string) => {
    const ownerId = userId.trim();
    if (!ownerId) {
      setActionStatus("Please enter your user ID.");
      return;
    }
    try {
      setActionStatus("Creating conversation...");
      setCreatingContactId(contactId);
      const payload = {
        type: "ONE_ON_ONE",
        participantIds: [ownerId, contactUserId]
      };
      const res = await apiPost<any>("/api/conversations", payload);
      const conversationId = res.data?.conversationId;
      if (conversationId) {
        setActionStatus(null);
        router.push(`/chat/${conversationId}`);
      } else {
        setActionStatus("Conversation creation failed.");
      }
    } catch (error) {
      setActionStatus("Failed to start chat.");
    } finally {
      setCreatingContactId(null);
    }
  };

  const onChangeUserId = (value: string) => {
    setUserId(value);
    updateUser({ userId: value });
  };

  return (
    <Layout>
      <div className="card">
        <h2 className="section-title">Contacts</h2>
        <p style={{ color: "var(--color-muted)", marginBottom: 16 }}>
          AI agents are ready to chat and share stories.
        </p>
        <div style={{ display: "flex", gap: 12, marginBottom: 16 }}>
          <input
            className="input"
            placeholder="Your user ID"
            value={userId}
            onChange={(event) => onChangeUserId(event.target.value)}
          />
          <button className="btn-primary" onClick={loadContacts}>
            Load
          </button>
        </div>
        {loadStatus && <p className="hint">{loadStatus}</p>}
        {actionStatus && <p className="hint">{actionStatus}</p>}
        {!loadStatus && contacts.length === 0 && (
          <p className="empty-state">No contacts yet. Try loading your AI agents.</p>
        )}
        <div className="list">
          {contacts.map((contact) => (
            <div key={contact.contactId} className="list-item">
              <div>
                <div style={{ fontWeight: 600 }}>{contact.contactUserId}</div>
                <div style={{ color: "var(--color-muted)", fontSize: 12 }}>
                  {contact.groupName || "Ungrouped"}
                </div>
              </div>
              <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
                <span className="tag">{contact.groupName || "ungrouped"}</span>
                <span className="tag">{contact.blocked ? "Blocked" : "Online"}</span>
                <button
                  className="btn-secondary"
                  disabled={Boolean(creatingContactId) || contact.blocked}
                  onClick={() => startChat(contact.contactUserId, contact.contactId)}
                >
                  {creatingContactId === contact.contactId ? "Starting..." : "Start Chat"}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </Layout>
  );
}
