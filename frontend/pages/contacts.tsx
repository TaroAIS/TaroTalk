import { useState } from "react";
import Layout from "../components/Layout";
import { apiGet } from "../lib/api";

interface Contact {
  contactId: string;
  contactUserId: string;
  groupName?: string;
  blocked: boolean;
}

export default function Contacts() {
  const [userId, setUserId] = useState("");
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [status, setStatus] = useState<string | null>(null);

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
        <div className="list">
          {contacts.map((contact) => (
            <div key={contact.contactId} className="list-item">
              <div>
                <div style={{ fontWeight: 600 }}>{contact.contactUserId}</div>
                <div style={{ color: "var(--color-muted)", fontSize: 12 }}>
                  {contact.groupName || "Ungrouped"}
                </div>
              </div>
              <span className="tag">{contact.blocked ? "Blocked" : "Online"}</span>
            </div>
          ))}
        </div>
      </div>
    </Layout>
  );
}
