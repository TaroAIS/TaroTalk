from typing import List
import random

ROLES = ["self-agent", "friend", "mentor", "rival"]


def pick_round_speakers(participants: List[str], round_index: int) -> List[str]:
    is_group = len(participants) >= 3
    if round_index == 0:
        speakers = ["self-agent"]
    else:
        candidates = [role for role in ROLES if role != "self-agent"]
        speakers = [random.choice(candidates)]

    if is_group and len(speakers) < 2:
        remaining = [role for role in ROLES if role not in speakers]
        speakers.append(random.choice(remaining))
    return speakers


def role_style(role: str) -> str:
    if role == "self-agent":
        return "自省、稳重、简洁"
    if role == "friend":
        return "亲和、情绪化、鼓励"
    if role == "mentor":
        return "理性、指导、条理"
    if role == "rival":
        return "轻微对立、挑衅但不过分"
    return "中性"
