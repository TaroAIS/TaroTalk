from typing import Dict, List
import re


def _tokenize(text: str) -> List[str]:
    if not text:
        return []
    return [token for token in re.split(r"[^a-zA-Z0-9]+", text.lower()) if len(token) >= 2]


class DriftGuard:
    def __init__(self, threshold: float = 0.90):
        self.threshold = threshold
        self.role_keywords: Dict[str, List[str]] = {
            "self-agent": ["reflect", "steady", "calm", "balanced"],
            "friend": ["support", "care", "together", "share", "warm"],
            "mentor": ["advice", "plan", "step", "focus", "improve"],
            "rival": ["challenge", "compete", "prove", "push", "debate"],
            "advertiser": ["offer", "discount", "brand", "buy", "deal"],
        }
        self.cross_keywords: Dict[str, List[str]] = {
            "friend": ["discount", "offer", "buy"],
            "mentor": ["discount", "buy", "deal"],
            "rival": ["discount", "deal"],
        }

    def score(self, role: str, content: str, persona_summary: str = "", recent_turns: List[str] = None) -> float:
        tokens = set(_tokenize(content))
        if not tokens:
            return 1.0

        base_role = self._normalize_role(role)
        style_tokens = set(self.role_keywords.get(base_role, []))
        style_overlap = len(tokens.intersection(style_tokens)) / float(len(style_tokens) or 1)

        persona_tokens = set(_tokenize(persona_summary))
        persona_overlap = 0.5 if not persona_tokens else len(tokens.intersection(persona_tokens)) / float(len(tokens))

        recent_turns = recent_turns or []
        novelty = 1.0
        if recent_turns:
            recent_tokens = set()
            for row in recent_turns[-3:]:
                recent_tokens.update(_tokenize(row))
            if recent_tokens:
                novelty = 1.0 - min(1.0, len(tokens.intersection(recent_tokens)) / float(len(tokens)))

        cross_penalty = 0.0
        for keyword in self.cross_keywords.get(base_role, []):
            if keyword in tokens:
                cross_penalty += 0.1
        cross_penalty = min(cross_penalty, 0.4)

        stability = 0.55 * style_overlap + 0.25 * persona_overlap + 0.20 * novelty
        drift_score = 1.0 - stability + cross_penalty
        return max(0.0, min(1.0, drift_score))

    def is_drift(self, score: float) -> bool:
        return score >= self.threshold

    def _normalize_role(self, role: str) -> str:
        value = (role or "").strip().lower()
        if value.startswith("friend"):
            return "friend"
        if value.startswith("mentor"):
            return "mentor"
        if value.startswith("rival"):
            return "rival"
        if value.startswith("advertiser"):
            return "advertiser"
        if value.startswith("self-agent"):
            return "self-agent"
        return value
