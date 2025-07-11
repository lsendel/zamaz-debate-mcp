"""
Monitoring and metrics collection for MCP services
"""
from typing import Dict, Any, List
import asyncio
import time
from datetime import datetime, timedelta
import json
import logging

from .concurrency import concurrency_metrics, request_queue, connection_pool

logger = logging.getLogger(__name__)


class PerformanceMonitor:
    """Monitor system performance and concurrency metrics"""
    
    def __init__(self):
        self.start_time = time.time()
        self.request_history: List[Dict[str, Any]] = []
        self.alert_thresholds = {
            "max_response_time_ms": 5000,
            "max_concurrent_requests": 50,
            "min_success_rate": 0.95,
            "max_queue_size": 100
        }
        self.alerts: List[Dict[str, Any]] = []
    
    async def get_system_metrics(self) -> Dict[str, Any]:
        """Get comprehensive system metrics"""
        metrics = concurrency_metrics.metrics
        
        return {
            "system": {
                "uptime_seconds": time.time() - self.start_time,
                "timestamp": datetime.utcnow().isoformat(),
                "status": "healthy" if await self._check_system_health() else "degraded"
            },
            "requests": metrics,
            "concurrency": {
                "active_requests": request_queue.active_count,
                "max_concurrent": request_queue.max_concurrent,
                "queue_utilization": request_queue.active_count / request_queue.max_concurrent
            },
            "connections": {
                "active_connections": connection_pool.connection_count,
                "max_connections": connection_pool.max_connections,
                "pool_utilization": connection_pool.connection_count / connection_pool.max_connections,
                "is_full": connection_pool.is_full
            },
            "alerts": self.alerts[-10:],  # Last 10 alerts
            "thresholds": self.alert_thresholds
        }
    
    async def _check_system_health(self) -> bool:
        """Check if system is healthy based on metrics"""
        metrics = concurrency_metrics.metrics
        
        # Check response time
        if metrics["avg_response_time_ms"] > self.alert_thresholds["max_response_time_ms"]:
            await self._create_alert("HIGH_RESPONSE_TIME", f"Average response time: {metrics['avg_response_time_ms']:.1f}ms")
            return False
        
        # Check success rate
        if metrics["success_rate"] < self.alert_thresholds["min_success_rate"]:
            await self._create_alert("LOW_SUCCESS_RATE", f"Success rate: {metrics['success_rate']:.1%}")
            return False
        
        # Check concurrent requests
        if request_queue.active_count > self.alert_thresholds["max_concurrent_requests"]:
            await self._create_alert("HIGH_CONCURRENCY", f"Active requests: {request_queue.active_count}")
            return False
        
        # Check connection pool
        if connection_pool.is_full:
            await self._create_alert("CONNECTION_POOL_FULL", "Connection pool at capacity")
            return False
        
        return True
    
    async def _create_alert(self, alert_type: str, message: str):
        """Create a system alert"""
        alert = {
            "type": alert_type,
            "message": message,
            "timestamp": datetime.utcnow().isoformat(),
            "severity": "warning"
        }
        
        self.alerts.append(alert)
        logger.warning(f"System alert: {alert_type} - {message}")
        
        # Keep only last 100 alerts
        if len(self.alerts) > 100:
            self.alerts = self.alerts[-100:]
    
    async def record_request_details(self, request_type: str, org_id: str, 
                                   response_time: float, success: bool, 
                                   metadata: Dict[str, Any] = None):
        """Record detailed request information"""
        record = {
            "timestamp": datetime.utcnow().isoformat(),
            "request_type": request_type,
            "org_id": org_id,
            "response_time_ms": response_time * 1000,
            "success": success,
            "metadata": metadata or {}
        }
        
        self.request_history.append(record)
        
        # Keep only last 1000 requests
        if len(self.request_history) > 1000:
            self.request_history = self.request_history[-1000:]
    
    async def get_organization_metrics(self, org_id: str) -> Dict[str, Any]:
        """Get metrics specific to an organization"""
        org_requests = [r for r in self.request_history if r["org_id"] == org_id]
        
        if not org_requests:
            return {"org_id": org_id, "no_data": True}
        
        total_requests = len(org_requests)
        successful_requests = len([r for r in org_requests if r["success"]])
        avg_response_time = sum(r["response_time_ms"] for r in org_requests) / total_requests
        
        # Request types breakdown
        request_types = {}
        for req in org_requests:
            req_type = req["request_type"]
            if req_type not in request_types:
                request_types[req_type] = {"count": 0, "success": 0}
            request_types[req_type]["count"] += 1
            if req["success"]:
                request_types[req_type]["success"] += 1
        
        return {
            "org_id": org_id,
            "total_requests": total_requests,
            "success_rate": successful_requests / total_requests if total_requests > 0 else 0,
            "avg_response_time_ms": avg_response_time,
            "request_types": request_types,
            "recent_activity": org_requests[-10:]  # Last 10 requests
        }
    
    async def get_concurrency_report(self) -> Dict[str, Any]:
        """Generate detailed concurrency report"""
        return {
            "current_state": {
                "active_requests": request_queue.active_count,
                "connection_pool_usage": f"{connection_pool.connection_count}/{connection_pool.max_connections}",
                "queue_capacity": f"{request_queue.active_count}/{request_queue.max_concurrent}"
            },
            "limits": {
                "max_concurrent_requests": request_queue.max_concurrent,
                "max_connections": connection_pool.max_connections,
                "rate_limits": "100 req/min per org"
            },
            "performance": await concurrency_metrics.metrics,
            "recommendations": await self._get_scaling_recommendations()
        }
    
    async def _get_scaling_recommendations(self) -> List[str]:
        """Provide scaling recommendations based on metrics"""
        recommendations = []
        
        utilization = request_queue.active_count / request_queue.max_concurrent
        
        if utilization > 0.8:
            recommendations.append("Consider increasing max_concurrent_requests")
        
        if connection_pool.connection_count / connection_pool.max_connections > 0.8:
            recommendations.append("Consider increasing connection pool size")
        
        metrics = concurrency_metrics.metrics
        if metrics["avg_response_time_ms"] > 2000:
            recommendations.append("High response times - consider optimizing or scaling")
        
        if metrics["success_rate"] < 0.98:
            recommendations.append("Low success rate - investigate error causes")
        
        if not recommendations:
            recommendations.append("System performing well within normal parameters")
        
        return recommendations


# Global monitor instance
performance_monitor = PerformanceMonitor()


async def get_health_check() -> Dict[str, Any]:
    """Get basic health check response"""
    try:
        metrics = await performance_monitor.get_system_metrics()
        return {
            "status": "healthy",
            "timestamp": datetime.utcnow().isoformat(),
            "uptime": metrics["system"]["uptime_seconds"],
            "active_requests": metrics["concurrency"]["active_requests"],
            "success_rate": metrics["requests"]["success_rate"]
        }
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return {
            "status": "unhealthy",
            "error": str(e),
            "timestamp": datetime.utcnow().isoformat()
        }


async def log_request_metrics(request_type: str, org_id: str, 
                            response_time: float, success: bool,
                            metadata: Dict[str, Any] = None):
    """Log request metrics for monitoring"""
    await performance_monitor.record_request_details(
        request_type, org_id, response_time, success, metadata
    )
    await concurrency_metrics.record_request(success, response_time)