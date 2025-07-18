#!/bin/bash

# Stop all MCP services
echo "🛑 Stopping all MCP services..."

if [ -f mcp-services.pids ]; then
    PIDS=$(cat mcp-services.pids)
    echo "📝 Found PIDs: """$PIDS""""
    
    for pid in """$PIDS"""; do
        if kill -0 """$pid""" 2>/dev/null; then
            echo "🔄 Stopping process """$pid"""..."
            kill -TERM $pid
        fi
    done
    
    # Wait a moment for graceful shutdown
    sleep 5
    
    # Force kill if necessary
    for pid in """$PIDS"""; do
        if kill -0 """$pid""" 2>/dev/null; then
            echo "💀 Force killing process """$pid"""..."
            kill -KILL $pid
        fi
    done
    
    rm -f mcp-services.pids
else
    echo "⚠️  No PID file found. Attempting to kill by process name..."
    pkill -f "mcp-organization"
    pkill -f "mcp-context"
    pkill -f "mcp-llm"
    pkill -f "mcp-controller"
    pkill -f "mcp-rag"
fi

echo "✅ All MCP services stopped."