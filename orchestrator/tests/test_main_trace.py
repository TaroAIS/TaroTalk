import asyncio
import sys
from pathlib import Path

sys.path.append(str(Path(__file__).resolve().parents[1]))

import orchestrator.app.main as main_module
from orchestrator.app.models import A2AChatRequest, Message, SimulateRequest, WhatIfRequest


def test_run_chat_prefers_request_trace_id(monkeypatch):
    async def fake_run_chat(**kwargs):
        return {
            "reply": "[self-agent] hello",
            "tool_calls": [],
            "turns": [{"round": 1, "role": "self-agent", "user_id": "u1", "content": "hello"}],
            "role_user_map": {"self-agent": "u1"},
            "director_trace": {"rounds": []},
            "state_effects": [{"effect_type": "CHAT_MESSAGE"}],
            "safety_report": [{"rule": "noop"}],
        }

    monkeypatch.setattr(main_module.engine, "run_chat", fake_run_chat)
    request = A2AChatRequest(
        conversation_id="c1",
        participants=["u1"],
        sender_id="u1",
        messages=[Message(role="user", content="hi")],
        trace_id="trace-chat-fixed",
    )

    response = asyncio.get_event_loop().run_until_complete(main_module.run_chat(request))
    assert response.trace_id == "trace-chat-fixed"
    assert response.director_trace["trace_id"] == "trace-chat-fixed"
    assert response.state_effects[0]["trace_id"] == "trace-chat-fixed"
    assert response.safety_report[0]["trace_id"] == "trace-chat-fixed"


def test_run_simulation_propagates_trace_to_events(monkeypatch):
    async def fake_run_simulation(**kwargs):
        return {
            "status": "scheduled",
            "workflow_id": "wf-1",
            "scheduled_events": [{"event_type": "WORLD_EVOLUTION"}],
        }

    monkeypatch.setattr(main_module.engine, "run_simulation", fake_run_simulation)
    request = SimulateRequest(
        world_id="world-1",
        actors=["u1"],
        trace_id="trace-sim-fixed",
    )

    response = asyncio.get_event_loop().run_until_complete(main_module.run_simulation(request))
    assert response.workflow_id == "wf-1"
    assert response.scheduled_events[0]["trace_id"] == "trace-sim-fixed"


def test_run_what_if_prefers_request_trace_and_persists(monkeypatch):
    async def fake_run_what_if(**kwargs):
        return {
            "branches": [
                {
                    "branch_id": "branch-1",
                    "score": 0.8,
                    "events": [{"event_type": "WORLD_EVOLUTION"}],
                    "reason": "higher continuity score",
                }
            ],
            "recommended_branch_id": "branch-1",
            "selection_policy": "max_score",
        }

    captured = {}

    async def fake_persist(world_id, trace_id, selection_policy, dry_run, branches):
        captured["world_id"] = world_id
        captured["trace_id"] = trace_id
        captured["selection_policy"] = selection_policy
        captured["dry_run"] = dry_run
        captured["branches"] = branches

    monkeypatch.setattr(main_module.engine, "run_what_if", fake_run_what_if)
    monkeypatch.setattr(main_module, "persist_branch_scenarios", fake_persist)

    request = WhatIfRequest(
        world_id="world-1",
        actors=["u1"],
        trace_id="trace-whatif-fixed",
        branch_count=1,
        selection_policy="max_score",
        dry_run=True,
    )

    response = asyncio.get_event_loop().run_until_complete(main_module.run_what_if(request))
    assert response.trace_id == "trace-whatif-fixed"
    assert response.branches[0].events[0]["trace_id"] == "trace-whatif-fixed"
    assert captured["trace_id"] == "trace-whatif-fixed"
    assert captured["world_id"] == "world-1"
