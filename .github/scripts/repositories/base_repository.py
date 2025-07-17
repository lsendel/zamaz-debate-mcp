"""
Base repository implementation with common database operations.

This module provides a base class for all repositories, implementing
common CRUD operations and query building.
"""

import json
import sqlite3
from abc import ABC, abstractmethod
from contextlib import asynccontextmanager
from datetime import datetime
from typing import Any, Dict, List, Optional, Type, TypeVar

from ..core.interfaces import DatabaseInterface, RepositoryInterface
from ..core.exceptions import DatabaseError, EntityNotFoundError


T = TypeVar('T')


class Entity(ABC):
    """Base class for all entities."""
    
    @abstractmethod
    def to_dict(self) -> Dict[str, Any]:
        """Convert entity to dictionary."""
        pass
    
    @classmethod
    @abstractmethod
    def from_dict(cls: Type[T], data: Dict[str, Any]) -> T:
        """Create entity from dictionary."""
        pass


class BaseRepository(RepositoryInterface):
    """
    Base repository with common database operations.
    
    Provides:
    - Connection management
    - Transaction support
    - Common CRUD operations
    - Query building helpers
    """
    
    def __init__(self, database: DatabaseInterface, table_name: str):
        self.database = database
        self.table_name = table_name
        self._entity_class: Optional[Type[Entity]] = None
    
    def set_entity_class(self, entity_class: Type[Entity]) -> None:
        """Set the entity class for this repository."""
        self._entity_class = entity_class
    
    async def find_by_id(self, entity_id: str) -> Optional[Entity]:
        """Find entity by ID."""
        query = f"SELECT * FROM {self.table_name} WHERE id = ?"
        result = await self.database.execute(query, {'id': entity_id})
        
        if not result:
            return None
        
        return self._map_to_entity(result[0])
    
    async def find_all(
        self,
        filters: Optional[Dict[str, Any]] = None,
        order_by: Optional[str] = None,
        limit: Optional[int] = None,
        offset: Optional[int] = None
    ) -> List[Entity]:
        """Find all entities matching filters."""
        query_parts = [f"SELECT * FROM {self.table_name}"]
        params = {}
        
        # Add filters
        if filters:
            where_clauses = []
            for key, value in filters.items():
                if value is None:
                    where_clauses.append(f"{key} IS NULL")
                else:
                    where_clauses.append(f"{key} = :{key}")
                    params[key] = value
            
            if where_clauses:
                query_parts.append("WHERE " + " AND ".join(where_clauses))
        
        # Add ordering
        if order_by:
            query_parts.append(f"ORDER BY {order_by}")
        
        # Add limit and offset
        if limit:
            query_parts.append(f"LIMIT {limit}")
        if offset:
            query_parts.append(f"OFFSET {offset}")
        
        query = " ".join(query_parts)
        results = await self.database.execute(query, params)
        
        return [self._map_to_entity(row) for row in results]
    
    async def save(self, entity: Entity) -> str:
        """Save entity and return ID."""
        data = entity.to_dict()
        
        # Generate ID if not present
        if 'id' not in data or not data['id']:
            data['id'] = self._generate_id()
        
        # Add timestamps
        now = datetime.utcnow().isoformat()
        data['created_at'] = data.get('created_at', now)
        data['updated_at'] = now
        
        # Build insert query
        columns = list(data.keys())
        placeholders = [f":{col}" for col in columns]
        
        query = f"""
            INSERT INTO {self.table_name} ({', '.join(columns)})
            VALUES ({', '.join(placeholders)})
        """
        
        await self.database.execute(query, data)
        return data['id']
    
    async def update(self, entity_id: str, updates: Dict[str, Any]) -> bool:
        """Update entity."""
        # Check if entity exists
        existing = await self.find_by_id(entity_id)
        if not existing:
            raise EntityNotFoundError(self.table_name, entity_id)
        
        # Add updated timestamp
        updates['updated_at'] = datetime.utcnow().isoformat()
        
        # Build update query
        set_clauses = [f"{col} = :{col}" for col in updates.keys()]
        query = f"""
            UPDATE {self.table_name}
            SET {', '.join(set_clauses)}
            WHERE id = :id
        """
        
        params = {**updates, 'id': entity_id}
        result = await self.database.execute(query, params)
        
        return result.rowcount > 0 if hasattr(result, 'rowcount') else True
    
    async def delete(self, entity_id: str) -> bool:
        """Delete entity."""
        query = f"DELETE FROM {self.table_name} WHERE id = ?"
        result = await self.database.execute(query, {'id': entity_id})
        
        return result.rowcount > 0 if hasattr(result, 'rowcount') else True
    
    async def count(self, filters: Optional[Dict[str, Any]] = None) -> int:
        """Count entities matching filters."""
        query_parts = [f"SELECT COUNT(*) FROM {self.table_name}"]
        params = {}
        
        if filters:
            where_clauses = []
            for key, value in filters.items():
                if value is None:
                    where_clauses.append(f"{key} IS NULL")
                else:
                    where_clauses.append(f"{key} = :{key}")
                    params[key] = value
            
            if where_clauses:
                query_parts.append("WHERE " + " AND ".join(where_clauses))
        
        query = " ".join(query_parts)
        result = await self.database.execute(query, params)
        
        return result[0][0] if result else 0
    
    async def exists(self, entity_id: str) -> bool:
        """Check if entity exists."""
        query = f"SELECT 1 FROM {self.table_name} WHERE id = ? LIMIT 1"
        result = await self.database.execute(query, {'id': entity_id})
        
        return bool(result)
    
    @asynccontextmanager
    async def transaction(self):
        """Execute operations in a transaction."""
        async with self.database.transaction() as tx:
            yield tx
    
    def _map_to_entity(self, row: Any) -> Entity:
        """Map database row to entity."""
        if not self._entity_class:
            raise DatabaseError("Entity class not set for repository")
        
        # Convert row to dictionary (handle different database response formats)
        if isinstance(row, dict):
            data = row
        elif hasattr(row, '_asdict'):  # namedtuple
            data = row._asdict()
        elif hasattr(row, 'keys'):  # sqlite3.Row
            data = dict(row)
        else:
            # Assume it's a tuple/list with column order matching entity
            # This would need column mapping in production
            raise DatabaseError("Cannot map row to entity without column information")
        
        return self._entity_class.from_dict(data)
    
    def _generate_id(self) -> str:
        """Generate unique ID for entity."""
        import uuid
        return str(uuid.uuid4())


class SQLiteRepository(BaseRepository):
    """SQLite-specific repository implementation."""
    
    def __init__(self, db_path: str, table_name: str):
        from ..implementations.sqlite_database import SQLiteDatabase
        database = SQLiteDatabase(db_path)
        super().__init__(database, table_name)
    
    async def create_table(self, schema: str) -> None:
        """Create table with given schema."""
        await self.database.execute(schema)
    
    async def create_index(self, index_name: str, columns: List[str]) -> None:
        """Create index on columns."""
        columns_str = ', '.join(columns)
        query = f"CREATE INDEX IF NOT EXISTS {index_name} ON {self.table_name} ({columns_str})"
        await self.database.execute(query)