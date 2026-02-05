from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any


class Message(BaseModel):
    role: str
    content: str


class A2AChatRequest(BaseModel):
    conversation_id: str
    participants: List[str]
    persona_summary: Optional[str] = None
    messages: List[Message]


class ToolCall(BaseModel):
    name: str
    arguments: Dict[str, Any]


class A2AChatResponse(BaseModel):
    reply: str
    tool_calls: List[ToolCall] = Field(default_factory=list)


class BootstrapRequest(BaseModel):
    user_id: str
    persona_summary: str
    traits: Optional[Dict[str, Any]] = None
    agent_count: int = 5


class AgentProfile(BaseModel):
    user_id: str
    nickname: str
    user_type: str
    role: str


class BootstrapResponse(BaseModel):
    agents: List[AgentProfile]


class SimulateRequest(BaseModel):
    user_id: str
    objective: Optional[str] = None


class SimulateResponse(BaseModel):
    status: str
