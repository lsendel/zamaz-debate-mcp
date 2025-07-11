import os
import json
from typing import List, Optional, Dict, Any
from pathlib import Path
import aiosqlite
import structlog
from datetime import datetime

from ..models import Debate, Turn, DebateSummary

logger = structlog.get_logger()


class DebateStore:
    """Simple SQLite-based storage for debates"""
    
    def __init__(self):
        db_path = os.getenv("DATABASE_PATH", "/app/data/debates.db")
        self.db_path = Path(db_path)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        
    async def initialize(self):
        """Initialize database schema"""
        async with aiosqlite.connect(self.db_path) as db:
            # Create debates table
            await db.execute('''
                CREATE TABLE IF NOT EXISTS debates (
                    id TEXT PRIMARY KEY,
                    org_id TEXT NOT NULL,
                    data JSON NOT NULL,
                    status TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            ''')
            
            # Create turns table
            await db.execute('''
                CREATE TABLE IF NOT EXISTS turns (
                    id TEXT PRIMARY KEY,
                    debate_id TEXT NOT NULL,
                    participant_id TEXT NOT NULL,
                    turn_number INTEGER NOT NULL,
                    data JSON NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (debate_id) REFERENCES debates (id)
                )
            ''')
            
            # Create summaries table
            await db.execute('''
                CREATE TABLE IF NOT EXISTS summaries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    debate_id TEXT NOT NULL,
                    data JSON NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (debate_id) REFERENCES debates (id)
                )
            ''')
            
            # Create indices
            await db.execute('CREATE INDEX IF NOT EXISTS idx_debates_org ON debates(org_id)')
            await db.execute('CREATE INDEX IF NOT EXISTS idx_debates_status ON debates(status)')
            await db.execute('CREATE INDEX IF NOT EXISTS idx_turns_debate ON turns(debate_id)')
            
            await db.commit()
            
        logger.info("Database initialized", path=str(self.db_path))
    
    async def save_debate(self, debate: Debate) -> None:
        """Save or update a debate"""
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute('''
                INSERT OR REPLACE INTO debates (id, org_id, data, status, updated_at)
                VALUES (?, ?, ?, ?, ?)
            ''', (
                debate.id,
                debate.org_id,
                debate.json(),
                debate.status.value,
                datetime.utcnow().isoformat()
            ))
            await db.commit()
            
        logger.info("Debate saved", debate_id=debate.id)
    
    async def get_debate(self, debate_id: str) -> Optional[Debate]:
        """Get a debate by ID"""
        async with aiosqlite.connect(self.db_path) as db:
            async with db.execute(
                'SELECT data FROM debates WHERE id = ?',
                (debate_id,)
            ) as cursor:
                row = await cursor.fetchone()
                if row:
                    return Debate.parse_raw(row[0])
                return None
    
    async def list_debates(self, org_id: Optional[str] = None, 
                          status: Optional[str] = None) -> List[Debate]:
        """List debates with optional filters"""
        query = 'SELECT data FROM debates WHERE 1=1'
        params = []
        
        if org_id:
            query += ' AND org_id = ?'
            params.append(org_id)
        
        if status:
            query += ' AND status = ?'
            params.append(status)
        
        query += ' ORDER BY updated_at DESC'
        
        debates = []
        async with aiosqlite.connect(self.db_path) as db:
            async with db.execute(query, params) as cursor:
                async for row in cursor:
                    debates.append(Debate.parse_raw(row[0]))
        
        return debates
    
    async def save_turn(self, turn: Turn) -> None:
        """Save a turn"""
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute('''
                INSERT INTO turns (id, debate_id, participant_id, turn_number, data)
                VALUES (?, ?, ?, ?, ?)
            ''', (
                turn.id,
                turn.debate_id,
                turn.participant_id,
                turn.turn_number,
                turn.json()
            ))
            await db.commit()
            
        logger.info("Turn saved", turn_id=turn.id, debate_id=turn.debate_id)
    
    async def get_turns(self, debate_id: str) -> List[Turn]:
        """Get all turns for a debate"""
        turns = []
        async with aiosqlite.connect(self.db_path) as db:
            async with db.execute(
                'SELECT data FROM turns WHERE debate_id = ? ORDER BY turn_number',
                (debate_id,)
            ) as cursor:
                async for row in cursor:
                    turns.append(Turn.parse_raw(row[0]))
        
        return turns
    
    async def save_summary(self, summary: DebateSummary) -> None:
        """Save a debate summary"""
        async with aiosqlite.connect(self.db_path) as db:
            await db.execute('''
                INSERT INTO summaries (debate_id, data)
                VALUES (?, ?)
            ''', (
                summary.debate_id,
                summary.json()
            ))
            await db.commit()
            
        logger.info("Summary saved", debate_id=summary.debate_id)
    
    async def get_latest_summary(self, debate_id: str) -> Optional[DebateSummary]:
        """Get the latest summary for a debate"""
        async with aiosqlite.connect(self.db_path) as db:
            async with db.execute(
                '''SELECT data FROM summaries 
                   WHERE debate_id = ? 
                   ORDER BY created_at DESC LIMIT 1''',
                (debate_id,)
            ) as cursor:
                row = await cursor.fetchone()
                if row:
                    return DebateSummary.parse_raw(row[0])
                return None