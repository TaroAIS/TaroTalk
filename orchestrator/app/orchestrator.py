from typing import List, Dict, Any, Optional
import json
import httpx

from .llm_client import run_completion
from .tools import tool_registry
from .tool_executor import ToolExecutor
from .director import pick_round_speakers, role_style
from .config import settings


class OrchestratorEngine:
    def __init__(self, max_steps: int = 3, max_tool_calls: int = 3):
        self.max_steps = max_steps
        self.max_tool_calls = max_tool_calls
        self.tools = tool_registry()
        self.executor = ToolExecutor()

    async def run_chat(
        self,
        messages: List[Dict[str, str]],
        persona_summary: Optional[str] = None,
        participants: Optional[List[str]] = None,
        sender_id: Optional[str] = None,
        rounds: int = 2,
    ) -> Dict[str, Any]:
        system_prompt = self._build_system_prompt(persona_summary, participants)
        convo = [{"role": "system", "content": system_prompt}] + messages
        tool_calls_collected: List[Dict[str, Any]] = []
        replies: List[str] = []
        role_map = self._build_role_map(sender_id, participants or [])
        memories = await self._load_memories(role_map)

        for round_index in range(rounds):
            speakers = pick_round_speakers(participants or [], round_index)
            speaker_block = "\n".join([f"- {role}: {role_style(role)}" for role in speakers])
            memory_block = self._build_memory_block(speakers, memories)
            round_prompt = (
                "You are the director. Generate messages for the following speakers.\n"
                "Return each line as: [role] message\n"
                f"Speakers:\n{speaker_block}\n"
                f"{memory_block}"
            )
            convo.append({"role": "system", "content": round_prompt})

            for _ in range(self.max_steps):
                result = run_completion(convo, tools=self.tools, tool_choice="auto")
                choice = result["choices"][0]["message"]

                if "tool_calls" in choice and choice["tool_calls"]:
                    for tool_call in choice["tool_calls"][: self.max_tool_calls]:
                        name = tool_call.get("function", {}).get("name")
                        raw_args = tool_call.get("function", {}).get("arguments", "{}")
                        try:
                            args = json.loads(raw_args) if isinstance(raw_args, str) else raw_args
                        except json.JSONDecodeError:
                            args = {}
                        output = await self.executor.execute(name, args)
                        tool_calls_collected.append({"name": name, "arguments": args})
                        convo.append(
                            {
                                "role": "tool",
                                "content": json.dumps(output),
                                "tool_call_id": tool_call.get("id"),
                            }
                        )
                    continue

                content = choice.get("content") or ""
                if content:
                    replies.append(content)
                    convo.append({"role": "assistant", "content": content})
                    break

        return {"reply": "\n".join(replies), "tool_calls": tool_calls_collected}

    def _build_system_prompt(self, persona_summary: Optional[str], participants: Optional[List[str]]) -> str:
        base = "You are a multi-agent chat orchestrator."
        if persona_summary:
            base += f" Persona summary: {persona_summary}."
        if participants:
            base += f" Participants: {', '.join(participants)}."
        base += " Use tools when needed and answer concisely."
        return base

    def _build_role_map(self, sender_id: Optional[str], participants: List[str]) -> Dict[str, str]:
        role_map: Dict[str, str] = {}
        if sender_id:
            role_map["self-agent"] = sender_id
        roles = ["friend", "mentor", "rival"]
        for participant in participants:
            if sender_id and participant == sender_id:
                continue
            if not roles:
                break
            role_map[roles.pop(0)] = participant
        return role_map

    async def _load_memories(self, role_map: Dict[str, str]) -> Dict[str, str]:
        if not settings.notification_service_url:
            return {}
        memories: Dict[str, str] = {}
        async with httpx.AsyncClient() as client:
            for role, user_id in role_map.items():
                memory = await self._fetch_feed_memories(client, user_id)
                if memory:
                    memories[role] = memory
        return memories

    async def _fetch_feed_memories(self, client: httpx.AsyncClient, user_id: str, limit: int = 10) -> str:
        try:
            params = {
                "userId": user_id,
                "types": "FEED_CREATED,FEED_LIKED,FEED_COMMENTED",
                "limit": limit,
            }
            res = await client.get(f"{settings.notification_service_url}/api/notifications", params=params)
            payload = res.json()
            items = payload.get("data", []) if isinstance(payload, dict) else []
            lines = []
            for item in items:
                if not isinstance(item, dict):
                    continue
                ntype = item.get("type") or ""
                content = item.get("content") or item.get("title") or ""
                if content:
                    lines.append(f"{ntype}: {content}")
            return "; ".join(lines)
        except Exception:
            return ""

    def _build_memory_block(self, speakers: List[str], memories: Dict[str, str]) -> str:
        lines = []
        for role in speakers:
            memory = memories.get(role)
            if memory:
                lines.append(f"{role}: {memory}")
        if not lines:
            return ""
        return "Memory for speakers:\n" + "\n".join(lines) + "\n"
