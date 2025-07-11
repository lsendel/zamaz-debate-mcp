from typing import List, Dict, Any, Optional, Literal
from datetime import datetime
from pydantic import BaseModel, Field
from enum import Enum
import uuid


class DebateStatus(str, Enum):
    DRAFT = "draft"
    ACTIVE = "active"
    PAUSED = "paused"
    COMPLETED = "completed"
    ARCHIVED = "archived"


class DebateFormat(str, Enum):
    ROUND_ROBIN = "round_robin"          # Each participant takes turns
    FREE_FORM = "free_form"              # No strict turn order
    OXFORD = "oxford"                    # Formal Oxford-style debate
    PANEL = "panel"                      # Panel discussion format
    SOCRATIC = "socratic"                # Question-driven format
    ADVERSARIAL = "adversarial"          # Two-sided argument


class TurnType(str, Enum):
    OPENING = "opening"
    ARGUMENT = "argument"
    REBUTTAL = "rebuttal"
    QUESTION = "question"
    ANSWER = "answer"
    CLOSING = "closing"


class ParticipantRole(str, Enum):
    DEBATER = "debater"
    MODERATOR = "moderator"
    JUDGE = "judge"
    OBSERVER = "observer"


class LLMConfig(BaseModel):
    provider: str  # "claude", "openai", "gemini", "llama"
    model: str
    temperature: float = 0.7
    max_tokens: int = 1000
    system_prompt: Optional[str] = None
    additional_params: Dict[str, Any] = Field(default_factory=dict)


class Participant(BaseModel):
    id: str = Field(default_factory=lambda: f"part-{uuid.uuid4().hex[:12]}")
    name: str
    role: ParticipantRole = ParticipantRole.DEBATER
    llm_config: LLMConfig
    position: Optional[str] = None  # Their stance/position in debate
    metadata: Dict[str, Any] = Field(default_factory=dict)
    created_at: datetime = Field(default_factory=datetime.utcnow)


class DebateRules(BaseModel):
    format: DebateFormat = DebateFormat.ROUND_ROBIN
    max_rounds: Optional[int] = None
    max_turns_per_participant: Optional[int] = None
    turn_time_limit_seconds: Optional[int] = None
    min_turn_length: Optional[int] = 50  # characters
    max_turn_length: Optional[int] = 2000  # characters
    allowed_turn_types: List[TurnType] = Field(default_factory=lambda: list(TurnType))
    enforce_citations: bool = False
    allow_interruptions: bool = False
    custom_rules: Dict[str, Any] = Field(default_factory=dict)


class Turn(BaseModel):
    id: str = Field(default_factory=lambda: f"turn-{uuid.uuid4().hex[:12]}")
    debate_id: str
    participant_id: str
    turn_number: int
    round_number: int
    turn_type: TurnType = TurnType.ARGUMENT
    content: str
    context_used: Optional[str] = None  # Context ID from context service
    metadata: Dict[str, Any] = Field(default_factory=dict)
    created_at: datetime = Field(default_factory=datetime.utcnow)
    duration_seconds: Optional[int] = None
    token_count: Optional[int] = None


class Debate(BaseModel):
    id: str = Field(default_factory=lambda: f"debate-{uuid.uuid4().hex[:12]}")
    org_id: str
    name: str
    description: Optional[str] = None
    topic: str
    participants: List[Participant]
    rules: DebateRules
    status: DebateStatus = DebateStatus.DRAFT
    context_namespace: Optional[str] = None  # Namespace in context service
    context_id: Optional[str] = None  # Active context in context service
    current_round: int = 1
    current_turn: int = 0
    next_participant_id: Optional[str] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)
    created_at: datetime = Field(default_factory=datetime.utcnow)
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    implementation_tracking: Optional[Dict[str, Any]] = None
    
    def get_participant(self, participant_id: str) -> Optional[Participant]:
        return next((p for p in self.participants if p.id == participant_id), None)
    
    def get_next_participant(self) -> Optional[Participant]:
        if self.rules.format == DebateFormat.ROUND_ROBIN:
            # Simple round-robin logic
            if not self.next_participant_id:
                return self.participants[0] if self.participants else None
            
            current_idx = next(
                (i for i, p in enumerate(self.participants) if p.id == self.next_participant_id),
                -1
            )
            next_idx = (current_idx + 1) % len(self.participants)
            return self.participants[next_idx]
        
        # Other formats would have different logic
        return None


class DebateSummary(BaseModel):
    debate_id: str
    summary: str
    key_points: List[str]
    participant_positions: Dict[str, str]  # participant_id -> position summary
    consensus_points: List[str]
    disagreement_points: List[str]
    created_at: datetime = Field(default_factory=datetime.utcnow)


class CreateDebateRequest(BaseModel):
    org_id: str
    name: str
    topic: str
    description: Optional[str] = None
    participants: List[Dict[str, Any]]  # Will be converted to Participant objects
    rules: Optional[Dict[str, Any]] = None  # Will be converted to DebateRules
    metadata: Dict[str, Any] = Field(default_factory=dict)


class AddTurnRequest(BaseModel):
    debate_id: str
    participant_id: Optional[str] = None  # If None, use next in order
    turn_type: TurnType = TurnType.ARGUMENT
    content: Optional[str] = None  # If None, generate via LLM
    use_rag: bool = False
    rag_query: Optional[str] = None


class GetNextTurnRequest(BaseModel):
    debate_id: str
    include_rag: bool = False
    rag_knowledge_base: Optional[str] = None


class SummarizeDebateRequest(BaseModel):
    debate_id: str
    summary_style: Literal["concise", "detailed", "bullet_points"] = "concise"
    include_consensus: bool = True
    include_disagreements: bool = True