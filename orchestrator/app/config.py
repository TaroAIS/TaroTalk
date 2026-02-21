import os
from typing import Optional

from pydantic import BaseModel


class Settings(BaseModel):
    service_port: int = int(os.getenv("ORCH_PORT", "8077"))
    user_service_url: str = os.getenv("USER_SERVICE_URL", "http://localhost:8082")
    contact_service_url: str = os.getenv("CONTACT_SERVICE_URL", "http://localhost:8082")
    relationship_service_url: str = os.getenv("RELATIONSHIP_SERVICE_URL", "http://localhost:8088")
    chat_service_url: str = os.getenv("CHAT_SERVICE_URL", "http://localhost:8084")
    feed_service_url: str = os.getenv("FEED_SERVICE_URL", "http://localhost:8085")
    persona_service_url: str = os.getenv("PERSONA_SERVICE_URL", "http://localhost:8083")
    notification_service_url: str = os.getenv("NOTIFICATION_SERVICE_URL", "http://localhost:8087")
    world_service_url: str = os.getenv("WORLD_SERVICE_URL", "http://localhost:8092")
    event_service_url: str = os.getenv("EVENT_SERVICE_URL", "http://localhost:8091")
    llm_provider: str = os.getenv("LLM_PROVIDER", "openai")
    llm_model: str = os.getenv("LLM_MODEL", "gpt-4o-mini")
    llm_api_base: Optional[str] = os.getenv("LLM_API_BASE")
    llm_api_key: Optional[str] = os.getenv("LLM_API_KEY")
    http_timeout_seconds: float = float(os.getenv("HTTP_TIMEOUT_SECONDS", "5"))
    http_connect_timeout_seconds: float = float(os.getenv("HTTP_CONNECT_TIMEOUT_SECONDS", "2"))
    http_retry_attempts: int = int(os.getenv("HTTP_RETRY_ATTEMPTS", "2"))


settings = Settings()
