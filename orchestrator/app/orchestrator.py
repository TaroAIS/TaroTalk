from typing import List, Dict, Any, Optional
import asyncio
import hashlib
import json
import re
import uuid
import httpx

from .llm_client import run_completion
from .tools import tool_registry
from .tool_executor import ToolExecutor
from .director import pick_round_speakers, role_style
from .drift_guard import DriftGuard
from .config import settings

ROLE_CANDIDATES = ["friend", "mentor", "rival", "advertiser"]


class OrchestratorEngine:
    def __init__(self, max_steps: int = 3, max_tool_calls: int = 3):
        self.max_steps = max_steps
        self.max_tool_calls = max_tool_calls
        self.tools = tool_registry()
        self.executor = ToolExecutor()
        self.drift_guard = DriftGuard()

    async def run_chat(
        self,
        messages: List[Dict[str, str]],
        persona_summary: Optional[str] = None,
        participants: Optional[List[str]] = None,
        sender_id: Optional[str] = None,
        rounds: int = 2,
        world_id: Optional[str] = None,
        context_window: Optional[int] = None,
        intent: Optional[str] = None,
        conversation_id: Optional[str] = None,
    ) -> Dict[str, Any]:
        seed_messages = messages
        if context_window and context_window > 0:
            seed_messages = messages[-context_window:]

        system_prompt = self._build_system_prompt(persona_summary, participants, world_id, intent)
        convo = [{"role": "system", "content": system_prompt}] + seed_messages
        tool_calls_collected: List[Dict[str, Any]] = []
        replies: List[str] = []
        turns: List[Dict[str, Any]] = []
        director_rounds: List[Dict[str, Any]] = []
        drift_decisions: List[Dict[str, Any]] = []
        recent_turns_by_role: Dict[str, List[str]] = {}
        bindings = await self._build_role_bindings(sender_id, participants or [], world_id)
        role_map = {item["role"]: item["user_id"] for item in bindings}
        memories = await self._load_memories(role_map, world_id)

        for round_index in range(rounds):
            speakers = pick_round_speakers(bindings, round_index)
            if not speakers:
                continue
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
            round_trace: Dict[str, Any] = {
                "round": round_index + 1,
                "speakers": [
                    {
                        "role": speaker.get("role"),
                        "user_id": speaker.get("user_id"),
                        "weight": float(speaker.get("weight", 0.0)),
                    }
                    for speaker in speakers
                ],
                "tool_calls": [],
                "drift_decisions": [],
            }

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
                        tool_record = {"name": name, "arguments": args}
                        tool_calls_collected.append(tool_record)
                        round_trace["tool_calls"].append(tool_record)
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
                    parsed_turns = self._parse_turns(content, round_index + 1, role_map, speaker_roles)
                    if not parsed_turns and speakers:
                        parsed_turns = [
                            {
                                "round": round_index + 1,
                                "role": speakers[0]["role"],
                                "user_id": speakers[0]["user_id"],
                                "content": content.strip(),
                            }
                        ]
                    parsed_turns, turn_decisions = self._guard_turns(
                        parsed_turns=parsed_turns,
                        persona_summary=persona_summary,
                        recent_turns_by_role=recent_turns_by_role,
                        convo=convo,
                        round_number=round_index + 1,
                        role_map=role_map,
                        allowed_roles=speaker_roles,
                        round_trace=round_trace,
                    )
                    turns.extend(parsed_turns)
                    drift_decisions.extend(turn_decisions)
                    for turn in parsed_turns:
                        role = str(turn.get("role") or "")
                        if not role:
                            continue
                        recent_turns_by_role.setdefault(role, []).append(str(turn.get("content") or ""))
                    round_trace["generated_turns"] = len(parsed_turns)
                    round_trace["raw_content"] = content
                    convo.append({"role": "assistant", "content": content})
                    break

            director_rounds.append(round_trace)

        return {
            "reply": "\n".join(replies),
            "tool_calls": tool_calls_collected,
            "turns": turns,
            "role_user_map": role_map,
            "director_trace": {
                "world_id": world_id,
                "intent": intent,
                "bindings": bindings,
                "rounds": director_rounds,
                "drift_decisions": drift_decisions,
            },
            "state_effects": self._build_state_effects(turns, tool_calls_collected, conversation_id, world_id),
        }

    async def run_simulation(
        self,
        world_id: Optional[str] = None,
        trigger_type: Optional[str] = None,
        objective: Optional[str] = None,
        actors: Optional[List[str]] = None,
        priority: Optional[int] = None,
    ) -> Dict[str, Any]:
        workflow_id = str(uuid.uuid4())
        actor_list = [str(actor) for actor in (actors or []) if str(actor).strip()]
        if not actor_list:
            actor_list = ["director"]
        effective_priority = 50 if priority is None else max(1, min(priority, 100))
        effective_trigger = trigger_type or "SCHEDULED_TICK"
        effective_objective = objective or "advance social continuity"

        scheduled_events = []
        for index, actor in enumerate(actor_list):
            scheduled_events.append(
                {
                    "sequence": index + 1,
                    "event_type": "WORLD_EVOLUTION",
                    "trigger_type": effective_trigger,
                    "objective": effective_objective,
                    "actor_id": actor,
                    "priority": effective_priority,
                    "world_id": world_id,
                }
            )

        return {
            "workflow_id": workflow_id,
            "scheduled_events": scheduled_events,
            "status": "scheduled",
        }

    async def run_what_if(
        self,
        world_id: Optional[str] = None,
        trigger_type: Optional[str] = None,
        objective: Optional[str] = None,
        actors: Optional[List[str]] = None,
        priority: Optional[int] = None,
        branch_count: int = 3,
        selection_policy: str = "max_score",
    ) -> Dict[str, Any]:
        count = max(1, min(int(branch_count or 3), 5))
        actor_list = [str(actor) for actor in (actors or []) if str(actor).strip()]
        if not actor_list:
            actor_list = ["director"]

        base_trigger = trigger_type or "WHAT_IF"
        base_objective = objective or "evaluate alternate social outcomes"
        effective_priority = 50 if priority is None else max(1, min(priority, 100))

        branches: List[Dict[str, Any]] = []
        for idx in range(count):
            branch_id = f"branch-{idx + 1}"
            score = self._score_branch(base_objective, actor_list, idx)
            events = []
            for event_idx, actor in enumerate(actor_list):
                events.append(
                    {
                        "sequence": event_idx + 1,
                        "event_type": "WORLD_EVOLUTION",
                        "trigger_type": base_trigger,
                        "objective": base_objective,
                        "actor_id": actor,
                        "priority": effective_priority,
                        "world_id": world_id,
                        "branch_id": branch_id,
                    }
                )
            branches.append(
                {
                    "branch_id": branch_id,
                    "score": score,
                    "events": events,
                    "reason": "higher continuity score" if score >= 0.5 else "lower disruption score",
                }
            )

        policy = (selection_policy or "max_score").strip().lower()
        if policy == "min_risk":
            branches.sort(key=lambda item: (item.get("score", 0.0), item.get("branch_id", "")), reverse=False)
        else:
            branches.sort(key=lambda item: (item.get("score", 0.0), item.get("branch_id", "")), reverse=True)

        recommended = branches[0]["branch_id"] if branches else None
        return {
            "branches": branches,
            "recommended_branch_id": recommended,
            "selection_policy": policy,
            "dry_run": True,
        }

    def _build_system_prompt(
        self,
        persona_summary: Optional[str],
        participants: Optional[List[str]],
        world_id: Optional[str],
        intent: Optional[str],
    ) -> str:
        base = "You are a multi-agent chat orchestrator."
        if persona_summary:
            base += f" Persona summary: {persona_summary}."
        if participants:
            base += f" Participants: {', '.join(participants)}."
        if world_id:
            base += f" World ID: {world_id}."
        if intent:
            base += f" Intent: {intent}."
        base += " Use tools when needed and answer concisely."
        return base

    async def _build_role_bindings(
        self,
        sender_id: Optional[str],
        participants: List[str],
        world_id: Optional[str] = None,
    ) -> List[Dict[str, Any]]:
        ordered_participants = [str(participant) for participant in participants]
        bindings: List[Dict[str, Any]] = []
        relationship_map = await self._load_relationship_map(sender_id)
        should_load_goals = bool(world_id) and any(str(sender_id) != participant for participant in ordered_participants)
        goal_utilities = await self._load_goal_utilities(world_id) if should_load_goals else {}
        peer_count = max(1, len([item for item in ordered_participants if str(sender_id) != item]))

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

            relation_weight = self._calculate_weight(relation)
            goal_utility = float(goal_utilities.get(participant, 0.0))
            recency_factor = max(0.0, 1.0 - ((order - 1) / float(peer_count)))
            weight = relation_weight + goal_utility + recency_factor
            bindings.append(
                {
                    "role": role,
                    "user_id": participant,
                    "relationship_type": relation_type or role,
                    "intimacy": float(relation.get("intimacyScore") or 0.0),
                    "commercial": float(relation.get("commercialScore") or 0.0),
                    "relation_weight": relation_weight,
                    "goal_utility": goal_utility,
                    "recency_factor": recency_factor,
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
            async with httpx.AsyncClient(timeout=self._http_timeout()) as client:
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

    async def _load_goal_utilities(self, world_id: Optional[str]) -> Dict[str, float]:
        if not world_id or not settings.world_service_url:
            return {}

        try:
            async with httpx.AsyncClient(timeout=self._http_timeout()) as client:
                response = await client.get(f"{settings.world_service_url}/api/v2/worlds/{world_id}/goals/economy")
                payload = response.json()
                rows = payload.get("data", []) if isinstance(payload, dict) else []
                goal_map: Dict[str, float] = {}
                for row in rows:
                    if not isinstance(row, dict):
                        continue
                    agent_id = row.get("agentId") or row.get("agent_id")
                    if not agent_id:
                        continue
                    try:
                        utility = float(row.get("utility") or 0.0)
                    except Exception:
                        utility = 0.0
                    goal_map[str(agent_id)] = utility
                return goal_map
        except Exception:
            return {}

    def _calculate_weight(self, relation: Dict[str, Any]) -> float:
        intimacy = float(relation.get("intimacyScore") or 0.0)
        commercial = float(relation.get("commercialScore") or 0.0)
        interactions = float(relation.get("interactionCount") or 0.0)
        return intimacy * 0.7 + commercial * 0.2 + min(interactions / 50.0, 1.0) * 0.1

    async def _load_memories(self, role_map: Dict[str, str], world_id: Optional[str] = None) -> Dict[str, List[Dict[str, str]]]:
        memories: Dict[str, List[Dict[str, str]]] = {}

        async with httpx.AsyncClient(timeout=self._http_timeout()) as client:
            if world_id and settings.world_service_url:
                world_tasks = [
                    self._fetch_world_memories(client, role, user_id, world_id)
                    for role, user_id in role_map.items()
                ]
                world_results = await asyncio.gather(*world_tasks)
                for role, rows in world_results:
                    if rows:
                        memories[role] = rows

            if settings.notification_service_url:
                fallback_tasks = []
                for role, user_id in role_map.items():
                    if role in memories and memories[role]:
                        continue
                    fallback_tasks.append(self._fetch_feed_memories(client, role, user_id))
                fallback_results = await asyncio.gather(*fallback_tasks)
                for role, rows in fallback_results:
                    if rows:
                        memories[role] = rows

        return memories

    def _http_timeout(self) -> httpx.Timeout:
        return httpx.Timeout(
            timeout=settings.http_timeout_seconds,
            connect=settings.http_connect_timeout_seconds
        )

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

    async def _fetch_world_memories(
        self,
        client: httpx.AsyncClient,
        role: str,
        user_id: str,
        world_id: str,
        limit: int = 5,
    ) -> Any:
        try:
            params = {
                "ownerId": user_id,
                "limit": limit,
                "minSalience": 0.1,
            }
            res = await client.get(f"{settings.world_service_url}/api/v2/worlds/{world_id}/memories", params=params)
            payload = res.json()
            items = payload.get("data", []) if isinstance(payload, dict) else []
            memories: List[Dict[str, str]] = []
            for item in items:
                if not isinstance(item, dict):
                    continue
                summary = str(item.get("summary") or "").strip()
                if not summary:
                    continue
                source_event_id = str(item.get("sourceEventId") or "")
                salience = float(item.get("salience") or 0.0)
                memories.append(
                    {
                        "type": "WORLD_MEMORY",
                        "feed_id": "",
                        "author_id": str(item.get("ownerId") or user_id),
                        "actor_id": str(item.get("ownerId") or user_id),
                        "summary": summary,
                        "source_event_id": source_event_id,
                        "salience": str(round(salience, 4)),
                    }
                )
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

    def _guard_turns(
        self,
        parsed_turns: List[Dict[str, Any]],
        persona_summary: Optional[str],
        recent_turns_by_role: Dict[str, List[str]],
        convo: List[Dict[str, str]],
        round_number: int,
        role_map: Dict[str, str],
        allowed_roles: List[str],
        round_trace: Dict[str, Any],
    ) -> Any:
        if not parsed_turns:
            return parsed_turns, []

        decisions: List[Dict[str, Any]] = []
        drifting_roles: List[str] = []
        scored: List[Dict[str, Any]] = []
        for turn in parsed_turns:
            role = str(turn.get("role") or "")
            content = str(turn.get("content") or "")
            score = self.drift_guard.score(
                role=role,
                content=content,
                persona_summary=persona_summary or "",
                recent_turns=recent_turns_by_role.get(role, []),
            )
            is_drift = self.drift_guard.is_drift(score)
            scored.append({"turn": turn, "score": score, "is_drift": is_drift})
            if is_drift:
                drifting_roles.append(role)
            decisions.append(
                {
                    "round": round_number,
                    "role": role,
                    "score": score,
                    "action": "detected" if is_drift else "accepted",
                }
            )

        if not drifting_roles:
            round_trace["drift_decisions"].extend(decisions)
            return parsed_turns, decisions

        regen_prompt = self._build_regeneration_prompt(parsed_turns, drifting_roles)
        regen_result = run_completion(convo + [{"role": "system", "content": regen_prompt}], tools=None, tool_choice="none")
        regen_choice = regen_result["choices"][0]["message"] if regen_result.get("choices") else {}
        regen_content = regen_choice.get("content") or ""
        regen_turns = self._parse_turns(regen_content, round_number, role_map, allowed_roles)
        regen_by_role = {str(turn.get("role")): turn for turn in regen_turns if isinstance(turn, dict)}

        guarded_turns: List[Dict[str, Any]] = []
        deweighted_roles: List[str] = []
        for row in scored:
            turn = row["turn"]
            role = str(turn.get("role") or "")
            if not row["is_drift"]:
                guarded_turns.append(turn)
                continue

            regenerated = regen_by_role.get(role)
            if regenerated:
                regen_score = self.drift_guard.score(
                    role=role,
                    content=str(regenerated.get("content") or ""),
                    persona_summary=persona_summary or "",
                    recent_turns=recent_turns_by_role.get(role, []),
                )
                decisions.append(
                    {
                        "round": round_number,
                        "role": role,
                        "score_before": row["score"],
                        "score_after": regen_score,
                        "action": "regenerate_once",
                    }
                )
                if not self.drift_guard.is_drift(regen_score):
                    guarded_turns.append(regenerated)
                    continue
            deweighted_roles.append(role)
            decisions.append(
                {
                    "round": round_number,
                    "role": role,
                    "score": row["score"],
                    "action": "deweighted",
                }
            )

        if deweighted_roles:
            deweight_set = set(deweighted_roles)
            for speaker in round_trace.get("speakers", []):
                role = str(speaker.get("role") or "")
                if role in deweight_set:
                    speaker["weight"] = round(float(speaker.get("weight", 0.0)) * 0.6, 4)

        round_trace["drift_decisions"].extend(decisions)
        return guarded_turns, decisions

    def _build_regeneration_prompt(self, parsed_turns: List[Dict[str, Any]], drifting_roles: List[str]) -> str:
        roles = ", ".join(sorted(set(drifting_roles)))
        lines = []
        for turn in parsed_turns:
            role = str(turn.get("role") or "")
            content = str(turn.get("content") or "")
            lines.append(f"[{role}] {content}")
        return (
            "Rewrite only the drifting role lines to match role style and persona.\n"
            "Return each line as: [role] message\n"
            f"Drifting roles: {roles}\n"
            "Original lines:\n"
            + "\n".join(lines)
        )

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

    def _build_state_effects(
        self,
        turns: List[Dict[str, Any]],
        tool_calls: List[Dict[str, Any]],
        conversation_id: Optional[str],
        world_id: Optional[str],
    ) -> List[Dict[str, Any]]:
        effects: List[Dict[str, Any]] = []
        for turn in turns:
            effects.append(
                {
                    "effect_type": "CHAT_MESSAGE",
                    "conversation_id": conversation_id,
                    "world_id": world_id,
                    "round": turn.get("round"),
                    "role": turn.get("role"),
                    "user_id": turn.get("user_id"),
                    "content": turn.get("content"),
                }
            )
        for tool in tool_calls:
            effects.append(
                {
                    "effect_type": "TOOL_CALL",
                    "world_id": world_id,
                    "tool_name": tool.get("name"),
                    "arguments": tool.get("arguments"),
                }
            )
        return effects

    def _score_branch(self, objective: str, actors: List[str], branch_index: int) -> float:
        seed = f"{objective}|{'/'.join(actors)}|{branch_index}".encode("utf-8")
        digest = hashlib.sha256(seed).hexdigest()
        value = int(digest[:8], 16) / float(0xFFFFFFFF)
        return round(max(0.01, min(value, 0.99)), 4)
