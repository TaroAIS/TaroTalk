from typing import List, Dict, Any


def tool_registry() -> List[Dict[str, Any]]:
    tools = [
        {
            "name": "send_message",
            "description": "Send a message to a conversation",
            "parameters": {
                "type": "object",
                "properties": {
                    "conversation_id": {"type": "string"},
                    "sender_id": {"type": "string"},
                    "content": {"type": "string"}
                },
                "required": ["conversation_id", "sender_id", "content"]
            }
        },
        {
            "name": "post_feed",
            "description": "Post a feed item",
            "parameters": {
                "type": "object",
                "properties": {
                    "author_id": {"type": "string"},
                    "content": {"type": "string"}
                },
                "required": ["author_id", "content"]
            }
        },
        {
            "name": "like_feed",
            "description": "Toggle like state for a feed item",
            "parameters": {
                "type": "object",
                "properties": {
                    "feed_id": {"type": "string"},
                    "user_id": {"type": "string"},
                    "action": {
                        "type": "string",
                        "enum": ["LIKE", "UNLIKE"],
                        "description": "LIKE to like the feed, UNLIKE to cancel like"
                    }
                },
                "required": ["feed_id", "user_id"]
            }
        },
        {
            "name": "update_relationship",
            "description": "Update relationship score",
            "parameters": {
                "type": "object",
                "properties": {
                    "user_id": {"type": "string"},
                    "target_id": {"type": "string"},
                    "type": {"type": "string"},
                    "intimacy_score": {"type": "number"},
                    "interaction_count": {"type": "integer"},
                    "commercial_score": {"type": "number"}
                },
                "required": ["user_id", "target_id", "type"]
            }
        },
        {
            "name": "create_notification",
            "description": "Create a notification",
            "parameters": {
                "type": "object",
                "properties": {
                    "user_id": {"type": "string"},
                    "type": {"type": "string"},
                    "title": {"type": "string"},
                    "content": {"type": "string"}
                },
                "required": ["user_id", "type", "title", "content"]
            }
        },
        {
            "name": "get_persona",
            "description": "Fetch persona summary",
            "parameters": {
                "type": "object",
                "properties": {"user_id": {"type": "string"}},
                "required": ["user_id"]
            }
        },
        {
            "name": "get_contacts",
            "description": "Fetch AI contacts",
            "parameters": {
                "type": "object",
                "properties": {"user_id": {"type": "string"}},
                "required": ["user_id"]
            }
        }
    ]
    return [{"type": "function", "function": tool} for tool in tools]
