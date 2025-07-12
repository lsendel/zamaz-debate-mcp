import os
from typing import List, Optional, Dict, Any
from datetime import datetime
import structlog
import asyncio
import time
from functools import wraps

from ..models import (
    Debate, Turn, Participant, DebateStatus, TurnType,
    CreateDebateRequest, AddTurnRequest, GetNextTurnRequest,
    SummarizeDebateRequest, DebateSummary, DebateRules
)
from ..clients.mcp_client import ContextServiceClient, LLMServiceClient, RAGServiceClient
from ..db.debate_store import DebateStore
from ..concurrency import (
    with_debate_lock, with_request_queue, with_rate_limit,
    concurrency_metrics, debate_lock_manager
)
from ..websocket_manager import (
    notify_debate_started, notify_turn_added, notify_debate_completed
)

logger = structlog.get_logger()


class DebateOrchestrator:
    """Orchestrates debate flow and manages interactions with other services"""
    
    def __init__(self):
        # Initialize service clients
        self.context_client = ContextServiceClient(
            os.getenv("CONTEXT_SERVICE_URL", "http://localhost:5001")
        )
        self.llm_client = LLMServiceClient(
            os.getenv("LLM_SERVICE_URL", "http://localhost:5002")
        )
        self.rag_client = RAGServiceClient(
            os.getenv("RAG_SERVICE_URL", "http://localhost:5004")
        )
        
        # Initialize local storage
        self.debate_store = DebateStore()
        
    def track_metrics(self, operation_name: str):
        """Decorator to track operation metrics"""
        def decorator(func):
            @wraps(func)
            async def wrapper(*args, **kwargs):
                start_time = time.time()
                success = True
                try:
                    result = await func(*args, **kwargs)
                    return result
                except Exception as e:
                    success = False
                    raise
                finally:
                    response_time = time.time() - start_time
                    await concurrency_metrics.record_request(success, response_time)
                    logger.info(
                        f"{operation_name} completed",
                        success=success,
                        response_time_ms=response_time * 1000
                    )
            return wrapper
        return decorator
        
    @with_rate_limit
    @with_request_queue("create_debate")
    async def create_debate(self, request: CreateDebateRequest) -> Debate:
        """Create a new debate"""
        logger.info("Creating debate", name=request.name, topic=request.topic)
        
        # Create participants
        participants = []
        for p_data in request.participants:
            participant = Participant(**p_data)
            participants.append(participant)
        
        # Create rules
        rules = DebateRules(**(request.rules or {}))
        
        # Create debate object
        debate = Debate(
            org_id=request.org_id,
            name=request.name,
            topic=request.topic,
            description=request.description,
            participants=participants,
            rules=rules,
            metadata=request.metadata
        )
        
        # Create context namespace for this debate
        try:
            context_result = await self.context_client.create_context(
                org_id=request.org_id,
                namespace_id="debates",
                name=f"Debate: {request.name}",
                initial_messages=[
                    {
                        "role": "system",
                        "content": f"This is a debate about: {request.topic}\n\nFormat: {rules.format}\n\nParticipants: {', '.join(p.name for p in participants)}"
                    }
                ]
            )
            debate.context_id = context_result.get("id")
        except Exception as e:
            logger.warning("Context service unavailable, debate will proceed without context tracking", error=str(e))
            # Continue without context - the debate can still function
        
        # Save debate
        await self.debate_store.save_debate(debate)
        
        logger.info("Debate created", debate_id=debate.id)
        return debate
    
    @with_request_queue("start_debate")
    async def start_debate(self, debate_id: str) -> Debate:
        """Start a debate"""
        debate = await self.debate_store.get_debate(debate_id)
        if not debate:
            raise ValueError(f"Debate {debate_id} not found")
        
        if debate.status != DebateStatus.DRAFT:
            raise ValueError(f"Debate must be in DRAFT status to start")
        
        debate.status = DebateStatus.ACTIVE
        debate.started_at = datetime.utcnow()
        debate.next_participant_id = debate.participants[0].id if debate.participants else None
        
        # Add opening message to context
        if debate.context_id:
            await self.context_client.append_to_context(
                debate.context_id,
                [{
                    "role": "system",
                    "content": f"The debate has started. First participant: {debate.participants[0].name}"
                }]
            )
        
        await self.debate_store.save_debate(debate)
        
        # Send WebSocket notification
        await notify_debate_started(debate.id, debate.org_id, debate.topic)
        
        return debate
    
    @with_request_queue("add_turn")
    async def add_turn(self, request: AddTurnRequest) -> Turn:
        """Add a turn to the debate"""
        # Get debate lock
        lock = await debate_lock_manager.get_lock(request.debate_id)
        
        async with lock:
            debate = await self.debate_store.get_debate(request.debate_id)
            if not debate:
                raise ValueError(f"Debate {request.debate_id} not found")
        
            if debate.status != DebateStatus.ACTIVE:
                raise ValueError("Debate is not active")
        
            # Determine participant
            participant_id = request.participant_id or debate.next_participant_id
            participant = debate.get_participant(participant_id)
            if not participant:
                raise ValueError(f"Participant {participant_id} not found")
        
            # Generate content if not provided
            content = request.content
            if not content:
                content = await self._generate_turn_content(
                    debate, participant, request.turn_type,
                    use_rag=request.use_rag, rag_query=request.rag_query
                )
        
            # Create turn
            turn = Turn(
                debate_id=debate.id,
                participant_id=participant.id,
                turn_number=debate.current_turn + 1,
                round_number=debate.current_round,
                turn_type=request.turn_type,
                content=content,
                context_used=debate.context_id
            )
        
            # Update debate state
            debate.current_turn += 1
            debate.next_participant_id = debate.get_next_participant().id
        
            # Check if round is complete
            if debate.current_turn % len(debate.participants) == 0:
                debate.current_round += 1
            
                # Check max rounds
                if debate.rules.max_rounds and debate.current_round > debate.rules.max_rounds:
                    debate.status = DebateStatus.COMPLETED
                    debate.completed_at = datetime.utcnow()
        
            # Save turn and update debate
            await self.debate_store.save_turn(turn)
            await self.debate_store.save_debate(debate)
        
            # Update context
            if debate.context_id:
                await self.context_client.append_to_context(
                    debate.context_id,
                    [{
                        "role": "assistant",
                        "content": f"[{participant.name}]: {content}"
                    }]
                )
        
            # Send WebSocket notification for turn added
            await notify_turn_added(debate.id, debate.org_id, participant.name, debate.current_turn)
        
            # If debate is completed, send completion notification
            if debate.status == DebateStatus.COMPLETED:
                await notify_debate_completed(debate.id, debate.org_id, debate.topic)
        
            logger.info("Turn added", debate_id=debate.id, turn_id=turn.id)
            return turn
    
    @with_debate_lock
    @with_request_queue("get_next_turn")
    async def get_next_turn(self, request: GetNextTurnRequest) -> Turn:
        """Orchestrate the next turn automatically"""
        debate = await self.debate_store.get_debate(request.debate_id)
        if not debate:
            raise ValueError(f"Debate {request.debate_id} not found")
        
        # Determine turn type based on debate progress
        turn_type = self._determine_turn_type(debate)
        
        # Create turn request
        turn_request = AddTurnRequest(
            debate_id=request.debate_id,
            turn_type=turn_type,
            use_rag=request.include_rag,
            rag_query=None  # Will be determined based on context
        )
        
        return await self.add_turn(turn_request)
    
    async def summarize_debate(self, request: SummarizeDebateRequest) -> DebateSummary:
        """Generate a summary of the debate"""
        debate = await self.debate_store.get_debate(request.debate_id)
        if not debate:
            raise ValueError(f"Debate {request.debate_id} not found")
        
        # Get all turns
        turns = await self.debate_store.get_turns(request.debate_id)
        
        # Prepare summary prompt
        summary_prompt = self._build_summary_prompt(debate, turns, request)
        
        # Get context window
        context_window = None
        if debate.context_id:
            context_window = await self.context_client.get_context_window(
                debate.context_id,
                max_tokens=4000,
                strategy="sliding_window_with_summary"
            )
        
        # Generate summary using LLM
        messages = [
            {"role": "system", "content": "You are a debate summarizer. Provide clear, balanced summaries."},
            {"role": "user", "content": summary_prompt}
        ]
        
        if context_window and context_window.get("messages"):
            # Add relevant context
            messages.extend(context_window["messages"][-10:])  # Last 10 messages
        
        response = await self.llm_client.complete(
            provider="claude",  # Use Claude for summaries
            model="claude-3-haiku-20240307",  # Fast model for summaries
            messages=messages,
            max_tokens=1500,
            temperature=0.3  # Lower temperature for factual summary
        )
        
        # Parse summary response
        summary_content = response.get("content", "")
        
        # Create summary object
        summary = DebateSummary(
            debate_id=debate.id,
            summary=summary_content,
            key_points=self._extract_key_points(summary_content),
            participant_positions=self._extract_positions(debate, turns),
            consensus_points=[] if not request.include_consensus else self._extract_consensus(summary_content),
            disagreement_points=[] if not request.include_disagreements else self._extract_disagreements(summary_content)
        )
        
        # Save summary
        await self.debate_store.save_summary(summary)
        
        return summary
    
    async def _generate_turn_content(self, debate: Debate, participant: Participant,
                                   turn_type: TurnType, use_rag: bool = False,
                                   rag_query: Optional[str] = None) -> str:
        """Generate content for a turn using LLM"""
        
        # Get context window
        context_messages = []
        if debate.context_id:
            try:
                context_window = await self.context_client.get_context_window(
                    debate.context_id,
                    max_tokens=participant.llm_config.max_tokens * 2,  # Leave room for response
                    strategy="sliding_window_with_summary"
                )
                if context_window and "messages" in context_window:
                    context_messages = context_window["messages"]
            except Exception as e:
                logger.warning("Context service unavailable, proceeding without context", error=str(e))
                # Continue without context - the debate can still function
        
        # Build prompt
        system_prompt = self._build_participant_prompt(debate, participant, turn_type)
        
        messages = [{"role": "system", "content": system_prompt}]
        messages.extend(context_messages)
        
        # Add RAG context if requested
        if use_rag and rag_query:
            try:
                rag_results = await self.rag_client.search(
                    kb_id=debate.metadata.get("knowledge_base_id", "default"),
                    query=rag_query,
                    max_results=3
                )
                if rag_results:
                    rag_context = "\n\n".join([r.get("content", "") for r in rag_results])
                    messages.append({
                        "role": "system",
                        "content": f"Relevant information:\n{rag_context}"
                    })
            except Exception as e:
                logger.warning("RAG search failed", error=str(e))
        
        # Add turn instruction
        messages.append({
            "role": "user",
            "content": f"Provide your {turn_type.value} for this debate. Be clear, concise, and stay on topic."
        })
        
        # Generate response
        response = await self.llm_client.complete(
            provider=participant.llm_config.provider,
            model=participant.llm_config.model,
            messages=messages,
            max_tokens=participant.llm_config.max_tokens,
            temperature=participant.llm_config.temperature
        )
        
        return response.get("content", "")
    
    def _build_participant_prompt(self, debate: Debate, participant: Participant,
                                turn_type: TurnType) -> str:
        """Build system prompt for participant"""
        prompt_parts = []
        
        # Base prompt from participant config
        if participant.llm_config.system_prompt:
            prompt_parts.append(participant.llm_config.system_prompt)
        
        # Debate context
        prompt_parts.append(f"You are participating in a debate about: {debate.topic}")
        prompt_parts.append(f"Your name is: {participant.name}")
        
        if participant.position:
            prompt_parts.append(f"Your position: {participant.position}")
        
        # Role-specific instructions
        if participant.role.value == "moderator":
            prompt_parts.append("As the moderator, ensure fair participation and keep the debate on track.")
        elif participant.role.value == "judge":
            prompt_parts.append("As a judge, evaluate arguments objectively and provide balanced feedback.")
        
        # Turn type instructions
        turn_instructions = {
            TurnType.OPENING: "Provide a clear opening statement outlining your position.",
            TurnType.ARGUMENT: "Present a well-reasoned argument supporting your position.",
            TurnType.REBUTTAL: "Address and counter the opposing arguments presented.",
            TurnType.QUESTION: "Ask a thoughtful question to advance the debate.",
            TurnType.ANSWER: "Provide a direct and comprehensive answer to the question.",
            TurnType.CLOSING: "Summarize your key points and provide a strong closing statement."
        }
        
        if turn_type in turn_instructions:
            prompt_parts.append(turn_instructions[turn_type])
        
        # Format rules
        prompt_parts.append(f"Format: {debate.rules.format.value}")
        
        if debate.rules.min_turn_length:
            prompt_parts.append(f"Minimum response length: {debate.rules.min_turn_length} characters")
        if debate.rules.max_turn_length:
            prompt_parts.append(f"Maximum response length: {debate.rules.max_turn_length} characters")
        
        return "\n\n".join(prompt_parts)
    
    def _determine_turn_type(self, debate: Debate) -> TurnType:
        """Determine appropriate turn type based on debate progress"""
        total_turns = debate.current_turn
        participant_count = len(debate.participants)
        
        # Opening statements for first round
        if total_turns < participant_count:
            return TurnType.OPENING
        
        # Closing statements if nearing end
        if debate.rules.max_rounds:
            if debate.current_round == debate.rules.max_rounds:
                if total_turns >= (debate.rules.max_rounds - 1) * participant_count:
                    return TurnType.CLOSING
        
        # Otherwise alternate between arguments and rebuttals
        if debate.current_round % 2 == 0:
            return TurnType.REBUTTAL
        else:
            return TurnType.ARGUMENT
    
    def _build_summary_prompt(self, debate: Debate, turns: List[Turn],
                            request: SummarizeDebateRequest) -> str:
        """Build prompt for generating debate summary"""
        prompt_parts = [
            f"Summarize the following debate about: {debate.topic}",
            f"Debate format: {debate.rules.format.value}",
            f"Number of participants: {len(debate.participants)}",
            f"Total turns: {len(turns)}",
            "",
            "Participants:"
        ]
        
        for p in debate.participants:
            prompt_parts.append(f"- {p.name}: {p.position or 'No stated position'}")
        
        prompt_parts.extend([
            "",
            f"Summary style: {request.summary_style}",
            ""
        ])
        
        if request.include_consensus:
            prompt_parts.append("Identify points of consensus between participants.")
        
        if request.include_disagreements:
            prompt_parts.append("Identify key points of disagreement.")
        
        prompt_parts.append("\nProvide a balanced and objective summary.")
        
        return "\n".join(prompt_parts)
    
    def _extract_key_points(self, summary: str) -> List[str]:
        """Extract key points from summary"""
        # Simple extraction - in production would use more sophisticated parsing
        lines = summary.split("\n")
        key_points = []
        
        for line in lines:
            line = line.strip()
            if line.startswith("- ") or line.startswith("• "):
                key_points.append(line[2:])
        
        return key_points[:10]  # Limit to 10 key points
    
    def _extract_positions(self, debate: Debate, turns: List[Turn]) -> Dict[str, str]:
        """Extract participant positions from turns"""
        positions = {}
        
        for participant in debate.participants:
            # Find their opening or first substantive turn
            participant_turns = [t for t in turns if t.participant_id == participant.id]
            if participant_turns:
                # Use opening statement or first argument
                opening = next(
                    (t for t in participant_turns if t.turn_type == TurnType.OPENING),
                    participant_turns[0]
                )
                # Truncate to first 200 characters
                positions[participant.id] = opening.content[:200] + "..."
            else:
                positions[participant.id] = participant.position or "No position stated"
        
        return positions
    
    def _extract_consensus(self, summary: str) -> List[str]:
        """Extract consensus points from summary"""
        # Simple keyword-based extraction
        consensus_keywords = ["agreed", "consensus", "both parties", "all participants", "common ground"]
        consensus_points = []
        
        lines = summary.split("\n")
        for line in lines:
            if any(keyword in line.lower() for keyword in consensus_keywords):
                consensus_points.append(line.strip())
        
        return consensus_points[:5]
    
    def _extract_disagreements(self, summary: str) -> List[str]:
        """Extract disagreement points from summary"""
        # Simple keyword-based extraction  
        disagreement_keywords = ["disagreed", "contested", "disputed", "conflicting", "opposing views"]
        disagreement_points = []
        
        lines = summary.split("\n")
        for line in lines:
            if any(keyword in line.lower() for keyword in disagreement_keywords):
                disagreement_points.append(line.strip())
        
        return disagreement_points[:5]
    
    async def list_debates(self, org_id: str) -> List[dict]:
        """List all debates for an organization"""
        debates = await self.debate_store.list_debates(org_id)
        return [debate.dict() for debate in debates]