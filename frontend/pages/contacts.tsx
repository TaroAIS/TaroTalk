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
  const { user } = useSessionUser();
  const [userId, setUserId] = useState(user?.userId || "");
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [status, setStatus] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  const loadContacts = async () => {
    try {
      setStatus("Loading...");
      const res = await apiGet<any>(`/api/contacts?userId=${userId}`);
      setContacts(res.data?.items || []);
      setStatus(null);
    } catch (error) {
      setStatus("Failed to load contacts.");
    }
  };

  const startChat = async (contactUserId: string) => {
    if (!userId) {
      setStatus("Please enter your user ID.");
      return;
    }
    try {
      setCreating(true);
      const payload = {
        type: "ONE_ON_ONE",
        participantIds: [userId, contactUserId]
      };
      const res = await apiPost<any>("/api/conversations", payload);
      const conversationId = res.data?.conversationId;
      if (conversationId) {
        router.push(`/chat/${conversationId}`);
      }
    } catch (error) {
      setStatus("Failed to start chat.");
    } finally {
      setCreating(false);
    }
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
            onChange={(e) => setUserId(e.target.value)}
          />
          <button className="btn-primary" onClick={loadContacts}>
            Load
          </button>
        </div>
        {status && <p style={{ color: "var(--color-muted)" }}>{status}</p>}
        {!status && contacts.length === 0 && (
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
                <span className="tag">{contact.blocked ? "Blocked" : "Online"}</span>
                <button
                  className="btn-secondary"
                  disabled={creating || contact.blocked}
                  onClick={() => startChat(contact.contactUserId)}
                >
                  Start Chat
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </Layout>
  );
}
