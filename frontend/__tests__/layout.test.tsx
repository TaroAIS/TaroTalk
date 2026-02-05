import { render, screen } from "@testing-library/react";
import Layout from "../components/Layout";

it("renders brand", () => {
  render(
    <Layout>
      <div>Child</div>
    </Layout>
  );
  expect(screen.getByText("Tarotalk")).toBeInTheDocument();
});
