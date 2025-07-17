"""
Database performance testing under load.

This module provides comprehensive database load testing including:
- Connection pool stress testing
- Transaction throughput testing
- Query optimization validation
- Deadlock detection and recovery
- Index performance under load
- Backup and recovery performance
"""

import asyncio
import json
import logging
import random
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Any, Optional, Tuple, Callable
from dataclasses import dataclass, field
from threading import Lock, Event, Barrier
import uuid

import asyncpg
import psycopg2
from psycopg2 import pool
import pytest
from sqlalchemy import create_engine, text, MetaData, Table, Column, Integer, String, DateTime, Text, Boolean, ForeignKey
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import QueuePool

from ..framework.base import (
    PerformanceTestBase,
    TestConfiguration,
    generate_test_data,
    measure_async_time
)


@dataclass
class DatabaseLoadConfig:
    """Configuration for database load testing."""
    
    # Connection settings
    db_url: str
    max_connections: int = 50
    min_connections: int = 5
    connection_timeout: int = 30
    
    # Load testing parameters
    concurrent_users: int = 100
    transactions_per_user: int = 100
    test_duration_minutes: int = 10
    
    # Query complexity
    simple_query_ratio: float = 0.4
    complex_query_ratio: float = 0.3
    write_operation_ratio: float = 0.3
    
    # Data generation
    initial_data_size: int = 100000
    batch_size: int = 1000
    
    # Performance thresholds
    max_query_time_ms: int = 5000
    max_transaction_time_ms: int = 10000
    max_deadlock_rate: float = 0.01
    
    # Test scenarios
    enable_deadlock_testing: bool = True
    enable_index_testing: bool = True
    enable_backup_testing: bool = True
    enable_replication_testing: bool = False
    
    # Custom settings
    custom_settings: Dict[str, Any] = field(default_factory=dict)


class DatabaseSchema:
    """Database schema for load testing."""
    
    def __init__(self, engine):
        self.engine = engine
        self.metadata = MetaData()
        self.tables = self._create_tables()
    
    def _create_tables(self) -> Dict[str, Table]:
        """Create test tables."""
        # Users table
        users = Table(
            'load_test_users',
            self.metadata,
            Column('id', Integer, primary_key=True),
            Column('username', String(50), unique=True, nullable=False),
            Column('email', String(100), unique=True, nullable=False),
            Column('created_at', DateTime, default=datetime.utcnow),
            Column('updated_at', DateTime, default=datetime.utcnow, onupdate=datetime.utcnow),
            Column('is_active', Boolean, default=True),
            Column('profile_data', Text)
        )
        
        # Posts table
        posts = Table(
            'load_test_posts',
            self.metadata,
            Column('id', Integer, primary_key=True),
            Column('user_id', Integer, ForeignKey('load_test_users.id'), nullable=False),
            Column('title', String(200), nullable=False),
            Column('content', Text, nullable=False),
            Column('created_at', DateTime, default=datetime.utcnow),
            Column('updated_at', DateTime, default=datetime.utcnow, onupdate=datetime.utcnow),
            Column('view_count', Integer, default=0),
            Column('like_count', Integer, default=0),
            Column('is_published', Boolean, default=True)
        )
        
        # Comments table
        comments = Table(
            'load_test_comments',
            self.metadata,
            Column('id', Integer, primary_key=True),
            Column('post_id', Integer, ForeignKey('load_test_posts.id'), nullable=False),
            Column('user_id', Integer, ForeignKey('load_test_users.id'), nullable=False),
            Column('content', Text, nullable=False),
            Column('created_at', DateTime, default=datetime.utcnow),
            Column('updated_at', DateTime, default=datetime.utcnow, onupdate=datetime.utcnow),
            Column('is_approved', Boolean, default=True),
            Column('parent_id', Integer, ForeignKey('load_test_comments.id'))
        )
        
        # Activity log table
        activity_log = Table(
            'load_test_activity_log',
            self.metadata,
            Column('id', Integer, primary_key=True),
            Column('user_id', Integer, ForeignKey('load_test_users.id'), nullable=False),
            Column('action', String(50), nullable=False),
            Column('resource_type', String(50), nullable=False),
            Column('resource_id', Integer, nullable=False),
            Column('timestamp', DateTime, default=datetime.utcnow),
            Column('ip_address', String(45)),
            Column('user_agent', String(500)),
            Column('metadata', Text)
        )
        
        return {
            'users': users,
            'posts': posts,
            'comments': comments,
            'activity_log': activity_log
        }
    
    def create_schema(self):
        """Create database schema."""
        self.metadata.create_all(self.engine)
    
    def drop_schema(self):
        """Drop database schema."""
        self.metadata.drop_all(self.engine)
    
    def create_indexes(self):
        """Create performance indexes."""
        with self.engine.connect() as conn:
            # User indexes
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_users_username ON load_test_users(username)"))
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_users_email ON load_test_users(email)"))
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_users_created_at ON load_test_users(created_at)"))
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_users_active ON load_test_users(is_active)"))
            
            # Post indexes
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_posts_user_id ON load_test_posts(user_id)"))
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_posts_created_at ON load_test_posts(created_at)"))
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_posts_published ON load_test_posts(is_published)"))
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_posts_view_count ON load_test_posts(view_count)"))
            
            # Comment indexes
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_comments_post_id ON load_test_comments(post_id)"))
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_comments_user_id ON load_test_comments(user_id)"))
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_comments_created_at ON load_test_comments(created_at)"))
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON load_test_comments(parent_id)"))
            
            # Activity log indexes
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_activity_user_id ON load_test_activity_log(user_id)"))
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_activity_timestamp ON load_test_activity_log(timestamp)"))
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_activity_action ON load_test_activity_log(action)"))
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_activity_resource ON load_test_activity_log(resource_type, resource_id)"))
            
            # Composite indexes for common queries
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_posts_user_published ON load_test_posts(user_id, is_published)"))
            conn.execute(text("CREATE INDEX IF NOT EXISTS idx_comments_post_approved ON load_test_comments(post_id, is_approved)"))
            
            conn.commit()


class DatabaseLoadTest(PerformanceTestBase):
    """Database load testing with various scenarios."""
    
    def __init__(self, config: TestConfiguration, db_config: DatabaseLoadConfig):
        super().__init__(config)
        self.db_config = db_config
        self.engine = None
        self.session_factory = None
        self.connection_pool = None
        self.schema = None
        self.test_data = {
            'users': [],
            'posts': [],
            'comments': []
        }
        self.deadlock_count = 0
        self.deadlock_lock = Lock()
    
    async def setup_test(self):
        """Setup database load test."""
        self.logger.info("Setting up database load test")
        
        # Create database engine
        self.engine = create_engine(
            self.db_config.db_url,
            pool_class=QueuePool,
            pool_size=self.db_config.max_connections,
            max_overflow=0,
            pool_pre_ping=True,
            pool_recycle=3600,
            echo=False
        )
        
        # Create session factory
        self.session_factory = sessionmaker(bind=self.engine)
        
        # Create async connection pool
        self.connection_pool = await asyncpg.create_pool(
            self.db_config.db_url,
            min_size=self.db_config.min_connections,
            max_size=self.db_config.max_connections,
            command_timeout=self.db_config.connection_timeout
        )
        
        # Setup schema
        self.schema = DatabaseSchema(self.engine)
        self.schema.create_schema()
        self.schema.create_indexes()
        
        # Generate initial test data
        await self._generate_initial_data()
    
    async def run_test(self):
        """Run database load tests."""
        self.logger.info("Running database load tests")
        
        # Run different test scenarios
        await self._test_connection_pool_stress()
        await self._test_transaction_throughput()
        await self._test_query_performance()
        await self._test_concurrent_reads_writes()
        
        if self.db_config.enable_deadlock_testing:
            await self._test_deadlock_handling()
        
        if self.db_config.enable_index_testing:
            await self._test_index_performance()
        
        if self.db_config.enable_backup_testing:
            await self._test_backup_performance()
    
    async def cleanup_test(self):
        """Cleanup database load test."""
        if self.connection_pool:
            await self.connection_pool.close()
        
        if self.schema:
            self.schema.drop_schema()
        
        if self.engine:
            self.engine.dispose()
    
    async def _generate_initial_data(self):
        """Generate initial test data."""
        self.logger.info(f"Generating {self.db_config.initial_data_size} initial records")
        
        # Generate users
        users_data = []
        for i in range(min(self.db_config.initial_data_size, 10000)):
            users_data.append({
                'username': f'user_{i:06d}',
                'email': f'user_{i:06d}@example.com',
                'profile_data': json.dumps({
                    'bio': f'This is user {i}',
                    'location': f'City {i % 100}',
                    'interests': [f'interest_{j}' for j in range(i % 5)]
                })
            })
        
        # Insert users in batches
        async with self.connection_pool.acquire() as conn:
            for i in range(0, len(users_data), self.db_config.batch_size):
                batch = users_data[i:i + self.db_config.batch_size]
                await conn.executemany("""
                    INSERT INTO load_test_users (username, email, profile_data) 
                    VALUES ($1, $2, $3)
                """, [(u['username'], u['email'], u['profile_data']) for u in batch])
        
        # Get user IDs
        async with self.connection_pool.acquire() as conn:
            rows = await conn.fetch("SELECT id FROM load_test_users ORDER BY id")
            user_ids = [row['id'] for row in rows]
        
        self.test_data['users'] = user_ids
        
        # Generate posts
        posts_data = []
        for i in range(min(self.db_config.initial_data_size, 50000)):
            posts_data.append({
                'user_id': random.choice(user_ids),
                'title': f'Post {i}: {self._generate_random_title()}',
                'content': self._generate_random_content(),
                'view_count': random.randint(0, 1000),
                'like_count': random.randint(0, 100)
            })
        
        # Insert posts in batches
        async with self.connection_pool.acquire() as conn:
            for i in range(0, len(posts_data), self.db_config.batch_size):
                batch = posts_data[i:i + self.db_config.batch_size]
                await conn.executemany("""
                    INSERT INTO load_test_posts (user_id, title, content, view_count, like_count) 
                    VALUES ($1, $2, $3, $4, $5)
                """, [(p['user_id'], p['title'], p['content'], p['view_count'], p['like_count']) for p in batch])
        
        # Get post IDs
        async with self.connection_pool.acquire() as conn:
            rows = await conn.fetch("SELECT id, user_id FROM load_test_posts ORDER BY id")
            post_data = [(row['id'], row['user_id']) for row in rows]
        
        self.test_data['posts'] = post_data
        
        # Generate comments
        comments_data = []
        for i in range(min(self.db_config.initial_data_size, 100000)):
            post_id, post_user_id = random.choice(post_data)
            comments_data.append({
                'post_id': post_id,
                'user_id': random.choice(user_ids),
                'content': self._generate_random_comment()
            })
        
        # Insert comments in batches
        async with self.connection_pool.acquire() as conn:
            for i in range(0, len(comments_data), self.db_config.batch_size):
                batch = comments_data[i:i + self.db_config.batch_size]
                await conn.executemany("""
                    INSERT INTO load_test_comments (post_id, user_id, content) 
                    VALUES ($1, $2, $3)
                """, [(c['post_id'], c['user_id'], c['content']) for c in batch])
        
        # Get comment IDs
        async with self.connection_pool.acquire() as conn:
            rows = await conn.fetch("SELECT id FROM load_test_comments ORDER BY id")
            comment_ids = [row['id'] for row in rows]
        
        self.test_data['comments'] = comment_ids
        
        self.logger.info(f"Generated {len(user_ids)} users, {len(post_data)} posts, {len(comment_ids)} comments")
    
    def _generate_random_title(self) -> str:
        """Generate random post title."""
        topics = ['Technology', 'Science', 'Art', 'Music', 'Sports', 'Travel', 'Food', 'Health']
        adjectives = ['Amazing', 'Incredible', 'Fantastic', 'Interesting', 'Shocking', 'Beautiful']
        
        topic = random.choice(topics)
        adjective = random.choice(adjectives)
        
        return f"{adjective} {topic} Discovery"
    
    def _generate_random_content(self) -> str:
        """Generate random post content."""
        sentences = [
            "This is an interesting topic that deserves discussion.",
            "I have been researching this subject for a while now.",
            "The implications of this are far-reaching.",
            "We should consider all aspects of this issue.",
            "This development could change everything we know.",
            "I'm excited to hear your thoughts on this matter.",
            "The data supports this conclusion strongly.",
            "We need to approach this problem systematically."
        ]
        
        content = []
        for _ in range(random.randint(3, 8)):
            content.append(random.choice(sentences))
        
        return ' '.join(content)
    
    def _generate_random_comment(self) -> str:
        """Generate random comment content."""
        comments = [
            "Great post! Thanks for sharing.",
            "I agree with your points completely.",
            "This is very insightful information.",
            "Have you considered the alternative perspective?",
            "I have a different opinion on this topic.",
            "This reminds me of a similar situation.",
            "Could you provide more details?",
            "Excellent analysis of the situation."
        ]
        
        return random.choice(comments)
    
    @measure_async_time
    async def _test_connection_pool_stress(self):
        """Test connection pool under stress."""
        self.logger.info("Testing connection pool stress")
        
        async def connection_worker(worker_id: int):
            """Worker that acquires and releases connections."""
            operations = 0
            errors = 0
            
            for _ in range(100):
                try:
                    async with self.connection_pool.acquire() as conn:
                        # Simulate work
                        await conn.fetchval("SELECT 1")
                        await asyncio.sleep(random.uniform(0.01, 0.1))
                        operations += 1
                except Exception as e:
                    errors += 1
                    self.logger.error(f"Worker {worker_id} error: {e}")
            
            return {'worker_id': worker_id, 'operations': operations, 'errors': errors}
        
        # Create more workers than available connections
        workers = []
        for i in range(self.db_config.max_connections * 2):
            worker = asyncio.create_task(connection_worker(i))
            workers.append(worker)
        
        # Wait for all workers
        start_time = time.time()
        results = await asyncio.gather(*workers, return_exceptions=True)
        duration = (time.time() - start_time) * 1000
        
        # Analyze results
        successful_workers = [r for r in results if not isinstance(r, Exception)]
        total_operations = sum(r['operations'] for r in successful_workers)
        total_errors = sum(r['errors'] for r in successful_workers)
        
        self.metrics.record_operation("connection_pool_stress", duration, True, {
            'workers': len(workers),
            'successful_workers': len(successful_workers),
            'total_operations': total_operations,
            'total_errors': total_errors,
            'operations_per_second': total_operations / (duration / 1000)
        })
    
    @measure_async_time
    async def _test_transaction_throughput(self):
        """Test transaction throughput."""
        self.logger.info("Testing transaction throughput")
        
        async def transaction_worker(worker_id: int):
            """Worker that performs transactions."""
            transactions = 0
            
            for _ in range(self.db_config.transactions_per_user):
                try:
                    async with self.connection_pool.acquire() as conn:
                        async with conn.transaction():
                            # Simulate a transaction with multiple operations
                            user_id = random.choice(self.test_data['users'])
                            
                            # Insert activity log
                            await conn.execute("""
                                INSERT INTO load_test_activity_log 
                                (user_id, action, resource_type, resource_id, ip_address) 
                                VALUES ($1, $2, $3, $4, $5)
                            """, user_id, 'view', 'post', random.randint(1, 1000), '127.0.0.1')
                            
                            # Update user profile
                            await conn.execute("""
                                UPDATE load_test_users 
                                SET updated_at = CURRENT_TIMESTAMP 
                                WHERE id = $1
                            """, user_id)
                            
                            # Increment view count
                            if self.test_data['posts']:
                                post_id = random.choice(self.test_data['posts'])[0]
                                await conn.execute("""
                                    UPDATE load_test_posts 
                                    SET view_count = view_count + 1 
                                    WHERE id = $1
                                """, post_id)
                            
                            transactions += 1
                            
                except Exception as e:
                    self.logger.error(f"Transaction worker {worker_id} error: {e}")
            
            return {'worker_id': worker_id, 'transactions': transactions}
        
        # Run transaction workers
        workers = []
        for i in range(self.db_config.concurrent_users):
            worker = asyncio.create_task(transaction_worker(i))
            workers.append(worker)
        
        start_time = time.time()
        results = await asyncio.gather(*workers, return_exceptions=True)
        duration = (time.time() - start_time) * 1000
        
        # Analyze results
        successful_workers = [r for r in results if not isinstance(r, Exception)]
        total_transactions = sum(r['transactions'] for r in successful_workers)
        
        self.metrics.record_operation("transaction_throughput", duration, True, {
            'workers': len(workers),
            'total_transactions': total_transactions,
            'transactions_per_second': total_transactions / (duration / 1000)
        })
    
    @measure_async_time
    async def _test_query_performance(self):
        """Test query performance under load."""
        self.logger.info("Testing query performance")
        
        # Define query types
        query_types = [
            {
                'name': 'simple_select',
                'weight': self.db_config.simple_query_ratio,
                'query': "SELECT * FROM load_test_users WHERE id = $1",
                'params': lambda: [random.choice(self.test_data['users'])]
            },
            {
                'name': 'complex_join',
                'weight': self.db_config.complex_query_ratio,
                'query': """
                    SELECT u.username, p.title, COUNT(c.id) as comment_count
                    FROM load_test_users u
                    JOIN load_test_posts p ON u.id = p.user_id
                    LEFT JOIN load_test_comments c ON p.id = c.post_id
                    WHERE u.is_active = TRUE AND p.is_published = TRUE
                    GROUP BY u.id, u.username, p.id, p.title
                    ORDER BY comment_count DESC
                    LIMIT 10
                """,
                'params': lambda: []
            },
            {
                'name': 'aggregation',
                'weight': self.db_config.complex_query_ratio,
                'query': """
                    SELECT 
                        DATE_TRUNC('day', created_at) as day,
                        COUNT(*) as post_count,
                        AVG(view_count) as avg_views
                    FROM load_test_posts
                    WHERE created_at >= CURRENT_DATE - INTERVAL '30 days'
                    GROUP BY DATE_TRUNC('day', created_at)
                    ORDER BY day DESC
                """,
                'params': lambda: []
            }
        ]
        
        async def query_worker(worker_id: int):
            """Worker that executes queries."""
            query_results = {qt['name']: {'count': 0, 'total_time': 0} for qt in query_types}
            
            for _ in range(100):
                # Select query type based on weights
                query_type = random.choices(
                    query_types,
                    weights=[qt['weight'] for qt in query_types]
                )[0]
                
                try:
                    async with self.connection_pool.acquire() as conn:
                        start_time = time.time()
                        
                        if query_type['params']:
                            result = await conn.fetch(query_type['query'], *query_type['params']())
                        else:
                            result = await conn.fetch(query_type['query'])
                        
                        duration = (time.time() - start_time) * 1000
                        
                        query_results[query_type['name']]['count'] += 1
                        query_results[query_type['name']]['total_time'] += duration
                        
                        # Record individual query
                        self.metrics.record_operation(
                            f"query_{query_type['name']}",
                            duration,
                            True,
                            {
                                'worker_id': worker_id,
                                'rows_returned': len(result)
                            }
                        )
                        
                except Exception as e:
                    self.logger.error(f"Query worker {worker_id} error: {e}")
            
            return {'worker_id': worker_id, 'query_results': query_results}
        
        # Run query workers
        workers = []
        for i in range(self.db_config.concurrent_users):
            worker = asyncio.create_task(query_worker(i))
            workers.append(worker)
        
        start_time = time.time()
        results = await asyncio.gather(*workers, return_exceptions=True)
        duration = (time.time() - start_time) * 1000
        
        # Analyze results
        successful_workers = [r for r in results if not isinstance(r, Exception)]
        
        # Aggregate query statistics
        query_stats = {}
        for query_type in query_types:
            query_name = query_type['name']
            total_count = sum(
                r['query_results'][query_name]['count'] 
                for r in successful_workers
            )
            total_time = sum(
                r['query_results'][query_name]['total_time'] 
                for r in successful_workers
            )
            
            query_stats[query_name] = {
                'count': total_count,
                'total_time': total_time,
                'avg_time': total_time / total_count if total_count > 0 else 0,
                'queries_per_second': total_count / (duration / 1000)
            }
        
        self.metrics.record_operation("query_performance", duration, True, {
            'workers': len(workers),
            'query_stats': query_stats
        })
    
    @measure_async_time
    async def _test_concurrent_reads_writes(self):
        """Test concurrent read and write operations."""
        self.logger.info("Testing concurrent reads and writes")
        
        async def reader_worker(worker_id: int):
            """Worker that performs read operations."""
            reads = 0
            
            for _ in range(200):
                try:
                    async with self.connection_pool.acquire() as conn:
                        # Random read operation
                        operation = random.choice([
                            'user_posts',
                            'post_comments',
                            'user_activity',
                            'popular_posts'
                        ])
                        
                        if operation == 'user_posts':
                            user_id = random.choice(self.test_data['users'])
                            await conn.fetch("""
                                SELECT * FROM load_test_posts 
                                WHERE user_id = $1 AND is_published = TRUE
                                ORDER BY created_at DESC LIMIT 10
                            """, user_id)
                        
                        elif operation == 'post_comments':
                            if self.test_data['posts']:
                                post_id = random.choice(self.test_data['posts'])[0]
                                await conn.fetch("""
                                    SELECT c.*, u.username FROM load_test_comments c
                                    JOIN load_test_users u ON c.user_id = u.id
                                    WHERE c.post_id = $1 AND c.is_approved = TRUE
                                    ORDER BY c.created_at ASC
                                """, post_id)
                        
                        elif operation == 'user_activity':
                            user_id = random.choice(self.test_data['users'])
                            await conn.fetch("""
                                SELECT * FROM load_test_activity_log
                                WHERE user_id = $1
                                ORDER BY timestamp DESC LIMIT 50
                            """, user_id)
                        
                        elif operation == 'popular_posts':
                            await conn.fetch("""
                                SELECT p.*, u.username FROM load_test_posts p
                                JOIN load_test_users u ON p.user_id = u.id
                                WHERE p.is_published = TRUE
                                ORDER BY p.view_count DESC LIMIT 20
                            """)
                        
                        reads += 1
                        
                except Exception as e:
                    self.logger.error(f"Reader worker {worker_id} error: {e}")
            
            return {'worker_id': worker_id, 'reads': reads}
        
        async def writer_worker(worker_id: int):
            """Worker that performs write operations."""
            writes = 0
            
            for _ in range(50):
                try:
                    async with self.connection_pool.acquire() as conn:
                        # Random write operation
                        operation = random.choice([
                            'create_post',
                            'create_comment',
                            'update_views',
                            'log_activity'
                        ])
                        
                        if operation == 'create_post':
                            user_id = random.choice(self.test_data['users'])
                            title = f"New Post by Worker {worker_id}"
                            content = self._generate_random_content()
                            
                            await conn.execute("""
                                INSERT INTO load_test_posts (user_id, title, content)
                                VALUES ($1, $2, $3)
                            """, user_id, title, content)
                        
                        elif operation == 'create_comment':
                            if self.test_data['posts']:
                                post_id = random.choice(self.test_data['posts'])[0]
                                user_id = random.choice(self.test_data['users'])
                                content = self._generate_random_comment()
                                
                                await conn.execute("""
                                    INSERT INTO load_test_comments (post_id, user_id, content)
                                    VALUES ($1, $2, $3)
                                """, post_id, user_id, content)
                        
                        elif operation == 'update_views':
                            if self.test_data['posts']:
                                post_id = random.choice(self.test_data['posts'])[0]
                                await conn.execute("""
                                    UPDATE load_test_posts 
                                    SET view_count = view_count + 1
                                    WHERE id = $1
                                """, post_id)
                        
                        elif operation == 'log_activity':
                            user_id = random.choice(self.test_data['users'])
                            await conn.execute("""
                                INSERT INTO load_test_activity_log 
                                (user_id, action, resource_type, resource_id)
                                VALUES ($1, $2, $3, $4)
                            """, user_id, 'write', 'post', random.randint(1, 1000))
                        
                        writes += 1
                        
                except Exception as e:
                    self.logger.error(f"Writer worker {worker_id} error: {e}")
            
            return {'worker_id': worker_id, 'writes': writes}
        
        # Run mixed readers and writers
        workers = []
        
        # 70% readers, 30% writers
        reader_count = int(self.db_config.concurrent_users * 0.7)
        writer_count = self.db_config.concurrent_users - reader_count
        
        for i in range(reader_count):
            worker = asyncio.create_task(reader_worker(f"reader_{i}"))
            workers.append(worker)
        
        for i in range(writer_count):
            worker = asyncio.create_task(writer_worker(f"writer_{i}"))
            workers.append(worker)
        
        start_time = time.time()
        results = await asyncio.gather(*workers, return_exceptions=True)
        duration = (time.time() - start_time) * 1000
        
        # Analyze results
        successful_workers = [r for r in results if not isinstance(r, Exception)]
        total_reads = sum(r.get('reads', 0) for r in successful_workers)
        total_writes = sum(r.get('writes', 0) for r in successful_workers)
        
        self.metrics.record_operation("concurrent_reads_writes", duration, True, {
            'reader_workers': reader_count,
            'writer_workers': writer_count,
            'total_reads': total_reads,
            'total_writes': total_writes,
            'reads_per_second': total_reads / (duration / 1000),
            'writes_per_second': total_writes / (duration / 1000)
        })
    
    @measure_async_time
    async def _test_deadlock_handling(self):
        """Test deadlock detection and handling."""
        self.logger.info("Testing deadlock handling")
        
        async def deadlock_worker(worker_id: int):
            """Worker that may cause deadlocks."""
            operations = 0
            deadlocks = 0
            
            for _ in range(50):
                try:
                    async with self.connection_pool.acquire() as conn:
                        async with conn.transaction():
                            # Get two random posts
                            if len(self.test_data['posts']) >= 2:
                                post1_id, post2_id = random.sample(
                                    [p[0] for p in self.test_data['posts']], 2
                                )
                                
                                # Lock posts in different order to create deadlock potential
                                if worker_id % 2 == 0:
                                    first_id, second_id = post1_id, post2_id
                                else:
                                    first_id, second_id = post2_id, post1_id
                                
                                # Update first post
                                await conn.execute("""
                                    UPDATE load_test_posts 
                                    SET view_count = view_count + 1
                                    WHERE id = $1
                                """, first_id)
                                
                                # Small delay to increase deadlock chance
                                await asyncio.sleep(0.01)
                                
                                # Update second post
                                await conn.execute("""
                                    UPDATE load_test_posts 
                                    SET like_count = like_count + 1
                                    WHERE id = $1
                                """, second_id)
                                
                                operations += 1
                                
                except asyncpg.DeadlockDetectedError:
                    deadlocks += 1
                    with self.deadlock_lock:
                        self.deadlock_count += 1
                    
                    self.logger.warning(f"Deadlock detected in worker {worker_id}")
                    
                except Exception as e:
                    self.logger.error(f"Deadlock worker {worker_id} error: {e}")
            
            return {'worker_id': worker_id, 'operations': operations, 'deadlocks': deadlocks}
        
        # Run deadlock-prone workers
        workers = []
        for i in range(20):  # Fewer workers to increase contention
            worker = asyncio.create_task(deadlock_worker(i))
            workers.append(worker)
        
        start_time = time.time()
        results = await asyncio.gather(*workers, return_exceptions=True)
        duration = (time.time() - start_time) * 1000
        
        # Analyze results
        successful_workers = [r for r in results if not isinstance(r, Exception)]
        total_operations = sum(r['operations'] for r in successful_workers)
        total_deadlocks = sum(r['deadlocks'] for r in successful_workers)
        
        deadlock_rate = total_deadlocks / (total_operations + total_deadlocks) if (total_operations + total_deadlocks) > 0 else 0
        
        self.metrics.record_operation("deadlock_handling", duration, True, {
            'workers': len(workers),
            'total_operations': total_operations,
            'total_deadlocks': total_deadlocks,
            'deadlock_rate': deadlock_rate,
            'deadlock_rate_within_threshold': deadlock_rate <= self.db_config.max_deadlock_rate
        })
    
    @measure_async_time
    async def _test_index_performance(self):
        """Test index performance."""
        self.logger.info("Testing index performance")
        
        # Test queries that should use indexes
        index_queries = [
            {
                'name': 'user_lookup_by_username',
                'query': "SELECT * FROM load_test_users WHERE username = $1",
                'params': lambda: [f'user_{random.randint(0, 9999):06d}']
            },
            {
                'name': 'posts_by_user',
                'query': "SELECT * FROM load_test_posts WHERE user_id = $1",
                'params': lambda: [random.choice(self.test_data['users'])]
            },
            {
                'name': 'comments_by_post',
                'query': "SELECT * FROM load_test_comments WHERE post_id = $1",
                'params': lambda: [random.choice(self.test_data['posts'])[0]] if self.test_data['posts'] else [1]
            },
            {
                'name': 'recent_activity',
                'query': """
                    SELECT * FROM load_test_activity_log 
                    WHERE timestamp >= $1 
                    ORDER BY timestamp DESC 
                    LIMIT 100
                """,
                'params': lambda: [datetime.utcnow() - timedelta(hours=1)]
            }
        ]
        
        async def index_test_worker(worker_id: int):
            """Worker that tests index performance."""
            query_results = {}
            
            for query_config in index_queries:
                query_name = query_config['name']
                query_results[query_name] = []
                
                # Run each query multiple times
                for _ in range(20):
                    try:
                        async with self.connection_pool.acquire() as conn:
                            start_time = time.time()
                            
                            result = await conn.fetch(
                                query_config['query'], 
                                *query_config['params']()
                            )
                            
                            duration = (time.time() - start_time) * 1000
                            query_results[query_name].append({
                                'duration': duration,
                                'rows': len(result)
                            })
                            
                    except Exception as e:
                        self.logger.error(f"Index test worker {worker_id} error: {e}")
            
            return {'worker_id': worker_id, 'query_results': query_results}
        
        # Run index test workers
        workers = []
        for i in range(10):
            worker = asyncio.create_task(index_test_worker(i))
            workers.append(worker)
        
        start_time = time.time()
        results = await asyncio.gather(*workers, return_exceptions=True)
        duration = (time.time() - start_time) * 1000
        
        # Analyze results
        successful_workers = [r for r in results if not isinstance(r, Exception)]
        
        # Aggregate index performance
        index_stats = {}
        for query_config in index_queries:
            query_name = query_config['name']
            all_results = []
            
            for worker_result in successful_workers:
                all_results.extend(worker_result['query_results'][query_name])
            
            if all_results:
                durations = [r['duration'] for r in all_results]
                index_stats[query_name] = {
                    'count': len(all_results),
                    'avg_duration': sum(durations) / len(durations),
                    'min_duration': min(durations),
                    'max_duration': max(durations),
                    'avg_rows': sum(r['rows'] for r in all_results) / len(all_results)
                }
        
        self.metrics.record_operation("index_performance", duration, True, {
            'workers': len(workers),
            'index_stats': index_stats
        })
    
    @measure_async_time
    async def _test_backup_performance(self):
        """Test backup performance impact."""
        self.logger.info("Testing backup performance")
        
        # This is a simplified backup test
        # In a real scenario, you would trigger actual database backups
        
        async def backup_simulation():
            """Simulate backup operation."""
            # Simulate backup by reading all data
            async with self.connection_pool.acquire() as conn:
                tables = ['load_test_users', 'load_test_posts', 'load_test_comments', 'load_test_activity_log']
                
                for table in tables:
                    await conn.fetch(f"SELECT * FROM {table}")
                    await asyncio.sleep(0.1)  # Simulate backup processing
        
        async def normal_operation_worker(worker_id: int):
            """Worker that performs normal operations during backup."""
            operations = 0
            
            for _ in range(100):
                try:
                    async with self.connection_pool.acquire() as conn:
                        # Random operation
                        operation = random.choice(['read', 'write'])
                        
                        if operation == 'read':
                            user_id = random.choice(self.test_data['users'])
                            await conn.fetch("""
                                SELECT * FROM load_test_posts 
                                WHERE user_id = $1 LIMIT 10
                            """, user_id)
                        else:
                            user_id = random.choice(self.test_data['users'])
                            await conn.execute("""
                                INSERT INTO load_test_activity_log 
                                (user_id, action, resource_type, resource_id)
                                VALUES ($1, $2, $3, $4)
                            """, user_id, 'backup_test', 'system', 1)
                        
                        operations += 1
                        
                except Exception as e:
                    self.logger.error(f"Backup test worker {worker_id} error: {e}")
            
            return {'worker_id': worker_id, 'operations': operations}
        
        # Run backup simulation with concurrent normal operations
        backup_task = asyncio.create_task(backup_simulation())
        
        # Start normal operations
        workers = []
        for i in range(10):
            worker = asyncio.create_task(normal_operation_worker(i))
            workers.append(worker)
        
        start_time = time.time()
        
        # Wait for backup to complete
        await backup_task
        
        # Wait for workers to complete
        results = await asyncio.gather(*workers, return_exceptions=True)
        duration = (time.time() - start_time) * 1000
        
        # Analyze results
        successful_workers = [r for r in results if not isinstance(r, Exception)]
        total_operations = sum(r['operations'] for r in successful_workers)
        
        self.metrics.record_operation("backup_performance", duration, True, {
            'workers': len(workers),
            'total_operations': total_operations,
            'operations_per_second': total_operations / (duration / 1000)
        })


# Export main classes
__all__ = [
    'DatabaseLoadConfig',
    'DatabaseSchema',
    'DatabaseLoadTest'
]