import uuid
from typing import Optional

import httpx
from fastapi import FastAPI, APIRouter
from fastapi.responses import JSONResponse
from .config import settings
from .models import (
    A2AChatRequest,
    A2AChatResponse,
    BootstrapRequest,
    BootstrapResponse,
    AgentProfile,
    SimulateRequest,
    SimulateResponse,
    WhatIfRequest,
    WhatIfResponse,
    WhatIfBranch,
)
from .tools import tool_registry
from .orchestrator import OrchestratorEngine

app = FastAPI(title="Tarotalk Orchestrator")
router = APIRouter(prefix="/a2a")
v2_router = APIRouter(prefix="/api/v2/a2a")
engine = OrchestratorEngine()


async def create_user(client: httpx.AsyncClient, nickname: str, user_type: str, owner_user_id: Optional[str]):
    payload = {
        "nickname": nickname,
        "userType": user_type,
        "ownerUserId": owner_user_id,
    }
    response = await client.post(f"{settings.user_service_url}/api/users", json=payload)
    response.raise_for_status()
    return response.json()["data"]


async def create_contact(client: httpx.AsyncClient, user_id: str, contact_user_id: str, group_name: Optional[str]):
    payload = {
        "userId": user_id,
        "contactUserId": contact_user_id,
        "groupName": group_name,
        "blocked": False,
    }
    response = await client.post(f"{settings.contact_service_url}/api/contacts", json=payload)
    response.raise_for_status()


async def update_relationship(client: httpx.AsyncClient, user_id: str, target_id: str, rel_type: str,
                              intimacy: float, commercial: float):
    payload = {
        "targetId": target_id,
        "type": rel_type,
        "intimacyScore": intimacy,
        "interactionCount": 1,
        "commercialScore": commercial,
    }
    response = await client.post(f"{settings.relationship_service_url}/api/relationships/{user_id}", json=payload)
    response.raise_for_status()


@router.get("/tools")
async def tools():
    return JSONResponse(tool_registry())


@router.post("/bootstrap", response_model=BootstrapResponse)
async def bootstrap(request: BootstrapRequest):
    roles = ["friend", "mentor", "rival", "advertiser"]
    agents = []
    timeout = httpx.Timeout(
        timeout=settings.http_timeout_seconds,
        connect=settings.http_connect_timeout_seconds
    )
    async with httpx.AsyncClient(timeout=timeout) as client:
        # self-agent
        self_profile = await create_user(client, "Self Agent", "AI", request.user_id)
        await create_contact(client, request.user_id, self_profile["userId"], "self")
        await update_relationship(client, request.user_id, self_profile["userId"], "friend", 0.6, 0.0)
        agents.append(AgentProfile(
            user_id=self_profile["userId"],
            nickname=self_profile["nickname"],
            user_type=self_profile.get("userType", "AI"),
            role="self-agent",
        ))

        for idx in range(request.agent_count):
            role = roles[idx % len(roles)]
            nickname = f"{role.title()} Agent {idx + 1}"
            user_type = "BRAND" if role == "advertiser" else "AI"
            profile = await create_user(client, nickname, user_type, request.user_id)
            await create_contact(client, request.user_id, profile["userId"], role)
            intimacy = 0.2 if role == "rival" else 0.5
            commercial = 0.9 if role == "advertiser" else 0.0
            await update_relationship(client, request.user_id, profile["userId"], role, intimacy, commercial)
            agents.append(AgentProfile(
                user_id=profile["userId"],
                nickname=profile["nickname"],
                user_type=profile.get("userType", user_type),
                role=role,
            ))

    return BootstrapResponse(agents=agents)


@router.post("/chat", response_model=A2AChatResponse)
async def chat(request: A2AChatRequest):
    return await run_chat(request)


@v2_router.post("/chat", response_model=A2AChatResponse)
async def chat_v2(request: A2AChatRequest):
    return await run_chat(request)


async def run_chat(request: A2AChatRequest) -> A2AChatResponse:
    messages = [msg.model_dump() for msg in request.messages]
    result = await engine.run_chat(
        messages=messages,
        persona_summary=request.persona_summary,
        participants=request.participants,
        sender_id=request.sender_id,
        rounds=2,
        world_id=request.world_id,
        context_window=request.context_window,
        intent=request.intent,
        conversation_id=request.conversation_id,
    )
    trace_id = str(uuid.uuid4())
    director_trace = result.get("director_trace", {})
    if isinstance(director_trace, dict):
        director_trace["trace_id"] = trace_id
    state_effects = result.get("state_effects", [])
    if isinstance(state_effects, list):
        for effect in state_effects:
            if isinstance(effect, dict):
                effect["trace_id"] = trace_id
    safety_report = result.get("safety_report", [])
    if isinstance(safety_report, list):
        for item in safety_report:
            if isinstance(item, dict):
                item["trace_id"] = trace_id
    reply = result.get("reply", "")
    if (not reply) and result.get("turns"):
        reply = "\n".join([str(turn.get("content", "")) for turn in result.get("turns", []) if isinstance(turn, dict)])
    return A2AChatResponse(
        reply=reply,
        tool_calls=result.get("tool_calls", []),
        turns=result.get("turns", []),
        role_user_map=result.get("role_user_map", {}),
        director_trace=director_trace if isinstance(director_trace, dict) else {},
        state_effects=state_effects if isinstance(state_effects, list) else [],
        safety_report=safety_report if isinstance(safety_report, list) else [],
        trace_id=trace_id,
    )


@router.post("/simulate", response_model=SimulateResponse)
async def simulate(request: SimulateRequest):
    return await run_simulation(request)


@v2_router.post("/simulate", response_model=SimulateResponse)
async def simulate_v2(request: SimulateRequest):
    return await run_simulation(request)


@v2_router.post("/simulate/what-if", response_model=WhatIfResponse)
async def simulate_what_if(request: WhatIfRequest):
    return await run_what_if(request)


async def run_simulation(request: SimulateRequest) -> SimulateResponse:
    actors = list(request.actors or [])
    if request.user_id and request.user_id not in actors:
        actors.append(request.user_id)
    result = await engine.run_simulation(
        world_id=request.world_id,
        trigger_type=request.trigger_type,
        objective=request.objective,
        actors=actors,
        priority=request.priority,
    )
    return SimulateResponse(
        status=str(result.get("status") or "scheduled"),
        workflow_id=str(result.get("workflow_id")) if result.get("workflow_id") else None,
        scheduled_events=result.get("scheduled_events") or [],
    )


async def run_what_if(request: WhatIfRequest) -> WhatIfResponse:
    actors = list(request.actors or [])
    if request.user_id and request.user_id not in actors:
        actors.append(request.user_id)
    result = await engine.run_what_if(
        world_id=request.world_id,
        trigger_type=request.trigger_type,
        objective=request.objective,
        actors=actors,
        priority=request.priority,
        branch_count=request.branch_count,
        selection_policy=request.selection_policy,
    )
    trace_id = str(uuid.uuid4())
    await persist_branch_scenarios(
        request.world_id,
        trace_id,
        request.selection_policy,
        request.dry_run,
        result.get("branches") or [],
    )
    branches = [
        WhatIfBranch(
            branch_id=str(branch.get("branch_id") or ""),
            score=float(branch.get("score") or 0.0),
            events=branch.get("events") or [],
            reason=str(branch.get("reason") or ""),
        )
        for branch in (result.get("branches") or [])
    ]
    return WhatIfResponse(
        branches=branches,
        recommended_branch_id=str(result.get("recommended_branch_id") or "") or None,
        selection_policy=str(result.get("selection_policy") or request.selection_policy or "max_score"),
        dry_run=bool(request.dry_run),
        trace_id=trace_id,
    )


async def persist_branch_scenarios(
    world_id: Optional[str],
    trace_id: str,
    selection_policy: Optional[str],
    dry_run: bool,
    branches: list,
) -> None:
    if not world_id or not settings.world_service_url or not branches:
        return
    payload = {
        "traceId": trace_id,
        "selectionPolicy": selection_policy or "max_score",
        "dryRun": bool(dry_run),
        "branches": [
            {
                "branchId": branch.get("branch_id"),
                "score": branch.get("score"),
                "reason": branch.get("reason"),
                "events": branch.get("events") or [],
            }
            for branch in branches
            if isinstance(branch, dict)
        ],
    }
    timeout = httpx.Timeout(
        timeout=settings.http_timeout_seconds,
        connect=settings.http_connect_timeout_seconds
    )
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            await client.post(f"{settings.world_service_url}/api/v2/worlds/{world_id}/branches", json=payload)
    except Exception:
        return


app.include_router(router)
app.include_router(router, prefix="/api")
app.include_router(v2_router)
