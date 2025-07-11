import os
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import declarative_base
import structlog

logger = structlog.get_logger()

Base = declarative_base()


class DatabaseManager:
    """Manages database connections and sessions"""
    
    def __init__(self):
        self.database_url = os.getenv(
            "DATABASE_URL",
            "postgresql+asyncpg://context_user:context_pass@localhost:5432/context_db"
        )
        self.engine = None
        self.async_session = None
        
    async def initialize(self):
        """Initialize database connection"""
        logger.info("Initializing database connection")
        
        self.engine = create_async_engine(
            self.database_url,
            echo=False,
            pool_pre_ping=True,
            pool_size=20,
            max_overflow=40
        )
        
        self.async_session = async_sessionmaker(
            self.engine,
            class_=AsyncSession,
            expire_on_commit=False
        )
        
        # Create tables if they don't exist
        async with self.engine.begin() as conn:
            await conn.run_sync(Base.metadata.create_all)
            
        logger.info("Database initialized")
    
    def get_session(self) -> AsyncSession:
        """Get a new database session"""
        if not self.async_session:
            raise RuntimeError("Database not initialized")
        return self.async_session()
    
    async def close(self):
        """Close database connection"""
        if self.engine:
            await self.engine.dispose()