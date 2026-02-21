import { render, screen } from "@testing-library/react";
import Layout from "../components/Layout";

describe("layout", () => {
  const originalDebug = process.env.NEXT_PUBLIC_INTERNAL_DEBUG;

  afterEach(() => {
    process.env.NEXT_PUBLIC_INTERNAL_DEBUG = originalDebug;
  });

  it("renders brand", () => {
    render(
      <Layout>
        <div>Child</div>
      </Layout>
    );
    expect(screen.getByText("Tarotalk")).toBeInTheDocument();
  });

  it("hides trace entry when internal debug is disabled", () => {
    process.env.NEXT_PUBLIC_INTERNAL_DEBUG = "false";
    render(
      <Layout>
        <div>Child</div>
      </Layout>
    );
    expect(screen.queryByText("Trace")).not.toBeInTheDocument();
  });

  it("shows trace entry when internal debug is enabled", () => {
    process.env.NEXT_PUBLIC_INTERNAL_DEBUG = "true";
    render(
      <Layout>
        <div>Child</div>
      </Layout>
    );
    expect(screen.getByText("Trace")).toBeInTheDocument();
  });
});
