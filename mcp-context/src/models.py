from typing import List, Dict, Any, Optional, Literal
from datetime import datetime
from pydantic import BaseModel, Field
from enum import Enum
import uuid


class AccessLevel(str, Enum):
    READ = "read"
    WRITE = "write"
    APPEND = "append"
    ADMIN = "admin"


class ContextStrategy(str, Enum):
    FULL = "full"
    SLIDING_WINDOW = "sliding_window"
    SLIDING_WINDOW_WITH_SUMMARY = "sliding_window_with_summary"
    SEMANTIC_SELECTION = "semantic_selection"
    HYBRID = "hybrid"


class Organization(BaseModel):
    id: str = Field(default_factory=lambda: f"org-{uuid.uuid4().hex[:12]}")
    name: str
    api_key: str
    settings: Dict[str, Any] = Field(default_factory=dict)
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    is_active: bool = True


class ContextNamespace(BaseModel):
    id: str = Field(default_factory=lambda: f"ns-{uuid.uuid4().hex[:12]}")
    org_id: str
    name: str
    description: Optional[str] = None
    settings: Dict[str, Any] = Field(default_factory=dict)
    created_at: datetime = Field(default_factory=datetime.utcnow)


class Message(BaseModel):
    id: str = Field(default_factory=lambda: f"msg-{uuid.uuid4().hex[:12]}")
    role: Literal["system", "user", "assistant", "function"]
    content: str
    name: Optional[str] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)
    created_at: datetime = Field(default_factory=datetime.utcnow)


class Context(BaseModel):
    id: str = Field(default_factory=lambda: f"ctx-{uuid.uuid4().hex[:12]}")
    org_id: str
    namespace_id: str
    name: str
    description: Optional[str] = None
    messages: List[Message] = Field(default_factory=list)
    metadata: Dict[str, Any] = Field(default_factory=dict)
    version: int = 1
    parent_version: Optional[int] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    token_count: int = 0
    is_active: bool = True


class ContextShare(BaseModel):
    id: str = Field(default_factory=lambda: f"share-{uuid.uuid4().hex[:12]}")
    context_id: str
    source_org_id: str
    target_org_id: str
    access_level: AccessLevel = AccessLevel.READ
    expires_at: Optional[datetime] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
    created_by: str  # User who created the share
    is_active: bool = True


class ContextWindow(BaseModel):
    context_id: str
    messages: List[Message]
    total_messages: int
    included_messages: int
    token_count: int
    strategy_used: ContextStrategy
    summary: Optional[str] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)


class ContextSummary(BaseModel):
    context_id: str
    summary: str
    message_range: tuple[int, int]  # (start_idx, end_idx)
    created_at: datetime = Field(default_factory=datetime.utcnow)
    token_count: int


class AuditLog(BaseModel):
    id: str = Field(default_factory=lambda: f"audit-{uuid.uuid4().hex[:12]}")
    org_id: str
    user_id: Optional[str] = None
    action: str
    resource_type: str
    resource_id: str
    details: Dict[str, Any] = Field(default_factory=dict)
    ip_address: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)


class CreateContextRequest(BaseModel):
    org_id: str
    namespace_id: str
    name: str
    description: Optional[str] = None
    initial_messages: List[Message] = Field(default_factory=list)
    metadata: Dict[str, Any] = Field(default_factory=dict)


class AppendMessagesRequest(BaseModel):
    context_id: str
    messages: List[Message]
    update_version: bool = True


class GetContextWindowRequest(BaseModel):
    context_id: str
    max_tokens: int = 8000
    strategy: ContextStrategy = ContextStrategy.SLIDING_WINDOW
    include_summary: bool = True


class ShareContextRequest(BaseModel):
    context_id: str
    target_org_id: str
    access_level: AccessLevel = AccessLevel.READ
    expires_in_hours: Optional[int] = None


class SearchContextsRequest(BaseModel):
    org_id: str
    namespace_id: Optional[str] = None
    query: Optional[str] = None
    limit: int = 20
    offset: int = 0
    include_shared: bool = True