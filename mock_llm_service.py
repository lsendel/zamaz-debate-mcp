#!/usr/bin/env python3
"""
Mock LLM Service for testing debate functionality
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
from datetime import datetime
import uuid

app = FastAPI(title="Mock LLM Service")

# Enable CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mock providers data
MOCK_PROVIDERS = {
    "providers": [
        {
            "id": "claude",
            "name": "Claude",
            "models": [
                {"id": "claude-3-haiku", "name": "Claude 3 Haiku"},
                {"id": "claude-3-sonnet", "name": "Claude 3 Sonnet"},
                {"id": "claude-3-opus", "name": "Claude 3 Opus"}
            ],
            "status": "active"
        },
        {
            "id": "openai",
            "name": "OpenAI",
            "models": [
                {"id": "gpt-3.5-turbo", "name": "GPT-3.5 Turbo"},
                {"id": "gpt-4", "name": "GPT-4"}
            ],
            "status": "active"
        }
    ]
}

@app.get("/providers")
async def get_providers():
    """Return mock LLM providers"""
    return MOCK_PROVIDERS

@app.get("/health")
async def health():
    """Health check endpoint"""
    return {"status": "healthy", "service": "mock-llm-service"}

@app.post("/completions")
async def create_completion(request: dict):
    """Mock completion endpoint"""
    # Extract the prompt from the messages
    prompt = ""
    if "messages" in request:
        for msg in request["messages"]:
            if msg.get("role") == "user":
                prompt = msg.get("content", "")
                break
    
    # Generate a mock response based on the prompt
    if "debate" in prompt.lower():
        response_text = f"As a participant in this debate about '{prompt}', I believe we should consider multiple perspectives..."
    else:
        response_text = f"This is a mock response to: {prompt}"
    
    return {
        "id": f"completion-{uuid.uuid4()}",
        "object": "text_completion",
        "created": int(datetime.now().timestamp()),
        "model": request.get("model", "mock-model"),
        "choices": [
            {
                "text": response_text,
                "index": 0,
                "finish_reason": "stop"
            }
        ],
        "usage": {
            "prompt_tokens": len(prompt.split()),
            "completion_tokens": len(response_text.split()),
            "total_tokens": len(prompt.split()) + len(response_text.split())
        }
    }

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=5002)