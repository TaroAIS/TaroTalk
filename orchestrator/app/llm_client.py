from typing import List, Dict, Any
from litellm import completion
from .config import settings


def run_completion(messages: List[Dict[str, str]], tools: List[Dict[str, Any]] | None = None,
                   tool_choice: str | None = None) -> Dict[str, Any]:
    payload = {
        "model": settings.llm_model,
        "messages": messages,
    }
    if tools:
        payload["tools"] = tools
        if tool_choice:
            payload["tool_choice"] = tool_choice

    if settings.llm_api_base:
        payload["api_base"] = settings.llm_api_base
    if settings.llm_api_key:
        payload["api_key"] = settings.llm_api_key

    return completion(**payload)
