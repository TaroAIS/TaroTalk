from typing import List, Dict, Any


def pick_round_speakers(bindings: List[Dict[str, Any]], round_index: int) -> List[Dict[str, Any]]:
    if not bindings:
        return []

    self_binding = next((item for item in bindings if item.get("role") == "self-agent"), None)
    if round_index == 0 and self_binding is not None:
        return [self_binding]

    candidates = [item for item in bindings if item.get("role") != "self-agent"]
    if not candidates:
        return [self_binding] if self_binding is not None else [bindings[0]]

    candidates.sort(key=lambda item: (-float(item.get("weight", 0.0)), int(item.get("order", 0))))
    max_per_round = 2 if len(bindings) >= 3 else 1
    start = (round_index - 1) * max_per_round
    speakers = []
    for offset in range(max_per_round):
        index = (start + offset) % len(candidates)
        speakers.append(candidates[index])
    return speakers


def role_style(role: str) -> str:
    base_role = role
    if role.startswith("friend"):
        base_role = "friend"
    elif role.startswith("mentor"):
        base_role = "mentor"
    elif role.startswith("rival"):
        base_role = "rival"
    elif role.startswith("advertiser"):
        base_role = "advertiser"

    if base_role == "self-agent":
        return "自省、稳重、简洁"
    if base_role == "friend":
        return "亲和、情绪化、鼓励"
    if base_role == "mentor":
        return "理性、指导、条理"
    if base_role == "rival":
        return "轻微对立、挑衅但不过分"
    if base_role == "advertiser":
        return "商业、精准、带有推广意图"
    return "中性"
