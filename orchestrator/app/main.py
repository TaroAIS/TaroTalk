from fastapi import FastAPI, APIRouter
from fastapi.responses import JSONResponse
import httpx
import uuid
from .config import settings
from .models import (
    A2AChatRequest,
    A2AChatResponse,
    BootstrapRequest,
    BootstrapResponse,
    AgentProfile,
    SimulateRequest,
    SimulateResponse,
)
from .tools import tool_registry
from .orchestrator import OrchestratorEngine

app = FastAPI(title="Tarotalk Orchestrator")
router = APIRouter(prefix="/a2a")
engine = OrchestratorEngine()


async def create_user(client: httpx.AsyncClient, nickname: str, user_type: str, owner_user_id: str | None):
    payload = {
        "nickname": nickname,
        "userType": user_type,
        "ownerUserId": owner_user_id,
    }
    response = await client.post(f"{settings.user_service_url}/api/users", json=payload)
    response.raise_for_status()
    return response.json()["data"]


async def create_contact(client: httpx.AsyncClient, user_id: str, contact_user_id: str, group_name: str | None):
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
    async with httpx.AsyncClient() as client:
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
    messages = [msg.model_dump() for msg in request.messages]
    result = await engine.run_chat(messages, request.persona_summary, request.participants, rounds=2)
    trace_id = str(uuid.uuid4())
    return A2AChatResponse(reply=result["reply"], tool_calls=result["tool_calls"], trace_id=trace_id)


@router.post("/simulate", response_model=SimulateResponse)
async def simulate(request: SimulateRequest):
    prompt = f"Generate a short action for user {request.user_id}. Objective: {request.objective or 'daily update'}"
    result = await engine.run_chat([{"role": "user", "content": prompt}], None)
    return SimulateResponse(status=result["reply"] or "ok")


app.include_router(router)
app.include_router(router, prefix="/api")
