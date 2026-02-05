import { useState } from "react";
import Layout from "../components/Layout";
import { apiGet, apiPut } from "../lib/api";

export default function Profile() {
  const [userId, setUserId] = useState("");
  const [profile, setProfile] = useState<any>(null);
  const [persona, setPersona] = useState<any>(null);
  const [nickname, setNickname] = useState("");

  const loadProfile = async () => {
    const userRes = await apiGet<any>(`/api/users/${userId}`);
    setProfile(userRes.data);
    const personaRes = await apiGet<any>(`/api/personas/${userId}`);
    setPersona(personaRes.data);
  };

  const updateProfile = async () => {
    await apiPut(`/api/users/${userId}`, { nickname });
    loadProfile();
  };

  return (
    <Layout>
      <div className="grid grid-2">
        <div className="card">
          <h2 className="section-title">Profile</h2>
          <div style={{ display: "grid", gap: 12 }}>
            <input
              className="input"
              placeholder="User ID"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
            />
            <button className="btn-primary" onClick={loadProfile}>
              Load
            </button>
            {profile && (
              <div>
                <p style={{ fontWeight: 600 }}>{profile.nickname}</p>
                <p style={{ color: "var(--color-muted)" }}>{profile.userId}</p>
              </div>
            )}
          </div>
        </div>
        <div className="card">
          <h2 className="section-title">Edit profile</h2>
          <div style={{ display: "grid", gap: 12 }}>
            <input
              className="input"
              placeholder="New nickname"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
            />
            <button className="btn-secondary" onClick={updateProfile}>
              Update
            </button>
            {persona && (
              <div style={{ marginTop: 12 }}>
                <div className="tag" style={{ marginBottom: 8 }}>Persona Summary</div>
                <p style={{ color: "var(--color-muted)" }}>{persona.description}</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}
