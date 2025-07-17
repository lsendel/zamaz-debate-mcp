"""
Component-specific performance benchmarking tests.

This module contains benchmarks for individual components of the MCP system:
- Database operations
- API endpoints
- MCP protocol handlers
- LLM integrations
- Memory/CPU intensive operations
"""

import asyncio
import json
import logging
import random
import time
from pathlib import Path
from typing import Dict, List, Any, Optional
from unittest.mock import Mock, AsyncMock

import aiohttp
import pytest
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

from ..framework.base import (
    PerformanceTestBase,
    TestConfiguration,
    http_client_session,
    generate_test_data,
    measure_async_time
)


class DatabaseBenchmark(PerformanceTestBase):
    """Benchmark database operations."""
    
    def __init__(self, config: TestConfiguration, db_url: str):
        super().__init__(config)
        self.db_url = db_url
        self.engine = None
        self.session_factory = None
        self.test_data = []
    
    async def setup_test(self):
        """Setup database connection and test data."""
        self.logger.info("Setting up database benchmark")
        
        # Create database connection
        self.engine = create_engine(self.db_url, pool_size=self.config.db_pool_size)
        self.session_factory = sessionmaker(bind=self.engine)
        
        # Generate test data
        self.test_data = generate_test_data(self.config.test_data_size, "sequential")
        
        # Create test tables
        await self._create_test_tables()
        
        # Insert test data
        await self._insert_test_data()
    
    async def run_test(self):
        """Run database benchmark tests."""
        self.logger.info("Running database benchmark tests")
        
        # Test different database operations
        await self._benchmark_inserts()
        await self._benchmark_selects()
        await self._benchmark_updates()
        await self._benchmark_deletes()
        await self._benchmark_complex_queries()
        await self._benchmark_transactions()
        await self._benchmark_concurrent_operations()
    
    async def cleanup_test(self):
        """Cleanup database resources."""
        if self.engine:
            await self._drop_test_tables()
            self.engine.dispose()
    
    async def _create_test_tables(self):
        """Create test tables."""
        with self.engine.connect() as conn:
            conn.execute(text("""
                CREATE TABLE IF NOT EXISTS test_items (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    value INTEGER NOT NULL,
                    data TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                
                CREATE TABLE IF NOT EXISTS test_relations (
                    id SERIAL PRIMARY KEY,
                    item_id INTEGER REFERENCES test_items(id),
                    relation_type VARCHAR(50),
                    metadata JSONB,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                
                CREATE INDEX IF NOT EXISTS idx_items_name ON test_items(name);
                CREATE INDEX IF NOT EXISTS idx_items_value ON test_items(value);
                CREATE INDEX IF NOT EXISTS idx_relations_item_id ON test_relations(item_id);
                CREATE INDEX IF NOT EXISTS idx_relations_type ON test_relations(relation_type);
            """))
            conn.commit()
    
    async def _insert_test_data(self):
        """Insert initial test data."""
        batch_size = 1000
        
        for i in range(0, len(self.test_data), batch_size):
            batch = self.test_data[i:i + batch_size]
            
            with self.engine.connect() as conn:
                for item in batch:
                    conn.execute(text(
                        "INSERT INTO test_items (name, value, data) VALUES (:name, :value, :data)"
                    ), {
                        'name': item['name'],
                        'value': item['value'],
                        'data': item['data']
                    })
                conn.commit()
    
    @measure_async_time
    async def _benchmark_inserts(self):
        """Benchmark insert operations."""
        self.logger.info("Benchmarking insert operations")
        
        # Single inserts
        start_time = time.time()
        for i in range(100):
            with self.engine.connect() as conn:
                conn.execute(text(
                    "INSERT INTO test_items (name, value, data) VALUES (:name, :value, :data)"
                ), {
                    'name': f'bench_item_{i}',
                    'value': i,
                    'data': f'benchmark_data_{i}' * 10
                })
                conn.commit()
        
        single_insert_time = (time.time() - start_time) * 1000
        self.metrics.record_operation("single_insert", single_insert_time, True, 
                                    {"operation_count": 100})
        
        # Batch inserts
        start_time = time.time()
        batch_data = []
        for i in range(1000):
            batch_data.append({
                'name': f'batch_item_{i}',
                'value': i,
                'data': f'batch_data_{i}' * 10
            })
        
        with self.engine.connect() as conn:
            conn.execute(text(
                "INSERT INTO test_items (name, value, data) VALUES (:name, :value, :data)"
            ), batch_data)
            conn.commit()
        
        batch_insert_time = (time.time() - start_time) * 1000
        self.metrics.record_operation("batch_insert", batch_insert_time, True,
                                    {"operation_count": 1000})
    
    @measure_async_time
    async def _benchmark_selects(self):
        """Benchmark select operations."""
        self.logger.info("Benchmarking select operations")
        
        # Simple select by ID
        start_time = time.time()
        with self.engine.connect() as conn:
            for i in range(100):
                result = conn.execute(text(
                    "SELECT * FROM test_items WHERE id = :id"
                ), {'id': random.randint(1, 1000)})
                result.fetchone()
        
        select_by_id_time = (time.time() - start_time) * 1000
        self.metrics.record_operation("select_by_id", select_by_id_time, True,
                                    {"operation_count": 100})
        
        # Select with WHERE clause
        start_time = time.time()
        with self.engine.connect() as conn:
            for i in range(50):
                result = conn.execute(text(
                    "SELECT * FROM test_items WHERE value > :value ORDER BY value LIMIT 10"
                ), {'value': random.randint(1, 500)})
                result.fetchall()
        
        select_with_where_time = (time.time() - start_time) * 1000
        self.metrics.record_operation("select_with_where", select_with_where_time, True,
                                    {"operation_count": 50})
        
        # Complex select with JOIN
        start_time = time.time()
        with self.engine.connect() as conn:
            for i in range(20):
                result = conn.execute(text("""
                    SELECT i.*, r.relation_type, r.metadata
                    FROM test_items i
                    LEFT JOIN test_relations r ON i.id = r.item_id
                    WHERE i.value > :value
                    ORDER BY i.created_at DESC
                    LIMIT 20
                """), {'value': random.randint(1, 100)})
                result.fetchall()
        
        complex_select_time = (time.time() - start_time) * 1000
        self.metrics.record_operation("complex_select", complex_select_time, True,
                                    {"operation_count": 20})
    
    @measure_async_time
    async def _benchmark_updates(self):
        """Benchmark update operations."""
        self.logger.info("Benchmarking update operations")
        
        # Single updates
        start_time = time.time()
        with self.engine.connect() as conn:
            for i in range(100):
                conn.execute(text(
                    "UPDATE test_items SET value = value + 1, updated_at = CURRENT_TIMESTAMP WHERE id = :id"
                ), {'id': random.randint(1, 1000)})
            conn.commit()
        
        single_update_time = (time.time() - start_time) * 1000
        self.metrics.record_operation("single_update", single_update_time, True,
                                    {"operation_count": 100})
        
        # Batch updates
        start_time = time.time()
        with self.engine.connect() as conn:
            conn.execute(text(
                "UPDATE test_items SET value = value * 2 WHERE value < 500"
            ))
            conn.commit()
        
        batch_update_time = (time.time() - start_time) * 1000
        self.metrics.record_operation("batch_update", batch_update_time, True,
                                    {"operation_count": 1})
    
    @measure_async_time
    async def _benchmark_deletes(self):
        """Benchmark delete operations."""
        self.logger.info("Benchmarking delete operations")
        
        # Single deletes
        start_time = time.time()
        with self.engine.connect() as conn:
            for i in range(50):
                conn.execute(text(
                    "DELETE FROM test_items WHERE name = :name"
                ), {'name': f'bench_item_{i}'})
            conn.commit()
        
        single_delete_time = (time.time() - start_time) * 1000
        self.metrics.record_operation("single_delete", single_delete_time, True,
                                    {"operation_count": 50})
    
    @measure_async_time
    async def _benchmark_complex_queries(self):
        """Benchmark complex queries."""
        self.logger.info("Benchmarking complex queries")
        
        # Aggregation query
        start_time = time.time()
        with self.engine.connect() as conn:
            for i in range(10):
                result = conn.execute(text("""
                    SELECT 
                        COUNT(*) as total_items,
                        AVG(value) as avg_value,
                        MAX(value) as max_value,
                        MIN(value) as min_value
                    FROM test_items
                    WHERE created_at > CURRENT_DATE - INTERVAL '1 day'
                """))
                result.fetchone()
        
        aggregation_time = (time.time() - start_time) * 1000
        self.metrics.record_operation("aggregation_query", aggregation_time, True,
                                    {"operation_count": 10})
        
        # Window function query
        start_time = time.time()
        with self.engine.connect() as conn:
            for i in range(5):
                result = conn.execute(text("""
                    SELECT 
                        id,
                        name,
                        value,
                        ROW_NUMBER() OVER (ORDER BY value DESC) as rank,
                        LAG(value) OVER (ORDER BY value DESC) as prev_value
                    FROM test_items
                    WHERE value > 100
                    ORDER BY value DESC
                    LIMIT 50
                """))
                result.fetchall()
        
        window_function_time = (time.time() - start_time) * 1000
        self.metrics.record_operation("window_function_query", window_function_time, True,
                                    {"operation_count": 5})
    
    @measure_async_time
    async def _benchmark_transactions(self):
        """Benchmark transaction operations."""
        self.logger.info("Benchmarking transaction operations")
        
        # Simple transactions
        start_time = time.time()
        for i in range(50):
            with self.engine.connect() as conn:
                trans = conn.begin()
                try:
                    conn.execute(text(
                        "INSERT INTO test_items (name, value, data) VALUES (:name, :value, :data)"
                    ), {
                        'name': f'trans_item_{i}',
                        'value': i,
                        'data': f'transaction_data_{i}'
                    })
                    
                    conn.execute(text(
                        "UPDATE test_items SET value = value + 1 WHERE name = :name"
                    ), {'name': f'trans_item_{i}'})
                    
                    trans.commit()
                except Exception:
                    trans.rollback()
                    raise
        
        transaction_time = (time.time() - start_time) * 1000
        self.metrics.record_operation("transaction", transaction_time, True,
                                    {"operation_count": 50})
    
    @measure_async_time
    async def _benchmark_concurrent_operations(self):
        """Benchmark concurrent database operations."""
        self.logger.info("Benchmarking concurrent database operations")
        
        async def concurrent_operation(operation_id: int):
            """Single concurrent operation."""
            start_time = time.time()
            
            with self.engine.connect() as conn:
                # Mix of operations
                if operation_id % 4 == 0:
                    # Insert
                    conn.execute(text(
                        "INSERT INTO test_items (name, value, data) VALUES (:name, :value, :data)"
                    ), {
                        'name': f'concurrent_item_{operation_id}',
                        'value': operation_id,
                        'data': f'concurrent_data_{operation_id}'
                    })
                elif operation_id % 4 == 1:
                    # Select
                    result = conn.execute(text(
                        "SELECT * FROM test_items WHERE value > :value LIMIT 10"
                    ), {'value': random.randint(1, 100)})
                    result.fetchall()
                elif operation_id % 4 == 2:
                    # Update
                    conn.execute(text(
                        "UPDATE test_items SET value = value + 1 WHERE id = :id"
                    ), {'id': random.randint(1, 500)})
                else:
                    # Complex query
                    result = conn.execute(text("""
                        SELECT COUNT(*), AVG(value) FROM test_items 
                        WHERE value BETWEEN :min_val AND :max_val
                    """), {
                        'min_val': random.randint(1, 100),
                        'max_val': random.randint(200, 300)
                    })
                    result.fetchone()
                
                conn.commit()
            
            duration = (time.time() - start_time) * 1000
            return operation_id, duration
        
        # Run concurrent operations
        tasks = [concurrent_operation(i) for i in range(100)]
        results = await asyncio.gather(*tasks)
        
        total_time = sum(result[1] for result in results)
        avg_time = total_time / len(results)
        
        self.metrics.record_operation("concurrent_db_ops", avg_time, True,
                                    {"operation_count": 100, "concurrency": len(tasks)})
    
    async def _drop_test_tables(self):
        """Drop test tables."""
        with self.engine.connect() as conn:
            conn.execute(text("DROP TABLE IF EXISTS test_relations"))
            conn.execute(text("DROP TABLE IF EXISTS test_items"))
            conn.commit()


class APIMixin:
    """Mixin for API benchmark tests."""
    
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.session = None
    
    async def setup_api_client(self):
        """Setup API client."""
        self.session = aiohttp.ClientSession()
    
    async def cleanup_api_client(self):
        """Cleanup API client."""
        if self.session:
            await self.session.close()
    
    async def api_request(self, method: str, endpoint: str, **kwargs) -> Dict[str, Any]:
        """Make API request with timing."""
        url = f"{self.base_url}{endpoint}"
        start_time = time.time()
        
        try:
            async with self.session.request(method, url, **kwargs) as response:
                data = await response.json()
                success = response.status < 400
                duration = (time.time() - start_time) * 1000
                
                return {
                    'success': success,
                    'duration_ms': duration,
                    'status_code': response.status,
                    'data': data
                }
        except Exception as e:
            duration = (time.time() - start_time) * 1000
            return {
                'success': False,
                'duration_ms': duration,
                'error': str(e),
                'data': None
            }


class DebateAPIBenchmark(PerformanceTestBase, APIMixin):
    """Benchmark debate API operations."""
    
    def __init__(self, config: TestConfiguration, api_base_url: str):
        PerformanceTestBase.__init__(self, config)
        APIMixin.__init__(self, api_base_url)
        self.test_debates = []
        self.test_organizations = []
    
    async def setup_test(self):
        """Setup API benchmark."""
        await self.setup_api_client()
        
        # Create test organizations
        for i in range(10):
            org_data = {
                'name': f'Test Organization {i}',
                'description': f'Test organization for benchmarking {i}'
            }
            result = await self.api_request('POST', '/organizations', json=org_data)
            if result['success']:
                self.test_organizations.append(result['data'])
        
        # Create test debates
        for i in range(50):
            debate_data = {
                'title': f'Test Debate {i}',
                'description': f'Test debate for benchmarking {i}',
                'organization_id': random.choice(self.test_organizations)['id'] if self.test_organizations else None,
                'max_rounds': 3,
                'participants': [
                    {'name': 'Claude', 'model': 'claude-3-opus'},
                    {'name': 'GPT-4', 'model': 'gpt-4'}
                ]
            }
            result = await self.api_request('POST', '/debates', json=debate_data)
            if result['success']:
                self.test_debates.append(result['data'])
    
    async def run_test(self):
        """Run API benchmark tests."""
        await self._benchmark_organization_operations()
        await self._benchmark_debate_operations()
        await self._benchmark_concurrent_api_calls()
        await self._benchmark_large_payload_operations()
    
    async def cleanup_test(self):
        """Cleanup API benchmark."""
        # Clean up test data
        for debate in self.test_debates:
            await self.api_request('DELETE', f'/debates/{debate["id"]}')
        
        for org in self.test_organizations:
            await self.api_request('DELETE', f'/organizations/{org["id"]}')
        
        await self.cleanup_api_client()
    
    @measure_async_time
    async def _benchmark_organization_operations(self):
        """Benchmark organization API operations."""
        self.logger.info("Benchmarking organization API operations")
        
        # Create operations
        start_time = time.time()
        for i in range(20):
            result = await self.api_request('POST', '/organizations', json={
                'name': f'Benchmark Org {i}',
                'description': f'Benchmark organization {i}'
            })
            self.metrics.record_operation("create_organization", result['duration_ms'], 
                                        result['success'])
        
        # List operations
        start_time = time.time()
        for i in range(50):
            result = await self.api_request('GET', '/organizations')
            self.metrics.record_operation("list_organizations", result['duration_ms'], 
                                        result['success'])
        
        # Get operations
        for org in self.test_organizations[:10]:
            result = await self.api_request('GET', f'/organizations/{org["id"]}')
            self.metrics.record_operation("get_organization", result['duration_ms'], 
                                        result['success'])
        
        # Update operations
        for org in self.test_organizations[:5]:
            result = await self.api_request('PUT', f'/organizations/{org["id"]}', json={
                'name': f'{org["name"]} Updated',
                'description': f'{org["description"]} Updated'
            })
            self.metrics.record_operation("update_organization", result['duration_ms'], 
                                        result['success'])
    
    @measure_async_time
    async def _benchmark_debate_operations(self):
        """Benchmark debate API operations."""
        self.logger.info("Benchmarking debate API operations")
        
        # Create debates
        for i in range(30):
            debate_data = {
                'title': f'Benchmark Debate {i}',
                'description': f'Benchmark debate {i}',
                'max_rounds': 5,
                'participants': [
                    {'name': 'Claude', 'model': 'claude-3-opus'},
                    {'name': 'GPT-4', 'model': 'gpt-4'}
                ]
            }
            result = await self.api_request('POST', '/debates', json=debate_data)
            self.metrics.record_operation("create_debate", result['duration_ms'], 
                                        result['success'])
        
        # List debates
        for i in range(100):
            result = await self.api_request('GET', '/debates')
            self.metrics.record_operation("list_debates", result['duration_ms'], 
                                        result['success'])
        
        # Get debate details
        for debate in self.test_debates[:20]:
            result = await self.api_request('GET', f'/debates/{debate["id"]}')
            self.metrics.record_operation("get_debate", result['duration_ms'], 
                                        result['success'])
        
        # Start debates
        for debate in self.test_debates[:10]:
            result = await self.api_request('POST', f'/debates/{debate["id"]}/start')
            self.metrics.record_operation("start_debate", result['duration_ms'], 
                                        result['success'])
        
        # Add messages
        for debate in self.test_debates[:5]:
            for round_num in range(3):
                message_data = {
                    'content': f'This is a benchmark message for round {round_num}. ' * 20,
                    'participant': 'Claude',
                    'round': round_num
                }
                result = await self.api_request('POST', f'/debates/{debate["id"]}/messages', 
                                              json=message_data)
                self.metrics.record_operation("add_message", result['duration_ms'], 
                                            result['success'])
    
    @measure_async_time
    async def _benchmark_concurrent_api_calls(self):
        """Benchmark concurrent API calls."""
        self.logger.info("Benchmarking concurrent API calls")
        
        async def concurrent_request(request_id: int):
            """Single concurrent request."""
            # Mix of different API calls
            if request_id % 3 == 0:
                return await self.api_request('GET', '/organizations')
            elif request_id % 3 == 1:
                return await self.api_request('GET', '/debates')
            else:
                if self.test_debates:
                    debate_id = random.choice(self.test_debates)['id']
                    return await self.api_request('GET', f'/debates/{debate_id}')
                else:
                    return await self.api_request('GET', '/debates')
        
        # Run concurrent requests
        tasks = [concurrent_request(i) for i in range(100)]
        results = await asyncio.gather(*tasks)
        
        for i, result in enumerate(results):
            self.metrics.record_operation("concurrent_api_call", result['duration_ms'], 
                                        result['success'], {"request_id": i})
    
    @measure_async_time
    async def _benchmark_large_payload_operations(self):
        """Benchmark operations with large payloads."""
        self.logger.info("Benchmarking large payload operations")
        
        # Create debate with large description
        large_description = "This is a large debate description. " * 1000
        debate_data = {
            'title': 'Large Payload Debate',
            'description': large_description,
            'max_rounds': 10,
            'participants': [
                {'name': 'Claude', 'model': 'claude-3-opus'},
                {'name': 'GPT-4', 'model': 'gpt-4'}
            ]
        }
        
        result = await self.api_request('POST', '/debates', json=debate_data)
        self.metrics.record_operation("create_large_debate", result['duration_ms'], 
                                    result['success'], {"payload_size": len(json.dumps(debate_data))})
        
        # Add large messages
        if result['success']:
            debate_id = result['data']['id']
            large_message = "This is a very long debate message. " * 500
            
            for i in range(5):
                message_data = {
                    'content': large_message,
                    'participant': 'Claude',
                    'round': i
                }
                result = await self.api_request('POST', f'/debates/{debate_id}/messages', 
                                              json=message_data)
                self.metrics.record_operation("add_large_message", result['duration_ms'], 
                                            result['success'], 
                                            {"message_size": len(large_message)})


class LLMIntegrationBenchmark(PerformanceTestBase):
    """Benchmark LLM integration operations."""
    
    def __init__(self, config: TestConfiguration, llm_api_url: str):
        super().__init__(config)
        self.llm_api_url = llm_api_url
        self.test_prompts = []
        self.session = None
    
    async def setup_test(self):
        """Setup LLM benchmark."""
        self.session = aiohttp.ClientSession()
        
        # Generate test prompts of various sizes
        self.test_prompts = [
            "What is the capital of France?",  # Short
            "Explain the concept of machine learning in simple terms. " * 5,  # Medium
            "Write a detailed essay about the impact of artificial intelligence on society. " * 20,  # Long
            "Analyze the following code and suggest improvements: " + "def example(): pass\n" * 100,  # Code
            "Translate the following text to Spanish: " + "Hello, how are you? " * 50,  # Translation
        ]
    
    async def run_test(self):
        """Run LLM benchmark tests."""
        await self._benchmark_llm_requests()
        await self._benchmark_concurrent_llm_requests()
        await self._benchmark_streaming_responses()
        await self._benchmark_different_models()
    
    async def cleanup_test(self):
        """Cleanup LLM benchmark."""
        if self.session:
            await self.session.close()
    
    @measure_async_time
    async def _benchmark_llm_requests(self):
        """Benchmark LLM API requests."""
        self.logger.info("Benchmarking LLM API requests")
        
        for i, prompt in enumerate(self.test_prompts):
            for j in range(10):  # Test each prompt 10 times
                start_time = time.time()
                
                try:
                    async with self.session.post(
                        f"{self.llm_api_url}/chat/completions",
                        json={
                            "model": "claude-3-opus",
                            "messages": [{"role": "user", "content": prompt}],
                            "max_tokens": 1000
                        }
                    ) as response:
                        data = await response.json()
                        success = response.status == 200
                        duration = (time.time() - start_time) * 1000
                        
                        self.metrics.record_operation("llm_request", duration, success, {
                            "prompt_type": f"prompt_{i}",
                            "prompt_length": len(prompt),
                            "response_length": len(str(data)) if success else 0
                        })
                except Exception as e:
                    duration = (time.time() - start_time) * 1000
                    self.metrics.record_operation("llm_request", duration, False, {
                        "error": str(e),
                        "prompt_type": f"prompt_{i}"
                    })
    
    @measure_async_time
    async def _benchmark_concurrent_llm_requests(self):
        """Benchmark concurrent LLM requests."""
        self.logger.info("Benchmarking concurrent LLM requests")
        
        async def llm_request(request_id: int):
            """Single LLM request."""
            prompt = random.choice(self.test_prompts)
            start_time = time.time()
            
            try:
                async with self.session.post(
                    f"{self.llm_api_url}/chat/completions",
                    json={
                        "model": "claude-3-opus",
                        "messages": [{"role": "user", "content": prompt}],
                        "max_tokens": 500
                    }
                ) as response:
                    data = await response.json()
                    success = response.status == 200
                    duration = (time.time() - start_time) * 1000
                    
                    return {
                        'request_id': request_id,
                        'success': success,
                        'duration_ms': duration,
                        'response_size': len(str(data)) if success else 0
                    }
            except Exception as e:
                duration = (time.time() - start_time) * 1000
                return {
                    'request_id': request_id,
                    'success': False,
                    'duration_ms': duration,
                    'error': str(e)
                }
        
        # Run concurrent requests
        tasks = [llm_request(i) for i in range(20)]
        results = await asyncio.gather(*tasks)
        
        for result in results:
            self.metrics.record_operation("concurrent_llm_request", result['duration_ms'], 
                                        result['success'], result)
    
    @measure_async_time
    async def _benchmark_streaming_responses(self):
        """Benchmark streaming LLM responses."""
        self.logger.info("Benchmarking streaming LLM responses")
        
        for i in range(5):
            start_time = time.time()
            
            try:
                async with self.session.post(
                    f"{self.llm_api_url}/chat/completions",
                    json={
                        "model": "claude-3-opus",
                        "messages": [{"role": "user", "content": "Write a long story about space exploration."}],
                        "max_tokens": 2000,
                        "stream": True
                    }
                ) as response:
                    
                    chunks_received = 0
                    total_content_length = 0
                    
                    async for chunk in response.content.iter_chunked(1024):
                        chunks_received += 1
                        total_content_length += len(chunk)
                    
                    success = response.status == 200
                    duration = (time.time() - start_time) * 1000
                    
                    self.metrics.record_operation("streaming_llm_request", duration, success, {
                        "chunks_received": chunks_received,
                        "total_content_length": total_content_length
                    })
            except Exception as e:
                duration = (time.time() - start_time) * 1000
                self.metrics.record_operation("streaming_llm_request", duration, False, {
                    "error": str(e)
                })
    
    @measure_async_time
    async def _benchmark_different_models(self):
        """Benchmark different LLM models."""
        self.logger.info("Benchmarking different LLM models")
        
        models = ["claude-3-opus", "gpt-4", "gemini-pro", "llama-2-70b"]
        prompt = "Explain quantum computing in simple terms."
        
        for model in models:
            for i in range(3):
                start_time = time.time()
                
                try:
                    async with self.session.post(
                        f"{self.llm_api_url}/chat/completions",
                        json={
                            "model": model,
                            "messages": [{"role": "user", "content": prompt}],
                            "max_tokens": 1000
                        }
                    ) as response:
                        data = await response.json()
                        success = response.status == 200
                        duration = (time.time() - start_time) * 1000
                        
                        self.metrics.record_operation(f"llm_model_{model}", duration, success, {
                            "model": model,
                            "response_length": len(str(data)) if success else 0
                        })
                except Exception as e:
                    duration = (time.time() - start_time) * 1000
                    self.metrics.record_operation(f"llm_model_{model}", duration, False, {
                        "model": model,
                        "error": str(e)
                    })


class MemoryIntensiveBenchmark(PerformanceTestBase):
    """Benchmark memory-intensive operations."""
    
    def __init__(self, config: TestConfiguration):
        super().__init__(config)
        self.large_datasets = []
    
    async def setup_test(self):
        """Setup memory benchmark."""
        self.logger.info("Setting up memory intensive benchmark")
        
        # Generate large datasets
        for i in range(5):
            dataset = generate_test_data(10000, "random")
            self.large_datasets.append(dataset)
    
    async def run_test(self):
        """Run memory benchmark tests."""
        await self._benchmark_large_data_processing()
        await self._benchmark_memory_allocation()
        await self._benchmark_garbage_collection()
    
    async def cleanup_test(self):
        """Cleanup memory benchmark."""
        self.large_datasets.clear()
    
    @measure_async_time
    async def _benchmark_large_data_processing(self):
        """Benchmark large data processing operations."""
        self.logger.info("Benchmarking large data processing")
        
        for i, dataset in enumerate(self.large_datasets):
            start_time = time.time()
            
            # Process data
            processed_data = []
            for item in dataset:
                processed_item = {
                    'id': item['id'],
                    'processed_name': item['name'].upper(),
                    'doubled_value': item['value'] * 2,
                    'extended_data': item['data'] * 2
                }
                processed_data.append(processed_item)
            
            # Sort by value
            processed_data.sort(key=lambda x: x['doubled_value'])
            
            # Filter high values
            high_value_items = [item for item in processed_data if item['doubled_value'] > 1000]
            
            duration = (time.time() - start_time) * 1000
            self.metrics.record_operation("large_data_processing", duration, True, {
                "dataset_size": len(dataset),
                "processed_items": len(processed_data),
                "high_value_items": len(high_value_items)
            })
    
    @measure_async_time
    async def _benchmark_memory_allocation(self):
        """Benchmark memory allocation patterns."""
        self.logger.info("Benchmarking memory allocation")
        
        # Test different allocation patterns
        allocation_tests = [
            ("small_objects", lambda: [{'id': i, 'data': f'item_{i}'} for i in range(100000)]),
            ("large_objects", lambda: [{'id': i, 'data': 'x' * 10000} for i in range(1000)]),
            ("nested_structures", lambda: [{'id': i, 'nested': {'deep': {'data': f'item_{i}' * 100}}} for i in range(10000)])
        ]
        
        for test_name, allocator in allocation_tests:
            start_time = time.time()
            
            # Allocate memory
            allocated_data = allocator()
            
            # Use the data to prevent optimization
            total_items = len(allocated_data)
            
            duration = (time.time() - start_time) * 1000
            self.metrics.record_operation(f"memory_allocation_{test_name}", duration, True, {
                "items_allocated": total_items
            })
            
            # Clean up
            del allocated_data
    
    @measure_async_time
    async def _benchmark_garbage_collection(self):
        """Benchmark garbage collection impact."""
        self.logger.info("Benchmarking garbage collection")
        
        import gc
        
        # Create objects and measure GC impact
        for i in range(10):
            start_time = time.time()
            
            # Create many objects
            temp_objects = []
            for j in range(10000):
                obj = {
                    'id': j,
                    'references': [],
                    'data': f'object_{j}' * 100
                }
                temp_objects.append(obj)
            
            # Create circular references
            for j in range(len(temp_objects) - 1):
                temp_objects[j]['references'].append(temp_objects[j + 1])
            
            # Force garbage collection
            gc.collect()
            
            duration = (time.time() - start_time) * 1000
            self.metrics.record_operation("gc_impact", duration, True, {
                "objects_created": len(temp_objects),
                "gc_cycle": i
            })
            
            # Clean up
            del temp_objects


# Export benchmark classes
__all__ = [
    'DatabaseBenchmark',
    'DebateAPIBenchmark', 
    'LLMIntegrationBenchmark',
    'MemoryIntensiveBenchmark'
]