import { useState } from "react";
import Layout from "../components/Layout";
import { apiPost } from "../lib/api";

export default function Register() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [description, setDescription] = useState("");
  const [status, setStatus] = useState<string | null>(null);

  const onSubmit = async () => {
    try {
      setStatus("Creating account...");
      const auth = await apiPost<any>("/api/auth/register", {
        email,
        password,
        nickname
      });
      const userId = auth.data?.userId;
      if (userId) {
        await apiPost("/api/personas", { userId, description });
      }
      setStatus("Persona ready. You can start chatting.");
    } catch (error) {
      setStatus("Registration failed. Please review inputs.");
    }
  };

  return (
    <Layout>
      <div className="card" style={{ maxWidth: 720, margin: "0 auto" }}>
        <h2 className="section-title">Create your persona</h2>
        <p style={{ color: "var(--color-muted)", marginBottom: 16 }}>
          Tell us about your personality, interests, and background.
        </p>
        <div style={{ display: "grid", gap: 12 }}>
          <input
            className="input"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <input
            className="input"
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <input
            className="input"
            placeholder="Nickname"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
          />
          <textarea
            className="input"
            style={{ minHeight: 120 }}
            placeholder="Describe yourself..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <button className="btn-primary" onClick={onSubmit}>
            Generate persona
          </button>
          {status && <p style={{ color: "var(--color-muted)" }}>{status}</p>}
        </div>
      </div>
    </Layout>
  );
}
