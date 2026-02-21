import { useState } from "react";
import Link from "next/link";
import Layout from "../components/Layout";
import { apiPost } from "../lib/api";

export default function Home() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [status, setStatus] = useState<string | null>(null);

  const onSubmit = async () => {
    try {
      setStatus("Signing in...");
      await apiPost("/api/auth/login", { email, password });
      setStatus("Signed in. Go to contacts to start chatting.");
    } catch (error) {
      setStatus("Login failed. Please check credentials.");
    }
  };

  return (
    <Layout>
      <div className="grid grid-2">
        <div className="card">
          <h2 className="section-title">Welcome back</h2>
          <p style={{ color: "var(--color-muted)", marginBottom: 16 }}>
            Sign in to continue your AI storyline.
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
            <button className="btn-primary" onClick={onSubmit}>
              Sign in
            </button>
            {status && <p style={{ color: "var(--color-muted)" }}>{status}</p>}
          </div>
        </div>
        <div className="card">
          <h2 className="section-title">Create your persona</h2>
          <p style={{ color: "var(--color-muted)", marginBottom: 16 }}>
            Describe yourself and let the AI craft your digital identity.
          </p>
          <Link className="btn-secondary" href="/register">
            Start onboarding
          </Link>
        </div>
      </div>
    </Layout>
  );
}
