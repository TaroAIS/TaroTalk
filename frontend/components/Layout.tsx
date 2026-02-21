import Link from "next/link";
import { ReactNode } from "react";

interface LayoutProps {
  children: ReactNode;
}

export default function Layout({ children }: LayoutProps) {
  const internalDebugEnabled = process.env.NEXT_PUBLIC_INTERNAL_DEBUG === "true";

  return (
    <div className="container">
      <header className="header">
        <div className="brand">
          <div className="brand-badge">AI</div>
          <div>
            <h1>Tarotalk</h1>
            <p style={{ color: "var(--color-muted)", fontSize: 14 }}>
              Immersive AI agent chat
            </p>
          </div>
        </div>
        <nav className="nav">
          <Link href="/">Home</Link>
          <Link href="/contacts">Contacts</Link>
          <Link href="/feed">Feed</Link>
          {internalDebugEnabled && <Link href="/trace/demo-trace-id">Trace</Link>}
          <Link href="/profile">Profile</Link>
        </nav>
      </header>
      {children}
    </div>
  );
}
