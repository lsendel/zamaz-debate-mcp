"""
Memory and resource monitoring framework.

This module provides comprehensive system resource monitoring including:
- Real-time memory usage tracking
- CPU utilization monitoring
- Disk I/O monitoring
- Network I/O monitoring
- Memory leak detection
- Resource usage alerting
- Performance profiling integration
"""

import json
import logging
import os
import threading
import time
from collections import deque
from collections.abc import Callable
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from threading import Event, Lock
from typing import Any

import matplotlib.pyplot as plt
import numpy as np
import psutil
from prometheus_client import CollectorRegistry, Counter, Gauge, generate_latest


@dataclass
class ResourceThresholds:
    """Resource usage thresholds for alerting."""

    # Memory thresholds
    memory_warning_mb: float = 512.0
    memory_critical_mb: float = 1024.0
    memory_growth_rate_mb_per_min: float = 10.0

    # CPU thresholds
    cpu_warning_percent: float = 70.0
    cpu_critical_percent: float = 90.0
    cpu_sustained_duration_seconds: float = 60.0

    # Disk I/O thresholds
    disk_read_warning_mb_per_sec: float = 100.0
    disk_write_warning_mb_per_sec: float = 100.0

    # Network I/O thresholds
    network_warning_mb_per_sec: float = 10.0

    # Process thresholds
    max_open_files: int = 1000
    max_threads: int = 100

    # Custom thresholds
    custom_thresholds: dict[str, float] = field(default_factory=dict)


@dataclass
class ResourceSample:
    """Single resource usage sample."""

    timestamp: float

    # Memory metrics
    memory_rss_mb: float
    memory_vms_mb: float
    memory_percent: float
    memory_available_mb: float

    # CPU metrics
    cpu_percent: float
    cpu_count: int
    load_average: tuple[float, float, float]

    # Disk I/O metrics
    disk_read_bytes: int
    disk_write_bytes: int
    disk_read_ops: int
    disk_write_ops: int

    # Network I/O metrics
    network_bytes_sent: int
    network_bytes_recv: int
    network_packets_sent: int
    network_packets_recv: int

    # Process metrics
    open_files: int
    num_threads: int
    num_fds: int

    # Custom metrics
    custom_metrics: dict[str, Any] = field(default_factory=dict)


class ResourceMonitor:
    """Real-time resource monitoring with alerting."""

    def __init__(self, thresholds: ResourceThresholds = None, sample_interval: float = 1.0, history_size: int = 3600):
        self.thresholds = thresholds or ResourceThresholds()
        self.sample_interval = sample_interval
        self.history_size = history_size

        self.logger = logging.getLogger(self.__class__.__name__)

        # Monitoring state
        self.is_monitoring = False
        self.monitor_thread = None
        self.stop_event = Event()

        # Data storage
        self.samples = deque(maxlen=history_size)
        self.samples_lock = Lock()

        # Alert handling
        self.alert_callbacks: list[Callable] = []
        self.alert_history = deque(maxlen=1000)

        # Process reference
        self.process = psutil.Process()

        # Prometheus metrics
        self.registry = CollectorRegistry()
        self._setup_prometheus_metrics()

        # Performance profiling
        self.profiling_enabled = False
        self.profiling_data = {}

    def _setup_prometheus_metrics(self):
        """Setup Prometheus metrics."""
        self.memory_gauge = Gauge("system_memory_usage_mb", "Memory usage in MB", ["type"], registry=self.registry)

        self.cpu_gauge = Gauge("system_cpu_usage_percent", "CPU usage percentage", registry=self.registry)

        self.disk_io_counter = Counter(
            "system_disk_io_bytes_total", "Total disk I/O bytes", ["direction"], registry=self.registry
        )

        self.network_io_counter = Counter(
            "system_network_io_bytes_total", "Total network I/O bytes", ["direction"], registry=self.registry
        )

        self.alert_counter = Counter(
            "resource_alerts_total", "Total resource alerts", ["type", "level"], registry=self.registry
        )

    def start_monitoring(self):
        """Start resource monitoring."""
        if self.is_monitoring:
            self.logger.warning("Monitoring already started")
            return

        self.logger.info("Starting resource monitoring")
        self.is_monitoring = True
        self.stop_event.clear()

        self.monitor_thread = threading.Thread(target=self._monitoring_loop)
        self.monitor_thread.daemon = True
        self.monitor_thread.start()

    def stop_monitoring(self):
        """Stop resource monitoring."""
        if not self.is_monitoring:
            return

        self.logger.info("Stopping resource monitoring")
        self.is_monitoring = False
        self.stop_event.set()

        if self.monitor_thread:
            self.monitor_thread.join(timeout=5.0)

    def add_alert_callback(self, callback: Callable[[str, str, dict[str, Any]], None]):
        """Add alert callback function."""
        self.alert_callbacks.append(callback)

    def get_current_sample(self) -> ResourceSample:
        """Get current resource usage sample."""
        return self._collect_sample()

    def get_recent_samples(self, duration_seconds: int = 60) -> list[ResourceSample]:
        """Get recent samples within specified duration."""
        cutoff_time = time.time() - duration_seconds

        with self.samples_lock:
            return [s for s in self.samples if s.timestamp >= cutoff_time]

    def get_resource_summary(self) -> dict[str, Any]:
        """Get resource usage summary."""
        with self.samples_lock:
            if not self.samples:
                return {}

            # Calculate statistics
            memory_values = [s.memory_rss_mb for s in self.samples]
            cpu_values = [s.cpu_percent for s in self.samples]

            return {
                "current_memory_mb": self.samples[-1].memory_rss_mb,
                "peak_memory_mb": max(memory_values),
                "avg_memory_mb": np.mean(memory_values),
                "current_cpu_percent": self.samples[-1].cpu_percent,
                "peak_cpu_percent": max(cpu_values),
                "avg_cpu_percent": np.mean(cpu_values),
                "sample_count": len(self.samples),
                "monitoring_duration": self.samples[-1].timestamp - self.samples[0].timestamp
                if len(self.samples) > 1
                else 0,
            }

    def detect_memory_leaks(self, window_minutes: int = 10) -> dict[str, Any]:
        """Detect potential memory leaks."""
        window_seconds = window_minutes * 60
        recent_samples = self.get_recent_samples(window_seconds)

        if len(recent_samples) < 10:
            return {"insufficient_data": True}

        # Calculate memory growth rate
        times = [s.timestamp for s in recent_samples]
        memory_values = [s.memory_rss_mb for s in recent_samples]

        # Linear regression to detect trend
        x = np.array(times)
        y = np.array(memory_values)

        # Normalize x to minutes
        x_norm = (x - x[0]) / 60.0

        # Calculate slope (MB per minute)
        slope = np.polyfit(x_norm, y, 1)[0]

        # Calculate correlation coefficient
        correlation = np.corrcoef(x_norm, y)[0, 1]

        # Memory leak indicators
        leak_detected = (
            slope > self.thresholds.memory_growth_rate_mb_per_min and correlation > 0.7  # Strong positive correlation
        )

        return {
            "leak_detected": leak_detected,
            "growth_rate_mb_per_min": slope,
            "correlation": correlation,
            "window_minutes": window_minutes,
            "current_memory_mb": recent_samples[-1].memory_rss_mb,
            "initial_memory_mb": recent_samples[0].memory_rss_mb,
            "total_growth_mb": recent_samples[-1].memory_rss_mb - recent_samples[0].memory_rss_mb,
        }

    def analyze_cpu_patterns(self, window_minutes: int = 5) -> dict[str, Any]:
        """Analyze CPU usage patterns."""
        window_seconds = window_minutes * 60
        recent_samples = self.get_recent_samples(window_seconds)

        if len(recent_samples) < 5:
            return {"insufficient_data": True}

        cpu_values = [s.cpu_percent for s in recent_samples]

        # Calculate CPU statistics
        avg_cpu = np.mean(cpu_values)
        max_cpu = np.max(cpu_values)
        min_cpu = np.min(cpu_values)
        std_cpu = np.std(cpu_values)

        # Detect sustained high CPU usage
        high_cpu_samples = [v for v in cpu_values if v > self.thresholds.cpu_warning_percent]
        sustained_high_cpu = len(high_cpu_samples) / len(cpu_values) > 0.8

        # Detect CPU spikes
        cpu_spikes = [v for v in cpu_values if v > self.thresholds.cpu_critical_percent]
        spike_count = len(cpu_spikes)

        return {
            "avg_cpu_percent": avg_cpu,
            "max_cpu_percent": max_cpu,
            "min_cpu_percent": min_cpu,
            "std_cpu_percent": std_cpu,
            "sustained_high_cpu": sustained_high_cpu,
            "spike_count": spike_count,
            "high_cpu_ratio": len(high_cpu_samples) / len(cpu_values),
        }

    def analyze_io_patterns(self, window_minutes: int = 5) -> dict[str, Any]:
        """Analyze I/O patterns."""
        window_seconds = window_minutes * 60
        recent_samples = self.get_recent_samples(window_seconds)

        if len(recent_samples) < 2:
            return {"insufficient_data": True}

        # Calculate I/O rates
        disk_read_rates = []
        disk_write_rates = []
        network_send_rates = []
        network_recv_rates = []

        for i in range(1, len(recent_samples)):
            prev_sample = recent_samples[i - 1]
            curr_sample = recent_samples[i]
            time_diff = curr_sample.timestamp - prev_sample.timestamp

            if time_diff > 0:
                # Disk I/O rates (MB/s)
                disk_read_rate = (curr_sample.disk_read_bytes - prev_sample.disk_read_bytes) / (1024 * 1024) / time_diff
                disk_write_rate = (
                    (curr_sample.disk_write_bytes - prev_sample.disk_write_bytes) / (1024 * 1024) / time_diff
                )

                # Network I/O rates (MB/s)
                network_send_rate = (
                    (curr_sample.network_bytes_sent - prev_sample.network_bytes_sent) / (1024 * 1024) / time_diff
                )
                network_recv_rate = (
                    (curr_sample.network_bytes_recv - prev_sample.network_bytes_recv) / (1024 * 1024) / time_diff
                )

                disk_read_rates.append(max(0, disk_read_rate))
                disk_write_rates.append(max(0, disk_write_rate))
                network_send_rates.append(max(0, network_send_rate))
                network_recv_rates.append(max(0, network_recv_rate))

        return {
            "disk_read_mb_per_sec": {
                "avg": np.mean(disk_read_rates),
                "max": np.max(disk_read_rates),
                "total": np.sum(disk_read_rates),
            },
            "disk_write_mb_per_sec": {
                "avg": np.mean(disk_write_rates),
                "max": np.max(disk_write_rates),
                "total": np.sum(disk_write_rates),
            },
            "network_send_mb_per_sec": {
                "avg": np.mean(network_send_rates),
                "max": np.max(network_send_rates),
                "total": np.sum(network_send_rates),
            },
            "network_recv_mb_per_sec": {
                "avg": np.mean(network_recv_rates),
                "max": np.max(network_recv_rates),
                "total": np.sum(network_recv_rates),
            },
        }

    def export_prometheus_metrics(self) -> str:
        """Export metrics in Prometheus format."""
        return generate_latest(self.registry).decode("utf-8")

    def save_monitoring_data(self, filepath: str):
        """Save monitoring data to file."""
        with self.samples_lock:
            data = {
                "timestamp": datetime.now().isoformat(),
                "samples": [
                    {
                        "timestamp": s.timestamp,
                        "memory_rss_mb": s.memory_rss_mb,
                        "cpu_percent": s.cpu_percent,
                        "open_files": s.open_files,
                        "num_threads": s.num_threads,
                    }
                    for s in self.samples
                ],
                "summary": self.get_resource_summary(),
            }

        with filepath.open("w") as f:
            json.dump(data, f, indent=2)

    def generate_monitoring_report(self, output_dir: str):
        """Generate comprehensive monitoring report."""
        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)

        # Generate charts
        self._generate_monitoring_charts(output_path)

        # Generate summary
        summary = self.get_resource_summary()
        leak_analysis = self.detect_memory_leaks()
        cpu_analysis = self.analyze_cpu_patterns()
        io_analysis = self.analyze_io_patterns()

        report = {
            "generated_at": datetime.now().isoformat(),
            "summary": summary,
            "memory_leak_analysis": leak_analysis,
            "cpu_analysis": cpu_analysis,
            "io_analysis": io_analysis,
            "alert_history": list(self.alert_history),
        }

        # Save report
        with (output_path / "monitoring_report.json").open("w") as f:
            json.dump(report, f, indent=2)

        self.logger.info(f"Monitoring report generated: {output_path}")

    def _monitoring_loop(self):
        """Main monitoring loop."""
        self.logger.info("Resource monitoring loop started")

        while not self.stop_event.is_set():
            try:
                # Collect sample
                sample = self._collect_sample()

                # Store sample
                with self.samples_lock:
                    self.samples.append(sample)

                # Update Prometheus metrics
                self._update_prometheus_metrics(sample)

                # Check thresholds
                self._check_thresholds(sample)

                # Wait for next sample
                self.stop_event.wait(self.sample_interval)

            except Exception as e:
                self.logger.error(f"Error in monitoring loop: {e}")
                time.sleep(self.sample_interval)

        self.logger.info("Resource monitoring loop stopped")

    def _collect_sample(self) -> ResourceSample:
        """Collect current resource usage sample."""
        # System memory
        memory_info = psutil.virtual_memory()

        # Process memory
        process_memory = self.process.memory_info()

        # CPU usage
        cpu_percent = self.process.cpu_percent()

        # Load average
        load_avg = os.getloadavg() if hasattr(os, "getloadavg") else (0.0, 0.0, 0.0)

        # Disk I/O
        disk_io = psutil.disk_io_counters()

        # Network I/O
        network_io = psutil.net_io_counters()

        # Process info
        try:
            open_files = len(self.process.open_files())
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            open_files = 0

        try:
            num_threads = self.process.num_threads()
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            num_threads = 0

        try:
            num_fds = self.process.num_fds() if hasattr(self.process, "num_fds") else 0
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            num_fds = 0

        return ResourceSample(
            timestamp=time.time(),
            memory_rss_mb=process_memory.rss / (1024 * 1024),
            memory_vms_mb=process_memory.vms / (1024 * 1024),
            memory_percent=memory_info.percent,
            memory_available_mb=memory_info.available / (1024 * 1024),
            cpu_percent=cpu_percent,
            cpu_count=psutil.cpu_count(),
            load_average=load_avg,
            disk_read_bytes=disk_io.read_bytes if disk_io else 0,
            disk_write_bytes=disk_io.write_bytes if disk_io else 0,
            disk_read_ops=disk_io.read_count if disk_io else 0,
            disk_write_ops=disk_io.write_count if disk_io else 0,
            network_bytes_sent=network_io.bytes_sent if network_io else 0,
            network_bytes_recv=network_io.bytes_recv if network_io else 0,
            network_packets_sent=network_io.packets_sent if network_io else 0,
            network_packets_recv=network_io.packets_recv if network_io else 0,
            open_files=open_files,
            num_threads=num_threads,
            num_fds=num_fds,
        )

    def _update_prometheus_metrics(self, sample: ResourceSample):
        """Update Prometheus metrics."""
        self.memory_gauge.labels(type="rss").set(sample.memory_rss_mb)
        self.memory_gauge.labels(type="vms").set(sample.memory_vms_mb)
        self.memory_gauge.labels(type="available").set(sample.memory_available_mb)

        self.cpu_gauge.set(sample.cpu_percent)

        # Note: Counters should only increase, so we track deltas
        # This is a simplified implementation
        self.disk_io_counter.labels(direction="read")._value._value = sample.disk_read_bytes
        self.disk_io_counter.labels(direction="write")._value._value = sample.disk_write_bytes

        self.network_io_counter.labels(direction="sent")._value._value = sample.network_bytes_sent
        self.network_io_counter.labels(direction="recv")._value._value = sample.network_bytes_recv

    def _check_thresholds(self, sample: ResourceSample):
        """Check resource thresholds and generate alerts."""
        # Memory thresholds
        if sample.memory_rss_mb > self.thresholds.memory_critical_mb:
            self._trigger_alert(
                "memory",
                "critical",
                {"current_mb": sample.memory_rss_mb, "threshold_mb": self.thresholds.memory_critical_mb},
            )
        elif sample.memory_rss_mb > self.thresholds.memory_warning_mb:
            self._trigger_alert(
                "memory",
                "warning",
                {"current_mb": sample.memory_rss_mb, "threshold_mb": self.thresholds.memory_warning_mb},
            )

        # CPU thresholds
        if sample.cpu_percent > self.thresholds.cpu_critical_percent:
            self._trigger_alert(
                "cpu",
                "critical",
                {"current_percent": sample.cpu_percent, "threshold_percent": self.thresholds.cpu_critical_percent},
            )
        elif sample.cpu_percent > self.thresholds.cpu_warning_percent:
            self._trigger_alert(
                "cpu",
                "warning",
                {"current_percent": sample.cpu_percent, "threshold_percent": self.thresholds.cpu_warning_percent},
            )

        # File descriptor thresholds
        if sample.open_files > self.thresholds.max_open_files:
            self._trigger_alert(
                "open_files",
                "warning",
                {"current_count": sample.open_files, "threshold_count": self.thresholds.max_open_files},
            )

        # Thread count thresholds
        if sample.num_threads > self.thresholds.max_threads:
            self._trigger_alert(
                "threads",
                "warning",
                {"current_count": sample.num_threads, "threshold_count": self.thresholds.max_threads},
            )

    def _trigger_alert(self, alert_type: str, level: str, data: dict[str, Any]):
        """Trigger an alert."""
        alert = {"timestamp": time.time(), "type": alert_type, "level": level, "data": data}

        # Store alert
        self.alert_history.append(alert)

        # Update Prometheus counter
        self.alert_counter.labels(type=alert_type, level=level).inc()

        # Call alert callbacks
        for callback in self.alert_callbacks:
            try:
                callback(alert_type, level, data)
            except Exception as e:
                self.logger.error(f"Error in alert callback: {e}")

        # Log alert
        self.logger.warning(f"Resource alert: {alert_type} {level} - {data}")

    def _generate_monitoring_charts(self, output_dir: Path):
        """Generate monitoring charts."""
        with self.samples_lock:
            if len(self.samples) < 2:
                return

            # Prepare data
            timestamps = [datetime.fromtimestamp(s.timestamp) for s in self.samples]
            memory_values = [s.memory_rss_mb for s in self.samples]
            cpu_values = [s.cpu_percent for s in self.samples]

            # Memory usage chart
            plt.figure(figsize=(12, 6))
            plt.plot(timestamps, memory_values, linewidth=2, label="Memory Usage")
            plt.axhline(y=self.thresholds.memory_warning_mb, color="orange", linestyle="--", label="Warning Threshold")
            plt.axhline(y=self.thresholds.memory_critical_mb, color="red", linestyle="--", label="Critical Threshold")
            plt.xlabel("Time")
            plt.ylabel("Memory Usage (MB)")
            plt.title("Memory Usage Over Time")
            plt.legend()
            plt.grid(True, alpha=0.3)
            plt.xticks(rotation=45)
            plt.tight_layout()
            plt.savefig(output_dir / "memory_usage.png", dpi=300)
            plt.close()

            # CPU usage chart
            plt.figure(figsize=(12, 6))
            plt.plot(timestamps, cpu_values, linewidth=2, label="CPU Usage", color="green")
            plt.axhline(
                y=self.thresholds.cpu_warning_percent, color="orange", linestyle="--", label="Warning Threshold"
            )
            plt.axhline(y=self.thresholds.cpu_critical_percent, color="red", linestyle="--", label="Critical Threshold")
            plt.xlabel("Time")
            plt.ylabel("CPU Usage (%)")
            plt.title("CPU Usage Over Time")
            plt.legend()
            plt.grid(True, alpha=0.3)
            plt.xticks(rotation=45)
            plt.tight_layout()
            plt.savefig(output_dir / "cpu_usage.png", dpi=300)
            plt.close()


class MemoryProfiler:
    """Memory profiling utilities."""

    def __init__(self):
        self.logger = logging.getLogger(self.__class__.__name__)
        self.snapshots = []

    def take_snapshot(self, name: str | None = None) -> dict[str, Any]:
        """Take memory snapshot."""
        import tracemalloc

        if not tracemalloc.is_tracing():
            tracemalloc.start()

        snapshot = tracemalloc.take_snapshot()
        top_stats = snapshot.statistics("lineno")

        snapshot_data = {
            "name": name or f"snapshot_{len(self.snapshots)}",
            "timestamp": time.time(),
            "total_memory_mb": sum(stat.size for stat in top_stats) / (1024 * 1024),
            "top_allocations": [
                {"filename": stat.traceback.format()[0], "size_mb": stat.size / (1024 * 1024), "count": stat.count}
                for stat in top_stats[:10]
            ],
        }

        self.snapshots.append(snapshot_data)
        return snapshot_data

    def compare_snapshots(self, snapshot1_name: str, snapshot2_name: str) -> dict[str, Any]:
        """Compare two memory snapshots."""
        snapshot1 = next((s for s in self.snapshots if s["name"] == snapshot1_name), None)
        snapshot2 = next((s for s in self.snapshots if s["name"] == snapshot2_name), None)

        if not snapshot1 or not snapshot2:
            return {"error": "Snapshots not found"}

        memory_diff = snapshot2["total_memory_mb"] - snapshot1["total_memory_mb"]

        return {
            "snapshot1": snapshot1_name,
            "snapshot2": snapshot2_name,
            "memory_diff_mb": memory_diff,
            "time_diff_seconds": snapshot2["timestamp"] - snapshot1["timestamp"],
            "growth_rate_mb_per_sec": memory_diff / (snapshot2["timestamp"] - snapshot1["timestamp"])
            if snapshot2["timestamp"] != snapshot1["timestamp"]
            else 0,
        }


# Export main classes
__all__ = ["MemoryProfiler", "ResourceMonitor", "ResourceSample", "ResourceThresholds"]
