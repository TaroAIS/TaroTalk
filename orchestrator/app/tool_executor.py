from typing import Dict, Any, Optional
import httpx
from .config import settings


class ToolExecutor:
    def __init__(self):
        timeout = httpx.Timeout(
            timeout=settings.http_timeout_seconds,
            connect=settings.http_connect_timeout_seconds
        )
        self.client = httpx.AsyncClient(timeout=timeout)
        self.retry_attempts = max(1, int(settings.http_retry_attempts))

    async def aclose(self) -> None:
        await self.client.aclose()

    async def execute(self, name: str, arguments: Dict[str, Any]) -> Dict[str, Any]:
        if name == "send_message":
            return await self._send_message(arguments)
        if name == "post_feed":
            return await self._post_feed(arguments)
        if name == "like_feed":
            return await self._like_feed(arguments)
        if name == "update_relationship":
            return await self._update_relationship(arguments)
        if name == "get_persona":
            return await self._get_persona(arguments)
        if name == "get_contacts":
            return await self._get_contacts(arguments)
        if name == "create_notification":
            return await self._create_notification(arguments)
        return {"status": "unknown_tool"}

    async def _request(self, method: str, url: str, **kwargs: Any) -> Dict[str, Any]:
        last_error: Optional[Exception] = None
        for _ in range(self.retry_attempts):
            try:
                response = await self.client.request(method, url, **kwargs)
                response.raise_for_status()
                if response.content:
                    return response.json()
                return {"status": "ok"}
            except Exception as ex:
                last_error = ex
        return {"status": "error", "message": str(last_error) if last_error else "request failed"}

    async def _send_message(self, args: Dict[str, Any]) -> Dict[str, Any]:
        conversation_id = args["conversation_id"]
        payload = {
            "senderId": args["sender_id"],
            "content": args["content"],
            "type": "text",
        }
        return await self._request(
            "POST",
            f"{settings.chat_service_url}/api/conversations/{conversation_id}/messages",
            json=payload
        )

    async def _post_feed(self, args: Dict[str, Any]) -> Dict[str, Any]:
        payload = {
            "authorId": args["author_id"],
            "content": args["content"],
        }
        return await self._request("POST", f"{settings.feed_service_url}/api/feeds", json=payload)

    async def _like_feed(self, args: Dict[str, Any]) -> Dict[str, Any]:
        feed_id = args["feed_id"]
        payload = {
            "userId": args["user_id"],
            "action": str(args.get("action") or "LIKE").upper(),
        }
        return await self._request("POST", f"{settings.feed_service_url}/api/v2/feeds/{feed_id}/like", json=payload)

    async def _update_relationship(self, args: Dict[str, Any]) -> Dict[str, Any]:
        user_id = args["user_id"]
        payload = {
            "targetId": args["target_id"],
            "type": args["type"],
            "intimacyScore": args.get("intimacy_score", 0.0),
            "interactionCount": args.get("interaction_count", 1),
            "commercialScore": args.get("commercial_score", 0.0),
        }
        return await self._request("POST", f"{settings.relationship_service_url}/api/relationships/{user_id}", json=payload)

    async def _get_persona(self, args: Dict[str, Any]) -> Dict[str, Any]:
        user_id = args["user_id"]
        return await self._request("GET", f"{settings.persona_service_url}/api/personas/{user_id}")

    async def _get_contacts(self, args: Dict[str, Any]) -> Dict[str, Any]:
        user_id = args["user_id"]
        return await self._request("GET", f"{settings.contact_service_url}/api/contacts", params={"userId": user_id})

    async def _create_notification(self, args: Dict[str, Any]) -> Dict[str, Any]:
        payload = {
            "userId": args["user_id"],
            "type": args["type"],
            "title": args["title"],
            "content": args["content"],
        }
        return await self._request("POST", f"{settings.notification_service_url}/api/notifications", json=payload)
