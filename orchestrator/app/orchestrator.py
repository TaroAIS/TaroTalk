from typing import List, Dict, Any, Optional
import asyncio
import json
import re
import httpx

from .llm_client import run_completion
from .tools import tool_registry
from .tool_executor import ToolExecutor
from .director import pick_round_speakers, role_style
from .config import settings

ROLE_CANDIDATES = ["friend", "mentor", "rival", "advertiser"]


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
        turns: List[Dict[str, Any]] = []
        bindings = await self._build_role_bindings(sender_id, participants or [])
        role_map = {item["role"]: item["user_id"] for item in bindings}
        memories = await self._load_memories(role_map)

        for round_index in range(rounds):
            speakers = pick_round_speakers(bindings, round_index)
            speaker_roles = [speaker["role"] for speaker in speakers]
            speaker_block = "\n".join([f"- {speaker['role']}: {role_style(speaker['role'])}" for speaker in speakers])
            memory_block = self._build_memory_block(speakers, memories)
            round_prompt = (
                "You are the director. Generate messages for the following speakers.\n"
                "Return each line as: [role] message\n"
                f"Round: {round_index + 1}\n"
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
                    turns.extend(self._parse_turns(content, round_index + 1, role_map, speaker_roles))
                    convo.append({"role": "assistant", "content": content})
                    break

        return {
            "reply": "\n".join(replies),
            "tool_calls": tool_calls_collected,
            "turns": turns,
            "role_user_map": role_map,
        }

    def _build_system_prompt(self, persona_summary: Optional[str], participants: Optional[List[str]]) -> str:
        base = "You are a multi-agent chat orchestrator."
        if persona_summary:
            base += f" Persona summary: {persona_summary}."
        if participants:
            base += f" Participants: {', '.join(participants)}."
        base += " Use tools when needed and answer concisely."
        return base

    async def _build_role_bindings(self, sender_id: Optional[str], participants: List[str]) -> List[Dict[str, Any]]:
        ordered_participants = [str(participant) for participant in participants]
        bindings: List[Dict[str, Any]] = []
        relationship_map = await self._load_relationship_map(sender_id)

        if sender_id:
            bindings.append(
                {
                    "role": "self-agent",
                    "user_id": str(sender_id),
                    "relationship_type": "self",
                    "intimacy": 1.0,
                    "commercial": 0.0,
                    "weight": 10.0,
                    "order": 0,
                }
            )

        role_index = 0
        for order, participant in enumerate(ordered_participants, start=1):
            if sender_id and participant == str(sender_id):
                continue

            relation = relationship_map.get(participant, {})
            relation_type = str(relation.get("type") or "").strip().lower()
            if relation_type and relation_type != "self":
                role = relation_type
            else:
                role = ROLE_CANDIDATES[min(role_index, len(ROLE_CANDIDATES) - 1)]
                role_index += 1

            role = self._allocate_role_name(role, bindings)

            weight = self._calculate_weight(relation)
            bindings.append(
                {
                    "role": role,
                    "user_id": participant,
                    "relationship_type": relation_type or role,
                    "intimacy": float(relation.get("intimacyScore") or 0.0),
                    "commercial": float(relation.get("commercialScore") or 0.0),
                    "weight": weight,
                    "order": order,
                }
            )

        return bindings

    def _allocate_role_name(self, candidate: str, bindings: List[Dict[str, Any]]) -> str:
        existing = {item.get("role") for item in bindings}
        if candidate not in existing:
            return candidate

        index = 2
        while True:
            role_name = f"{candidate}-{index}"
            if role_name not in existing:
                return role_name
            index += 1

    async def _load_relationship_map(self, sender_id: Optional[str]) -> Dict[str, Dict[str, Any]]:
        if not sender_id or not settings.relationship_service_url:
            return {}

        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(f"{settings.relationship_service_url}/api/relationships/{sender_id}")
                payload = response.json()
                rows = payload.get("data", []) if isinstance(payload, dict) else []
                relation_map: Dict[str, Dict[str, Any]] = {}
                for row in rows:
                    if not isinstance(row, dict):
                        continue
                    target_id = row.get("targetId")
                    if not target_id:
                        continue
                    relation_map[str(target_id)] = row
                return relation_map
        except Exception:
            return {}

    def _calculate_weight(self, relation: Dict[str, Any]) -> float:
        intimacy = float(relation.get("intimacyScore") or 0.0)
        commercial = float(relation.get("commercialScore") or 0.0)
        interactions = float(relation.get("interactionCount") or 0.0)
        return intimacy * 0.7 + commercial * 0.2 + min(interactions / 50.0, 1.0) * 0.1

    async def _load_memories(self, role_map: Dict[str, str]) -> Dict[str, List[Dict[str, str]]]:
        if not settings.notification_service_url:
            return {}
        memories: Dict[str, List[Dict[str, str]]] = {}
        async with httpx.AsyncClient() as client:
            tasks = [self._fetch_feed_memories(client, role, user_id) for role, user_id in role_map.items()]
            results = await asyncio.gather(*tasks)
            for role, memory in results:
                if memory:
                    memories[role] = memory
        return memories

    async def _fetch_feed_memories(
        self,
        client: httpx.AsyncClient,
        role: str,
        user_id: str,
        limit: int = 10,
    ) -> Any:
        try:
            params = {
                "userId": user_id,
                "types": "FEED_CREATED,FEED_LIKED,FEED_COMMENTED",
                "limit": limit,
            }
            res = await client.get(f"{settings.notification_service_url}/api/notifications", params=params)
            payload = res.json()
            items = payload.get("data", []) if isinstance(payload, dict) else []
            dedup = set()
            memories: List[Dict[str, str]] = []
            for item in items:
                if not isinstance(item, dict):
                    continue
                memory = self._normalize_memory_item(item)
                dedup_key = (memory["type"], memory.get("feed_id", ""), memory.get("actor_id", ""))
                if dedup_key in dedup:
                    continue
                dedup.add(dedup_key)
                memories.append(memory)
                if len(memories) >= 5:
                    break
            return role, memories
        except Exception:
            return role, []

    def _normalize_memory_item(self, item: Dict[str, Any]) -> Dict[str, str]:
        ntype = str(item.get("type") or "")
        content = item.get("content")
        title = str(item.get("title") or "")

        if isinstance(content, str):
            try:
                parsed = json.loads(content)
                if isinstance(parsed, dict):
                    summary = str(parsed.get("summary") or title or "")
                    feed_id = str(parsed.get("feedId") or "")
                    author_id = str(parsed.get("authorId") or "")
                    actor_id = str(parsed.get("actorId") or "")
                    return {
                        "type": ntype,
                        "feed_id": feed_id,
                        "author_id": author_id,
                        "actor_id": actor_id,
                        "summary": summary,
                    }
            except Exception:
                pass

        plain = str(content or title)
        return {
            "type": ntype,
            "feed_id": "",
            "author_id": "",
            "actor_id": "",
            "summary": plain,
        }

    def _build_memory_block(self, speakers: List[Dict[str, Any]], memories: Dict[str, List[Dict[str, str]]]) -> str:
        lines = []
        for speaker in speakers:
            role = speaker["role"]
            role_memories = memories.get(role, [])
            if not role_memories:
                continue
            lines.append(f"Memory for [{role}]:")
            for memory in role_memories:
                summary = memory.get("summary", "")
                feed_id = memory.get("feed_id", "")
                actor_id = memory.get("actor_id", "")
                lines.append(
                    f"- type={memory.get('type', '')}, feed_id={feed_id}, actor_id={actor_id}, summary={summary}"
                )
        if not lines:
            return ""
        return "\n".join(lines) + "\n"

    def _parse_turns(
        self,
        content: str,
        round_number: int,
        role_map: Dict[str, str],
        allowed_roles: List[str],
    ) -> List[Dict[str, Any]]:
        turns: List[Dict[str, Any]] = []
        line_pattern = re.compile(r"^\[(?P<role>[^\]]+)\]\s*(?P<message>.+)$")
        allowed_set = set(allowed_roles)

        for raw_line in content.splitlines():
            line = raw_line.strip()
            if not line:
                continue
            match = line_pattern.match(line)
            if not match:
                continue
            role = match.group("role").strip()
            message = match.group("message").strip()
            if not message or role not in role_map:
                continue
            if allowed_set and role not in allowed_set:
                continue
            turns.append(
                {
                    "round": round_number,
                    "role": role,
                    "user_id": role_map[role],
                    "content": message,
                }
            )
        return turns
