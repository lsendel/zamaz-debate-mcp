"""
Database connection and session management for Organization MCP
"""

import os
import asyncio
from typing import AsyncGenerator, Optional
from contextlib import asynccontextmanager
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.pool import NullPool
import structlog

from ..models import Base

logger = structlog.get_logger()


class DatabaseManager:
    """Manages database connections and sessions"""
    
    def __init__(self):
        self.engine = None
        self.session_factory = None
        self._initialized = False
    
    async def initialize(self):
        """Initialize database connection"""
        if self._initialized:
            return
        
        # Database configuration
        db_host = os.getenv("POSTGRES_HOST", "localhost")
        db_port = os.getenv("POSTGRES_PORT", "5432")
        db_name = os.getenv("POSTGRES_DB", "debate_org")
        db_user = os.getenv("POSTGRES_USER", "postgres")
        db_password = os.getenv("POSTGRES_PASSWORD", "password")
        
        # Create database URL
        database_url = f"postgresql+asyncpg://{db_user}:{db_password}@{db_host}:{db_port}/{db_name}"
        
        logger.info("Initializing database connection", 
                   host=db_host, port=db_port, database=db_name)
        
        try:
            # Create async engine
            self.engine = create_async_engine(
                database_url,
                poolclass=NullPool,  # Use NullPool for MCP servers
                echo=os.getenv("SQL_DEBUG", "false").lower() == "true",
                pool_pre_ping=True,
                pool_recycle=3600,  # Recycle connections every hour
                connect_args={
                    "server_settings": {
                        "jit": "off",  # Disable JIT for better compatibility
                        "application_name": "mcp-organization"
                    }
                }
            )
            
            # Create session factory
            self.session_factory = async_sessionmaker(
                self.engine,
                class_=AsyncSession,
                expire_on_commit=False
            )
            
            # Test connection
            await self._test_connection()
            
            # Create tables if they don't exist
            await self._create_tables()
            
            self._initialized = True
            logger.info("Database connection initialized successfully")
            
        except Exception as e:
            logger.error("Failed to initialize database connection", error=str(e))
            raise
    
    async def _test_connection(self):
        """Test database connection"""
        try:
            async with self.engine.begin() as conn:
                await conn.execute("SELECT 1")
            logger.info("Database connection test successful")
        except Exception as e:
            logger.error("Database connection test failed", error=str(e))
            raise
    
    async def _create_tables(self):
        """Create database tables"""
        try:
            async with self.engine.begin() as conn:
                await conn.run_sync(Base.metadata.create_all)
            logger.info("Database tables created/verified successfully")
        except Exception as e:
            logger.error("Failed to create database tables", error=str(e))
            raise
    
    @asynccontextmanager
    async def get_session(self) -> AsyncGenerator[AsyncSession, None]:
        """Get database session"""
        if not self._initialized:
            await self.initialize()
        
        async with self.session_factory() as session:
            try:
                yield session
            except Exception as e:
                logger.error("Database session error", error=str(e))
                await session.rollback()
                raise
            finally:
                await session.close()
    
    async def get_session_direct(self) -> AsyncSession:
        """Get database session directly (for dependency injection)"""
        if not self._initialized:
            await self.initialize()
        
        return self.session_factory()
    
    async def close(self):
        """Close database connections"""
        if self.engine:
            await self.engine.dispose()
            logger.info("Database connections closed")
        self._initialized = False
    
    async def health_check(self) -> dict:
        """Check database health"""
        try:
            async with self.get_session() as session:
                result = await session.execute("SELECT 1")
                await result.fetchone()
                
                # Get connection pool info
                pool = self.engine.pool
                pool_info = {
                    "size": pool.size(),
                    "checked_in": pool.checkedin(),
                    "checked_out": pool.checkedout(),
                    "invalidated": pool.invalidated()
                }
                
                return {
                    "status": "healthy",
                    "connection_pool": pool_info,
                    "timestamp": asyncio.get_event_loop().time()
                }
                
        except Exception as e:
            logger.error("Database health check failed", error=str(e))
            return {
                "status": "unhealthy",
                "error": str(e),
                "timestamp": asyncio.get_event_loop().time()
            }


# Global database manager instance
db_manager = DatabaseManager()


async def get_db_session() -> AsyncGenerator[AsyncSession, None]:
    """Dependency for getting database session"""
    async with db_manager.get_session() as session:
        yield session


async def init_database():
    """Initialize database on startup"""
    await db_manager.initialize()


async def close_database():
    """Close database on shutdown"""
    await db_manager.close()