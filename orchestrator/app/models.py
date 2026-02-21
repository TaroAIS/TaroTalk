from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any


class Message(BaseModel):
    role: str
    content: str


class A2AChatRequest(BaseModel):
    conversation_id: str
    participants: List[str]
    sender_id: Optional[str] = None
    persona_summary: Optional[str] = None
    world_id: Optional[str] = None
    context_window: Optional[int] = None
    intent: Optional[str] = None
    messages: List[Message]


class ToolCall(BaseModel):
    name: str
    arguments: Dict[str, Any]


class Turn(BaseModel):
    round: int
    role: str
    user_id: str
    content: str


class A2AChatResponse(BaseModel):
    reply: str
    tool_calls: List[ToolCall] = Field(default_factory=list)
    turns: List[Turn] = Field(default_factory=list)
    role_user_map: Dict[str, str] = Field(default_factory=dict)
    director_trace: Dict[str, Any] = Field(default_factory=dict)
    state_effects: List[Dict[str, Any]] = Field(default_factory=list)
    trace_id: Optional[str] = None


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
    user_id: Optional[str] = None
    world_id: Optional[str] = None
    trigger_type: Optional[str] = None
    objective: Optional[str] = None
    actors: List[str] = Field(default_factory=list)
    priority: Optional[int] = None


class SimulateResponse(BaseModel):
    status: str
    workflow_id: Optional[str] = None
    scheduled_events: List[Dict[str, Any]] = Field(default_factory=list)


class WhatIfRequest(BaseModel):
    user_id: Optional[str] = None
    world_id: Optional[str] = None
    trigger_type: Optional[str] = None
    objective: Optional[str] = None
    actors: List[str] = Field(default_factory=list)
    priority: Optional[int] = None
    branch_count: int = 3
    selection_policy: str = "max_score"
    dry_run: bool = True


class WhatIfBranch(BaseModel):
    branch_id: str
    score: float
    events: List[Dict[str, Any]] = Field(default_factory=list)
    reason: str


class WhatIfResponse(BaseModel):
    branches: List[WhatIfBranch] = Field(default_factory=list)
    recommended_branch_id: Optional[str] = None
    selection_policy: str = "max_score"
    dry_run: bool = True
    trace_id: Optional[str] = None
