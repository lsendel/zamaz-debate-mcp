#!/usr/bin/env python3
"""
Start both the MCP service and WebSocket API server
"""
import os
import sys
import asyncio
import multiprocessing
import signal
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

def run_mcp_service():
    """Run the MCP service"""
    from src.mcp_server import main
    main()

def run_websocket_server():
    """Run the WebSocket API server"""
    from src.api_server import start_api_server
    start_api_server()

def signal_handler(signum, frame):
    """Handle shutdown signals"""
    print("\nShutting down services...")
    sys.exit(0)

def main():
    """Start both services in separate processes"""
    # Setup signal handlers
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    # Create processes
    mcp_process = multiprocessing.Process(target=run_mcp_service)
    ws_process = multiprocessing.Process(target=run_websocket_server)
    
    # Start processes
    print("Starting MCP Debate Service...")
    mcp_process.start()
    
    print("Starting WebSocket API Server...")
    ws_process.start()
    
    try:
        # Wait for processes
        mcp_process.join()
        ws_process.join()
    except KeyboardInterrupt:
        print("\nShutting down...")
        mcp_process.terminate()
        ws_process.terminate()
        mcp_process.join()
        ws_process.join()

if __name__ == "__main__":
    main()