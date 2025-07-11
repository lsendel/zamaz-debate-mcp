from typing import List, Dict, Any, Optional, Literal
from pydantic import BaseModel, Field
from datetime import datetime
from enum import Enum


class LLMProvider(str, Enum):
    CLAUDE = "claude"
    OPENAI = "openai"
    GEMINI = "gemini"
    LLAMA = "llama"
    GROK = "grok"
    QWEN = "qwen"
    DEEPSEEK = "deepseek"


class Message(BaseModel):
    role: Literal["system", "user", "assistant"]
    content: str
    name: Optional[str] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)


class CompletionRequest(BaseModel):
    provider: LLMProvider
    model: str
    messages: List[Message]
    max_tokens: Optional[int] = 1000
    temperature: Optional[float] = 0.7
    top_p: Optional[float] = 1.0
    stop_sequences: Optional[List[str]] = None
    stream: bool = False
    metadata: Dict[str, Any] = Field(default_factory=dict)


class CompletionResponse(BaseModel):
    provider: LLMProvider
    model: str
    content: str
    usage: Dict[str, int] = Field(default_factory=dict)
    finish_reason: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
    metadata: Dict[str, Any] = Field(default_factory=dict)


class ModelInfo(BaseModel):
    name: str
    provider: LLMProvider
    context_window: int
    max_output_tokens: int
    input_cost_per_1k: Optional[float] = None
    output_cost_per_1k: Optional[float] = None
    supports_streaming: bool = True
    supports_functions: bool = False
    capabilities: List[str] = Field(default_factory=list)


class ProviderConfig(BaseModel):
    provider: LLMProvider
    api_key: Optional[str] = None
    endpoint: Optional[str] = None
    organization: Optional[str] = None
    project: Optional[str] = None
    timeout: int = 30
    max_retries: int = 3
    models: List[ModelInfo] = Field(default_factory=list)


class ErrorResponse(BaseModel):
    error: str
    error_type: str
    provider: Optional[LLMProvider] = None
    details: Dict[str, Any] = Field(default_factory=dict)
    timestamp: datetime = Field(default_factory=datetime.utcnow)