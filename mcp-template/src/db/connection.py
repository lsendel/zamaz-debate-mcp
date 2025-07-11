import os
import asyncio
from typing import Optional
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import declarative_base
from sqlalchemy import (
    Column, String, Text, DateTime, Integer, Boolean, JSON, 
    ForeignKey, Index, UniqueConstraint
)
from datetime import datetime
from loguru import logger

# Database configuration
DATABASE_URL = (
    f"postgresql+asyncpg://"
    f"{os.getenv('POSTGRES_USER', 'context_user')}:"
    f"{os.getenv('POSTGRES_PASSWORD', 'context_pass')}@"
    f"{os.getenv('POSTGRES_HOST', 'localhost')}:"
    f"{os.getenv('POSTGRES_PORT', '5432')}/"
    f"{os.getenv('POSTGRES_DB', 'template_db')}"
)

# Create async engine
engine = create_async_engine(
    DATABASE_URL,
    echo=os.getenv('LOG_LEVEL', 'INFO') == 'DEBUG',
    pool_pre_ping=True,
    pool_recycle=300,
    pool_size=10,
    max_overflow=20
)

# Create session factory
AsyncSessionLocal = async_sessionmaker(
    engine, 
    class_=AsyncSession, 
    expire_on_commit=False
)

# Create base class for models
Base = declarative_base()

class TemplateTable(Base):
    """Template database table"""
    __tablename__ = "templates"
    
    id = Column(String, primary_key=True)
    organization_id = Column(String, nullable=False, index=True)
    name = Column(String, nullable=False)
    description = Column(Text)
    category = Column(String, nullable=False, index=True)
    subcategory = Column(String, index=True)
    template_type = Column(String, nullable=False, default="jinja2")
    content = Column(Text, nullable=False)
    variables = Column(JSON, default=list)
    tags = Column(JSON, default=list)
    status = Column(String, nullable=False, default="draft", index=True)
    version = Column(Integer, nullable=False, default=1)
    parent_template_id = Column(String, ForeignKey("templates.id"), nullable=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)
    created_by = Column(String)
    updated_by = Column(String)
    usage_count = Column(Integer, nullable=False, default=0)
    metadata = Column(JSON, default=dict)
    
    # Indexes
    __table_args__ = (
        Index('idx_org_category', 'organization_id', 'category'),
        Index('idx_org_status', 'organization_id', 'status'),
        Index('idx_org_name', 'organization_id', 'name'),
        Index('idx_created_at', 'created_at'),
        Index('idx_usage_count', 'usage_count'),
        UniqueConstraint('organization_id', 'name', 'version', name='uq_org_name_version'),
    )

class TemplateUsageTable(Base):
    """Template usage tracking table"""
    __tablename__ = "template_usage"
    
    id = Column(String, primary_key=True)
    template_id = Column(String, ForeignKey("templates.id"), nullable=False, index=True)
    organization_id = Column(String, nullable=False, index=True)
    user_id = Column(String, index=True)
    used_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    render_time_ms = Column(Integer)
    variables_used = Column(JSON, default=dict)
    context_data = Column(JSON, default=dict)
    success = Column(Boolean, nullable=False, default=True)
    error_message = Column(Text)
    
    # Indexes
    __table_args__ = (
        Index('idx_template_used_at', 'template_id', 'used_at'),
        Index('idx_org_used_at', 'organization_id', 'used_at'),
        Index('idx_user_used_at', 'user_id', 'used_at'),
    )

class TemplateCategoryTable(Base):
    """Template categories and subcategories table"""
    __tablename__ = "template_categories"
    
    id = Column(String, primary_key=True)
    organization_id = Column(String, nullable=False, index=True)
    category = Column(String, nullable=False)
    subcategory = Column(String)
    description = Column(Text)
    icon = Column(String)
    color = Column(String)
    sort_order = Column(Integer, default=0)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    created_by = Column(String)
    
    # Indexes
    __table_args__ = (
        Index('idx_org_category', 'organization_id', 'category'),
        UniqueConstraint('organization_id', 'category', 'subcategory', name='uq_org_cat_subcat'),
    )

class DatabaseManager:
    """Database connection and session manager"""
    
    def __init__(self):
        self.engine = engine
        self.session_factory = AsyncSessionLocal
    
    async def create_tables(self):
        """Create all database tables"""
        try:
            async with self.engine.begin() as conn:
                await conn.run_sync(Base.metadata.create_all)
            logger.info("Database tables created successfully")
        except Exception as e:
            logger.error(f"Failed to create database tables: {e}")
            raise
    
    async def get_session(self) -> AsyncSession:
        """Get database session"""
        return self.session_factory()
    
    async def close(self):
        """Close database connection"""
        await self.engine.dispose()
        logger.info("Database connection closed")
    
    async def health_check(self) -> bool:
        """Check database health"""
        try:
            async with self.get_session() as session:
                await session.execute("SELECT 1")
                return True
        except Exception as e:
            logger.error(f"Database health check failed: {e}")
            return False

# Global database manager instance
db_manager = DatabaseManager()

async def get_db_session() -> AsyncSession:
    """Dependency for getting database session"""
    async with db_manager.get_session() as session:
        try:
            yield session
        finally:
            await session.close()

async def init_database():
    """Initialize database with default data"""
    await db_manager.create_tables()
    
    # Create default categories
    async with db_manager.get_session() as session:
        try:
            from sqlalchemy import select
            
            # Check if any categories exist
            result = await session.execute(select(TemplateCategoryTable).limit(1))
            if result.first():
                logger.info("Database already initialized")
                return
            
            # Create default categories for system organization
            default_categories = [
                {
                    "id": "cat_debate_opening",
                    "organization_id": "system",
                    "category": "debate",
                    "subcategory": "opening",
                    "description": "Templates for debate opening statements",
                    "icon": "📢",
                    "color": "#3B82F6",
                    "sort_order": 1
                },
                {
                    "id": "cat_debate_argument",
                    "organization_id": "system",
                    "category": "debate",
                    "subcategory": "argument",
                    "description": "Templates for debate arguments",
                    "icon": "⚖️",
                    "color": "#10B981",
                    "sort_order": 2
                },
                {
                    "id": "cat_debate_rebuttal",
                    "organization_id": "system",
                    "category": "debate",
                    "subcategory": "rebuttal",
                    "description": "Templates for debate rebuttals",
                    "icon": "🛡️",
                    "color": "#F59E0B",
                    "sort_order": 3
                },
                {
                    "id": "cat_debate_closing",
                    "organization_id": "system",
                    "category": "debate",
                    "subcategory": "closing",
                    "description": "Templates for debate closing statements",
                    "icon": "🎯",
                    "color": "#EF4444",
                    "sort_order": 4
                },
                {
                    "id": "cat_prompt_system",
                    "organization_id": "system",
                    "category": "prompt",
                    "subcategory": "system",
                    "description": "System prompt templates",
                    "icon": "⚙️",
                    "color": "#6B7280",
                    "sort_order": 5
                },
                {
                    "id": "cat_evaluation_criteria",
                    "organization_id": "system",
                    "category": "evaluation",
                    "subcategory": "criteria",
                    "description": "Evaluation criteria templates",
                    "icon": "📊",
                    "color": "#8B5CF6",
                    "sort_order": 6
                }
            ]
            
            for cat_data in default_categories:
                category = TemplateCategoryTable(**cat_data)
                session.add(category)
            
            await session.commit()
            logger.info("Default template categories created")
            
        except Exception as e:
            await session.rollback()
            logger.error(f"Failed to initialize database: {e}")
            raise