import asyncio
import sys
from pathlib import Path

sys.path.append(str(Path(__file__).resolve().parents[1]))

from orchestrator.app.orchestrator import OrchestratorEngine
import orchestrator.app.orchestrator as orch_module


def test_run_chat_multi_step(monkeypatch):
    calls = {"count": 0}

    def fake_completion(messages, tools=None, tool_choice=None):
        calls["count"] += 1
        if calls["count"] == 1:
            return {
                "choices": [
                    {
                        "message": {
                            "tool_calls": [
                                {
                                    "id": "tool-1",
                                    "function": {"name": "get_contacts", "arguments": "{\"user_id\":\"u1\"}"}
                                }
                            ]
                        }
                    }
                ]
            }
        return {"choices": [{"message": {"content": "final reply"}}]}

    class DummyExecutor:
        async def execute(self, name, arguments):
            return {"ok": True}

    monkeypatch.setattr(orch_module, "run_completion", fake_completion)
    engine = OrchestratorEngine(max_steps=2, max_tool_calls=1)
    engine.executor = DummyExecutor()

    result = asyncio.get_event_loop().run_until_complete(
        engine.run_chat([{"role": "user", "content": "hi"}], "persona", ["u1", "u2"], "u1", rounds=2)
    )
    reply_lines = [line for line in result["reply"].splitlines() if line.strip()]
    assert reply_lines == ["final reply", "final reply"]
    assert result["tool_calls"][0]["name"] == "get_contacts"


def test_director_roles(monkeypatch):
    def fake_completion(messages, tools=None, tool_choice=None):
        assert any("Memory for speakers" in msg.get("content", "") for msg in messages if msg.get("role") == "system")
        return {"choices": [{"message": {"content": "[self-agent] hello"}}]}

    class DummyExecutor:
        async def execute(self, name, arguments):
            return {"ok": True}

    monkeypatch.setattr(orch_module, "run_completion", fake_completion)
    async def fake_load_memories(role_map):
        return {"self-agent": "FEED_CREATED: {\"feedId\":\"f1\"}"}
    engine = OrchestratorEngine(max_steps=1, max_tool_calls=1)
    engine.executor = DummyExecutor()
    monkeypatch.setattr(engine, "_load_memories", fake_load_memories)

    result = asyncio.get_event_loop().run_until_complete(
        engine.run_chat([{"role": "user", "content": "hi"}], "persona", ["u1", "u2"], "u1", rounds=2)
    )
    assert "[self-agent]" in result["reply"]
