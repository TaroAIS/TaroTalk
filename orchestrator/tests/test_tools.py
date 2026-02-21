import asyncio

from orchestrator.app.tool_executor import ToolExecutor
from orchestrator.app.tools import tool_registry


def test_like_feed_tool_supports_action():
    tools = tool_registry()
    like_tool = [item["function"] for item in tools if item["function"]["name"] == "like_feed"][0]
    assert "action" in like_tool["parameters"]["properties"]


def test_like_feed_calls_v2_endpoint(monkeypatch):
    captured = {}

    class DummyResponse:
        content = b'{"ok": true}'

        def raise_for_status(self):
            return None

        def json(self):
            return {"ok": True}

    async def fake_request(self, method, url, **kwargs):  # noqa: ANN001
        captured["method"] = method
        captured["url"] = url
        captured["json"] = kwargs.get("json")
        return DummyResponse()

    monkeypatch.setattr("httpx.AsyncClient.request", fake_request)

    executor = ToolExecutor()
    result = asyncio.get_event_loop().run_until_complete(
        executor.execute("like_feed", {"feed_id": "f1", "user_id": "u1", "action": "unlike"})
    )

    assert result["ok"] is True
    assert captured["method"] == "POST"
    assert captured["url"].endswith("/api/v2/feeds/f1/like")
    assert captured["json"]["action"] == "UNLIKE"
