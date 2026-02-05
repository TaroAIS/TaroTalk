from pydantic import BaseModel
import os


class Settings(BaseModel):
    service_port: int = int(os.getenv("ORCH_PORT", "8077"))
    user_service_url: str = os.getenv("USER_SERVICE_URL", "http://localhost:8082")
    contact_service_url: str = os.getenv("CONTACT_SERVICE_URL", "http://localhost:8082")
    relationship_service_url: str = os.getenv("RELATIONSHIP_SERVICE_URL", "http://localhost:8088")
    chat_service_url: str = os.getenv("CHAT_SERVICE_URL", "http://localhost:8084")
    feed_service_url: str = os.getenv("FEED_SERVICE_URL", "http://localhost:8085")
    llm_provider: str = os.getenv("LLM_PROVIDER", "openai")
    llm_model: str = os.getenv("LLM_MODEL", "gpt-4o-mini")
    llm_api_base: str | None = os.getenv("LLM_API_BASE")
    llm_api_key: str | None = os.getenv("LLM_API_KEY")


settings = Settings()
