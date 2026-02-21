import asyncio
import sys
from pathlib import Path

sys.path.append(str(Path(__file__).resolve().parents[1]))

from orchestrator.app.orchestrator import OrchestratorEngine
import orchestrator.app.orchestrator as orch_module
from orchestrator.app.drift_guard import DriftGuard


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
        if calls["count"] == 2:
            return {"choices": [{"message": {"content": "[self-agent] final reply"}}]}
        return {"choices": [{"message": {"content": "[friend] final reply"}}]}

    class DummyExecutor:
        async def execute(self, name, arguments):
            return {"ok": True}

    monkeypatch.setattr(orch_module, "run_completion", fake_completion)
    engine = OrchestratorEngine(max_steps=2, max_tool_calls=1)
    engine.executor = DummyExecutor()
    async def fake_load_relationship_map(sender_id):
        return {
            "u2": {
                "targetId": "u2",
                "type": "friend",
                "intimacyScore": 0.8,
                "interactionCount": 5,
                "commercialScore": 0.1,
            }
        }

    async def fake_load_memories(role_map, world_id=None):
        return {
            "self-agent": [{"type": "FEED_CREATED", "feed_id": "f1", "actor_id": "u2", "summary": "first"}],
            "friend": [{"type": "FEED_LIKED", "feed_id": "f2", "actor_id": "u1", "summary": "second"}],
        }

    monkeypatch.setattr(engine, "_load_relationship_map", fake_load_relationship_map)
    monkeypatch.setattr(engine, "_load_memories", fake_load_memories)

    result = asyncio.get_event_loop().run_until_complete(
        engine.run_chat([{"role": "user", "content": "hi"}], "persona", ["u1", "u2"], "u1", rounds=2)
    )
    reply_lines = [line for line in result["reply"].splitlines() if line.strip()]
    assert reply_lines == ["[self-agent] final reply", "[friend] final reply"]
    assert result["tool_calls"][0]["name"] == "get_contacts"
    assert len(result["turns"]) == 2
    assert result["turns"][0]["role"] == "self-agent"
    assert result["turns"][0]["user_id"] == "u1"
    assert result["turns"][1]["role"] == "friend"
    assert result["turns"][1]["user_id"] == "u2"
    assert result["role_user_map"]["friend"] == "u2"


def test_drift_guard_threshold_detection():
    guard = DriftGuard()
    score = guard.score(role="friend", content="buy now limited discount deal", persona_summary="kind supportive person")
    assert guard.is_drift(score) is True


def test_director_roles(monkeypatch):
    prompts = []

    def fake_completion(messages, tools=None, tool_choice=None):
        system_prompts = [msg.get("content", "") for msg in messages if msg.get("role") == "system"]
        latest = system_prompts[-1]
        prompts.append(latest)
        assert "Memory for [self-agent]" in latest or "Memory for [mentor]" in latest
        if "Round: 1" in latest:
            return {"choices": [{"message": {"content": "[self-agent] hello"}}]}
        return {"choices": [{"message": {"content": "[mentor] got it"}}]}

    class DummyExecutor:
        async def execute(self, name, arguments):
            return {"ok": True}

    monkeypatch.setattr(orch_module, "run_completion", fake_completion)
    async def fake_load_relationship_map(sender_id):
        return {
            "u2": {
                "targetId": "u2",
                "type": "mentor",
                "intimacyScore": 0.9,
                "interactionCount": 10,
                "commercialScore": 0.0,
            },
            "u3": {
                "targetId": "u3",
                "type": "rival",
                "intimacyScore": 0.2,
                "interactionCount": 1,
                "commercialScore": 0.0,
            },
        }

    async def fake_load_memories(role_map, world_id=None):
        return {
            "self-agent": [{"type": "FEED_CREATED", "feed_id": "f1", "actor_id": "u2", "summary": "s1"}],
            "mentor": [{"type": "FEED_LIKED", "feed_id": "f2", "actor_id": "u3", "summary": "s2"}],
            "rival": [{"type": "FEED_COMMENTED", "feed_id": "f3", "actor_id": "u1", "summary": "s3"}],
        }

    engine = OrchestratorEngine(max_steps=1, max_tool_calls=1)
    engine.executor = DummyExecutor()
    monkeypatch.setattr(engine, "_load_memories", fake_load_memories)
    monkeypatch.setattr(engine, "_load_relationship_map", fake_load_relationship_map)

    result = asyncio.get_event_loop().run_until_complete(
        engine.run_chat([{"role": "user", "content": "hi"}], "persona", ["u1", "u2", "u3"], "u1", rounds=2)
    )
    assert "[self-agent]" in result["reply"]
    round_two = [prompt for prompt in prompts if "Round: 2" in prompt][0]
    assert "- mentor:" in round_two
    assert result["turns"][1]["role"] == "mentor"


def test_chat_outputs_director_trace_and_state_effects(monkeypatch):
    def fake_completion(messages, tools=None, tool_choice=None):
        return {"choices": [{"message": {"content": "[self-agent] hello there"}}]}

    class DummyExecutor:
        async def execute(self, name, arguments):
            return {"ok": True}

    monkeypatch.setattr(orch_module, "run_completion", fake_completion)
    engine = OrchestratorEngine(max_steps=1, max_tool_calls=1)
    engine.executor = DummyExecutor()

    async def fake_load_relationship_map(sender_id):
        return {}

    async def fake_load_memories(role_map, world_id=None):
        return {}

    monkeypatch.setattr(engine, "_load_relationship_map", fake_load_relationship_map)
    monkeypatch.setattr(engine, "_load_memories", fake_load_memories)

    result = asyncio.get_event_loop().run_until_complete(
        engine.run_chat(
            [{"role": "user", "content": "hi"}],
            "persona",
            ["u1"],
            "u1",
            rounds=1,
            world_id="w1",
            intent="chat",
            conversation_id="c1",
        )
    )
    assert "director_trace" in result
    assert result["director_trace"]["world_id"] == "w1"
    assert len(result["state_effects"]) >= 1
    assert result["state_effects"][0]["effect_type"] == "CHAT_MESSAGE"


def test_run_simulation_v2_shape():
    engine = OrchestratorEngine(max_steps=1, max_tool_calls=1)
    result = asyncio.get_event_loop().run_until_complete(
        engine.run_simulation(
            world_id="world-1",
            trigger_type="SCHEDULED_TICK",
            objective="advance arc",
            actors=["a1", "a2"],
            priority=80,
        )
    )
    assert result["status"] == "scheduled"
    assert result["workflow_id"]
    assert len(result["scheduled_events"]) == 2


def test_drift_regeneration_and_no_extra_call_when_stable(monkeypatch):
    calls = {"count": 0}

    def fake_completion(messages, tools=None, tool_choice=None):
        calls["count"] += 1
        if calls["count"] == 1:
            return {"choices": [{"message": {"content": "[friend] buy now discount deal"}}]}
        return {"choices": [{"message": {"content": "[friend] support and share together"}}]}

    monkeypatch.setattr(orch_module, "run_completion", fake_completion)
    engine = OrchestratorEngine(max_steps=1, max_tool_calls=1)

    async def fake_load_relationship_map(sender_id):
        return {}

    async def fake_load_memories(role_map, world_id=None):
        return {}

    monkeypatch.setattr(engine, "_load_relationship_map", fake_load_relationship_map)
    monkeypatch.setattr(engine, "_load_memories", fake_load_memories)

    result = asyncio.get_event_loop().run_until_complete(
        engine.run_chat(
            [{"role": "user", "content": "hi"}],
            "supportive friend",
            ["u2"],
            None,
            rounds=1,
        )
    )
    actions = [item.get("action") for item in result["director_trace"].get("drift_decisions", [])]
    assert "regenerate_once" in actions
    assert calls["count"] == 2
    assert "support and share together" in result["turns"][0]["content"]

    calls["count"] = 0

    def stable_completion(messages, tools=None, tool_choice=None):
        calls["count"] += 1
        return {"choices": [{"message": {"content": "[friend] support and share together"}}]}

    monkeypatch.setattr(orch_module, "run_completion", stable_completion)
    stable = asyncio.get_event_loop().run_until_complete(
        engine.run_chat(
            [{"role": "user", "content": "hello"}],
            "supportive friend",
            ["u2"],
            None,
            rounds=1,
        )
    )
    assert calls["count"] == 1
    assert stable["turns"][0]["role"] == "friend"


def test_run_what_if_returns_ranked_branches():
    engine = OrchestratorEngine(max_steps=1, max_tool_calls=1)
    result = asyncio.get_event_loop().run_until_complete(
        engine.run_what_if(
            world_id="world-1",
            trigger_type="STORY_BEAT",
            objective="keep continuity",
            actors=["a1", "a2"],
            priority=70,
            branch_count=3,
            selection_policy="max_score",
        )
    )
    assert len(result["branches"]) == 3
    assert result["recommended_branch_id"] == result["branches"][0]["branch_id"]
    assert result["branches"][0]["score"] >= result["branches"][1]["score"]
    assert result["dry_run"] is True


def test_memory_priority_world_then_notification():
    engine = OrchestratorEngine(max_steps=1, max_tool_calls=1)
    role_map = {
        "self-agent": "u1",
        "friend": "u2",
    }

    class DummyClient:
        pass

    async def fake_world(client, role, user_id, world_id, limit=5):
        if role == "self-agent":
            return role, [{"type": "WORLD_MEMORY", "summary": "from world"}]
        return role, []

    async def fake_feed(client, role, user_id, limit=10):
        return role, [{"type": "FEED_CREATED", "summary": "from feed"}]

    monkeypatch = __import__("pytest").MonkeyPatch()
    monkeypatch.setattr(engine, "_fetch_world_memories", fake_world)
    monkeypatch.setattr(engine, "_fetch_feed_memories", fake_feed)
    monkeypatch.setattr("httpx.AsyncClient", lambda *args, **kwargs: DummyAsyncClientContext(DummyClient()))
    try:
        memories = asyncio.get_event_loop().run_until_complete(engine._load_memories(role_map, "w1"))
        assert memories["self-agent"][0]["type"] == "WORLD_MEMORY"
        assert memories["friend"][0]["type"] == "FEED_CREATED"
    finally:
        monkeypatch.undo()


def test_goal_economy_changes_speaker_weight(monkeypatch):
    engine = OrchestratorEngine(max_steps=1, max_tool_calls=1)

    async def fake_load_relationship_map(sender_id):
        return {
            "u2": {
                "targetId": "u2",
                "type": "friend",
                "intimacyScore": 0.5,
                "interactionCount": 5,
                "commercialScore": 0.0,
            },
            "u3": {
                "targetId": "u3",
                "type": "mentor",
                "intimacyScore": 0.5,
                "interactionCount": 5,
                "commercialScore": 0.0,
            },
        }

    async def fake_load_goal_utilities(world_id):
        return {"u2": 0.1, "u3": 0.9}

    monkeypatch.setattr(engine, "_load_relationship_map", fake_load_relationship_map)
    monkeypatch.setattr(engine, "_load_goal_utilities", fake_load_goal_utilities)

    bindings = asyncio.get_event_loop().run_until_complete(
        engine._build_role_bindings("u1", ["u1", "u2", "u3"], "world-1")
    )
    binding_map = {item["user_id"]: item for item in bindings if item.get("user_id") != "u1"}
    assert binding_map["u3"]["goal_utility"] > binding_map["u2"]["goal_utility"]
    assert binding_map["u3"]["weight"] > binding_map["u2"]["weight"]


def test_safety_linter_blocks_illegal_relationship_tool(monkeypatch):
    calls = {"count": 0, "executed": 0}

    def fake_completion(messages, tools=None, tool_choice=None):
        calls["count"] += 1
        if calls["count"] == 1:
            return {
                "choices": [
                    {
                        "message": {
                            "tool_calls": [
                                {
                                    "id": "tc-1",
                                    "function": {
                                        "name": "update_relationship",
                                        "arguments": "{\"user_id\":\"u1\",\"target_id\":\"u2\",\"type\":\"friend;drop table\"}",
                                    },
                                }
                            ]
                        }
                    }
                ]
            }
        return {"choices": [{"message": {"content": "[self-agent] hello"}}]}

    class DummyExecutor:
        async def execute(self, name, arguments):
            calls["executed"] += 1
            return {"ok": True}

    async def fake_load_relationship_map(sender_id):
        return {"u2": {"targetId": "u2", "type": "friend", "intimacyScore": 0.7, "interactionCount": 4, "commercialScore": 0.0}}

    async def fake_load_memories(role_map, world_id=None):
        return {}

    monkeypatch.setattr(orch_module, "run_completion", fake_completion)
    engine = OrchestratorEngine(max_steps=2, max_tool_calls=1)
    engine.executor = DummyExecutor()
    monkeypatch.setattr(engine, "_load_relationship_map", fake_load_relationship_map)
    monkeypatch.setattr(engine, "_load_memories", fake_load_memories)

    result = asyncio.get_event_loop().run_until_complete(
        engine.run_chat([{"role": "user", "content": "hi"}], "persona", ["u1", "u2"], "u1", rounds=1, world_id="w1")
    )

    assert calls["executed"] == 0
    assert result["tool_calls"] == []
    assert any(item.get("action") == "SAFETY_BLOCKED" for item in result.get("safety_report", []))
    assert any(item.get("rule") == "RELATIONSHIP_TYPE_WHITELIST" for item in result.get("safety_report", []))


def test_safety_linter_warns_on_long_state_effect_content(monkeypatch):
    long_message = "x" * 900

    def fake_completion(messages, tools=None, tool_choice=None):
        return {"choices": [{"message": {"content": f"[self-agent] {long_message}"}}]}

    async def fake_load_relationship_map(sender_id):
        return {}

    async def fake_load_memories(role_map, world_id=None):
        return {}

    monkeypatch.setattr(orch_module, "run_completion", fake_completion)
    engine = OrchestratorEngine(max_steps=1, max_tool_calls=1)
    monkeypatch.setattr(engine, "_load_relationship_map", fake_load_relationship_map)
    monkeypatch.setattr(engine, "_load_memories", fake_load_memories)

    result = asyncio.get_event_loop().run_until_complete(
        engine.run_chat([{"role": "user", "content": "hello"}], "persona", ["u1"], "u1", rounds=1, world_id="w1")
    )

    assert len(result["state_effects"]) == 1
    assert any(item.get("action") == "SOFT_WARNING" for item in result.get("safety_report", []))
    assert any(item.get("rule") == "CONTENT_LENGTH_HIGH" for item in result.get("safety_report", []))
    assert len(result["director_trace"].get("safety_report", [])) >= 1


class DummyAsyncClientContext:
    def __init__(self, client):
        self.client = client

    async def __aenter__(self):
        return self.client

    async def __aexit__(self, exc_type, exc, tb):
        return False
