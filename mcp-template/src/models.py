from typing import Optional, List, Dict, Any, Literal
from pydantic import BaseModel, Field
from datetime import datetime
from enum import Enum

class TemplateCategory(str, Enum):
    """Template categories for organization"""
    DEBATE = "debate"
    PROMPT = "prompt"
    RESPONSE = "response"
    EVALUATION = "evaluation"
    MODERATION = "moderation"
    SYSTEM = "system"
    CUSTOM = "custom"

class TemplateStatus(str, Enum):
    """Template status"""
    DRAFT = "draft"
    ACTIVE = "active"
    ARCHIVED = "archived"
    DEPRECATED = "deprecated"

class TemplateType(str, Enum):
    """Template types for rendering"""
    JINJA2 = "jinja2"
    MARKDOWN = "markdown"
    PLAIN_TEXT = "plain_text"
    JSON = "json"

class TemplateVariable(BaseModel):
    """Template variable definition"""
    name: str = Field(..., description="Variable name")
    type: Literal["string", "number", "boolean", "array", "object"] = Field(..., description="Variable type")
    description: str = Field(..., description="Variable description")
    required: bool = Field(default=True, description="Whether variable is required")
    default_value: Optional[Any] = Field(default=None, description="Default value")
    validation_pattern: Optional[str] = Field(default=None, description="Regex pattern for validation")
    choices: Optional[List[str]] = Field(default=None, description="Valid choices for this variable")

class Template(BaseModel):
    """Template model"""
    id: Optional[str] = Field(default=None, description="Template unique identifier")
    organization_id: str = Field(..., description="Organization ID")
    name: str = Field(..., description="Template name")
    description: Optional[str] = Field(default=None, description="Template description")
    category: TemplateCategory = Field(..., description="Template category")
    subcategory: Optional[str] = Field(default=None, description="Template subcategory")
    template_type: TemplateType = Field(default=TemplateType.JINJA2, description="Template type")
    content: str = Field(..., description="Template content")
    variables: List[TemplateVariable] = Field(default_factory=list, description="Template variables")
    tags: List[str] = Field(default_factory=list, description="Template tags")
    status: TemplateStatus = Field(default=TemplateStatus.DRAFT, description="Template status")
    version: int = Field(default=1, description="Template version")
    parent_template_id: Optional[str] = Field(default=None, description="Parent template for versioning")
    created_at: Optional[datetime] = Field(default=None, description="Creation timestamp")
    updated_at: Optional[datetime] = Field(default=None, description="Last update timestamp")
    created_by: Optional[str] = Field(default=None, description="Creator user ID")
    updated_by: Optional[str] = Field(default=None, description="Last updater user ID")
    usage_count: int = Field(default=0, description="How many times template has been used")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="Additional metadata")

class TemplateRenderRequest(BaseModel):
    """Template render request"""
    template_id: str = Field(..., description="Template ID to render")
    variables: Dict[str, Any] = Field(default_factory=dict, description="Variables for rendering")
    organization_id: str = Field(..., description="Organization ID")
    context: Optional[Dict[str, Any]] = Field(default=None, description="Additional context")

class TemplateRenderResponse(BaseModel):
    """Template render response"""
    rendered_content: str = Field(..., description="Rendered template content")
    template_id: str = Field(..., description="Template ID")
    variables_used: Dict[str, Any] = Field(..., description="Variables used in rendering")
    render_time_ms: float = Field(..., description="Rendering time in milliseconds")
    warnings: List[str] = Field(default_factory=list, description="Rendering warnings")

class TemplateCreateRequest(BaseModel):
    """Template creation request"""
    organization_id: str = Field(..., description="Organization ID")
    name: str = Field(..., description="Template name")
    description: Optional[str] = Field(default=None, description="Template description")
    category: TemplateCategory = Field(..., description="Template category")
    subcategory: Optional[str] = Field(default=None, description="Template subcategory")
    template_type: TemplateType = Field(default=TemplateType.JINJA2, description="Template type")
    content: str = Field(..., description="Template content")
    variables: List[TemplateVariable] = Field(default_factory=list, description="Template variables")
    tags: List[str] = Field(default_factory=list, description="Template tags")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="Additional metadata")

class TemplateUpdateRequest(BaseModel):
    """Template update request"""
    name: Optional[str] = Field(default=None, description="Template name")
    description: Optional[str] = Field(default=None, description="Template description")
    category: Optional[TemplateCategory] = Field(default=None, description="Template category")
    subcategory: Optional[str] = Field(default=None, description="Template subcategory")
    content: Optional[str] = Field(default=None, description="Template content")
    variables: Optional[List[TemplateVariable]] = Field(default=None, description="Template variables")
    tags: Optional[List[str]] = Field(default=None, description="Template tags")
    status: Optional[TemplateStatus] = Field(default=None, description="Template status")
    metadata: Optional[Dict[str, Any]] = Field(default=None, description="Additional metadata")

class TemplateSearchRequest(BaseModel):
    """Template search request"""
    organization_id: str = Field(..., description="Organization ID")
    query: Optional[str] = Field(default=None, description="Search query")
    category: Optional[TemplateCategory] = Field(default=None, description="Filter by category")
    subcategory: Optional[str] = Field(default=None, description="Filter by subcategory")
    tags: Optional[List[str]] = Field(default=None, description="Filter by tags")
    status: Optional[TemplateStatus] = Field(default=None, description="Filter by status")
    limit: int = Field(default=50, description="Result limit")
    offset: int = Field(default=0, description="Result offset")

class TemplateSearchResponse(BaseModel):
    """Template search response"""
    templates: List[Template] = Field(..., description="Found templates")
    total_count: int = Field(..., description="Total matching templates")
    has_more: bool = Field(..., description="Whether there are more results")

class TemplateUsageStats(BaseModel):
    """Template usage statistics"""
    template_id: str = Field(..., description="Template ID")
    usage_count: int = Field(..., description="Total usage count")
    last_used: Optional[datetime] = Field(default=None, description="Last usage time")
    users_count: int = Field(default=0, description="Number of different users")
    avg_render_time_ms: float = Field(default=0.0, description="Average render time")

class DebateTemplateRequest(BaseModel):
    """Debate-specific template request"""
    topic: str = Field(..., description="Debate topic")
    participants: List[str] = Field(..., description="Participant names")
    participant_positions: Dict[str, str] = Field(..., description="Participant positions")
    rules: Optional[List[str]] = Field(default=None, description="Debate rules")
    time_limit: Optional[int] = Field(default=None, description="Time limit per turn")
    rounds: Optional[int] = Field(default=None, description="Number of rounds")
    context: Optional[str] = Field(default=None, description="Additional context")
    organization_id: str = Field(..., description="Organization ID")

class DebateTemplateResponse(BaseModel):
    """Debate template response"""
    debate_prompt: str = Field(..., description="Generated debate prompt")
    participant_prompts: Dict[str, str] = Field(..., description="Individual participant prompts")
    moderator_prompt: str = Field(..., description="Moderator prompt")
    evaluation_criteria: List[str] = Field(..., description="Evaluation criteria")
    template_ids_used: List[str] = Field(..., description="Template IDs used in generation")

class TemplateErrorResponse(BaseModel):
    """Template error response"""
    error: str = Field(..., description="Error message")
    error_code: str = Field(..., description="Error code")
    details: Optional[Dict[str, Any]] = Field(default=None, description="Additional error details")
    template_id: Optional[str] = Field(default=None, description="Template ID if applicable")