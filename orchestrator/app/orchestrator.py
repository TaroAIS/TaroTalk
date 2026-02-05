from typing import List, Dict, Any
import json

from .llm_client import run_completion
from .tools import tool_registry
from .tool_executor import ToolExecutor
from .director import pick_round_speakers, role_style


class OrchestratorEngine:
    def __init__(self, max_steps: int = 3, max_tool_calls: int = 3):
        self.max_steps = max_steps
        self.max_tool_calls = max_tool_calls
        self.tools = tool_registry()
        self.executor = ToolExecutor()

    async def run_chat(
        self,
        messages: List[Dict[str, str]],
        persona_summary: str | None = None,
        participants: List[str] | None = None,
        rounds: int = 2,
    ) -> Dict[str, Any]:
        system_prompt = self._build_system_prompt(persona_summary, participants)
        convo = [{"role": "system", "content": system_prompt}] + messages
        tool_calls_collected: List[Dict[str, Any]] = []
        replies: List[str] = []

        for round_index in range(rounds):
            speakers = pick_round_speakers(participants or [], round_index)
            speaker_block = "\n".join([f"- {role}: {role_style(role)}" for role in speakers])
            round_prompt = (
                "You are the director. Generate messages for the following speakers.\n"
                "Return each line as: [role] message\n"
                f"Speakers:\n{speaker_block}\n"
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

    def _build_system_prompt(self, persona_summary: str | None, participants: List[str] | None) -> str:
        base = "You are a multi-agent chat orchestrator."
        if persona_summary:
            base += f" Persona summary: {persona_summary}."
        if participants:
            base += f" Participants: {', '.join(participants)}."
        base += " Use tools when needed and answer concisely."
        return base
