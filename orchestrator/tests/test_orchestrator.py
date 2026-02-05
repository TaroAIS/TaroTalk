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
        engine.run_chat([{"role": "user", "content": "hi"}], "persona", ["u1", "u2"], rounds=2)
    )
    assert result["reply"] == "final reply"
    assert result["tool_calls"][0]["name"] == "get_contacts"


def test_director_roles(monkeypatch):
    def fake_completion(messages, tools=None, tool_choice=None):
        return {"choices": [{"message": {"content": "[self-agent] hello"}}]}

    class DummyExecutor:
        async def execute(self, name, arguments):
            return {"ok": True}

    monkeypatch.setattr(orch_module, "run_completion", fake_completion)
    engine = OrchestratorEngine(max_steps=1, max_tool_calls=1)
    engine.executor = DummyExecutor()

    result = asyncio.get_event_loop().run_until_complete(
        engine.run_chat([{"role": "user", "content": "hi"}], "persona", ["u1", "u2"], rounds=2)
    )
    assert "[self-agent]" in result["reply"]
