"""
FastAPI server wrapper for LLM MCP service
"""
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from typing import List, Dict, Any
import os
from datetime import datetime
from dotenv import load_dotenv
import uvicorn

from .models import LLMProvider
from .providers.provider_factory import ProviderFactory

# Load environment variables
load_dotenv()

app = FastAPI(
    title="LLM Service API",
    description="API gateway for LLM providers",
    version="1.0.0"
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    providers_status = {}
    
    # Check Claude
    if os.getenv("ANTHROPIC_API_KEY"):
        providers_status["claude"] = {
            "available": True,
            "models": ["claude-3-5-sonnet-20241022", "claude-3-opus-20240229", "claude-3-haiku-20240307"]
        }
    else:
        providers_status["claude"] = {
            "available": False,
            "error": "Missing ANTHROPIC_API_KEY",
            "models": []
        }
    
    # Check OpenAI
    if os.getenv("OPENAI_API_KEY"):
        providers_status["openai"] = {
            "available": True,
            "models": ["gpt-4o", "gpt-4-turbo-preview", "gpt-3.5-turbo"]
        }
    else:
        providers_status["openai"] = {
            "available": False,
            "error": "Missing OPENAI_API_KEY",
            "models": []
        }
    
    # Check Gemini
    if os.getenv("GOOGLE_API_KEY"):
        providers_status["gemini"] = {
            "available": True,
            "models": ["gemini-2.0-flash-exp", "gemini-1.5-pro"]
        }
    else:
        providers_status["gemini"] = {
            "available": False,
            "error": "Missing GOOGLE_API_KEY",
            "models": []
        }
    
    # Ollama is always available
    providers_status["llama"] = {
        "available": True,
        "models": ["mistral", "mixtral"]
    }
    
    # Determine overall status
    any_available = any(p["available"] for p in providers_status.values())
    status = "healthy" if any_available else "unhealthy"
    
    return {
        "status": status,
        "providers": providers_status,
        "timestamp": datetime.utcnow().isoformat()
    }

@app.get("/providers")
async def get_providers():
    """Get available LLM providers and their models"""
    providers = []
    
    # Check which providers are available based on environment variables
    provider_configs = {
        "claude": {
            "name": "Claude (Anthropic)",
            "enabled": bool(os.getenv("ANTHROPIC_API_KEY")),
            "models": [
                {"id": "claude-3.5-sonnet", "name": "Claude 3.5 Sonnet"},
                {"id": "claude-3-opus", "name": "Claude 3 Opus"},
                {"id": "claude-3-haiku", "name": "Claude 3 Haiku"}
            ]
        },
        "openai": {
            "name": "OpenAI",
            "enabled": bool(os.getenv("OPENAI_API_KEY")),
            "models": [
                {"id": "gpt-4o", "name": "GPT-4o"},
                {"id": "gpt-4-turbo-preview", "name": "GPT-4 Turbo"},
                {"id": "gpt-3.5-turbo", "name": "GPT-3.5 Turbo"}
            ]
        },
        "gemini": {
            "name": "Google Gemini",
            "enabled": bool(os.getenv("GOOGLE_API_KEY")),
            "models": [
                {"id": "gemini-2.0-flash-exp", "name": "Gemini 2.0 Flash"},
                {"id": "gemini-1.5-pro", "name": "Gemini 1.5 Pro"},
                {"id": "gemini-1.5-flash", "name": "Gemini 1.5 Flash"}
            ]
        },
        "llama": {
            "name": "Llama (Ollama)",
            "enabled": True,  # Always enabled for local Ollama
            "models": [
                {"id": "llama3.2", "name": "Llama 3.2"},
                {"id": "llama3.1", "name": "Llama 3.1"},
                {"id": "llama2", "name": "Llama 2"},
                {"id": "mistral", "name": "Mistral"},
                {"id": "mixtral", "name": "Mixtral"}
            ]
        }
    }
    
    for provider_id, config in provider_configs.items():
        if config["enabled"]:
            providers.append({
                "id": provider_id,
                "name": config["name"],
                "models": config["models"],
                "status": "active"
            })
        else:
            providers.append({
                "id": provider_id,
                "name": config["name"],
                "models": config["models"],
                "status": "disabled",
                "message": f"Missing API key for {config['name']}"
            })
    
    return {"providers": providers}

@app.post("/completions")
async def create_completion(request: Dict[str, Any]):
    """Create a completion using specified provider and model"""
    try:
        provider_id = request.get("provider", "llama")
        model = request.get("model", "llama3.2")
        messages = request.get("messages", [])
        
        # Convert provider string to enum
        provider_map = {
            "claude": LLMProvider.CLAUDE,
            "openai": LLMProvider.OPENAI,
            "gemini": LLMProvider.GEMINI,
            "llama": LLMProvider.LLAMA
        }
        
        provider_enum = provider_map.get(provider_id, LLMProvider.LLAMA)
        
        # Get provider instance
        provider = ProviderFactory.get_provider(provider_enum)
        
        # Convert messages to provider format
        from .models import Message
        formatted_messages = [
            Message(role=msg["role"], content=msg["content"])
            for msg in messages
        ]
        
        # Generate completion
        response = await provider.complete(
            messages=formatted_messages,
            model=model,
            max_tokens=request.get("max_tokens", 1000),
            temperature=request.get("temperature", 0.7)
        )
        
        # Format response for UI
        return {
            "id": f"completion-{response.provider.value}-{model}",
            "object": "text_completion",
            "created": 1234567890,
            "model": model,
            "choices": [
                {
                    "text": response.content,
                    "index": 0,
                    "finish_reason": response.finish_reason or "stop"
                }
            ],
            "usage": response.usage
        }
        
    except ValueError as e:
        raise HTTPException(status_code=400, detail=f"Invalid request: {str(e)}")
    except KeyError as e:
        raise HTTPException(status_code=400, detail=f"Missing required field: {str(e)}")
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")

@app.get("/models")
async def get_all_models():
    """Get all available models from all providers"""
    models = []
    
    # Check Claude models
    if os.getenv("ANTHROPIC_API_KEY"):
        models.extend([
            {
                "id": "claude-3-5-sonnet-20241022",
                "name": "Claude 3.5 Sonnet",
                "provider": "claude",
                "capabilities": ["text", "vision", "analysis"],
                "contextWindow": 200000,
                "costPer1kTokens": {"input": 0.003, "output": 0.015},
                "type": "api"
            },
            {
                "id": "claude-3-opus-20240229",
                "name": "Claude 3 Opus",
                "provider": "claude",
                "capabilities": ["text", "vision", "analysis"],
                "contextWindow": 200000,
                "costPer1kTokens": {"input": 0.015, "output": 0.075},
                "type": "api"
            },
            {
                "id": "claude-3-haiku-20240307",
                "name": "Claude 3 Haiku",
                "provider": "claude",
                "capabilities": ["text", "vision"],
                "contextWindow": 200000,
                "costPer1kTokens": {"input": 0.00025, "output": 0.00125},
                "type": "api"
            }
        ])
    
    # Check OpenAI models
    if os.getenv("OPENAI_API_KEY"):
        models.extend([
            {
                "id": "gpt-4o",
                "name": "GPT-4o",
                "provider": "openai",
                "capabilities": ["text", "vision", "analysis"],
                "contextWindow": 128000,
                "costPer1kTokens": {"input": 0.005, "output": 0.015},
                "type": "api"
            },
            {
                "id": "gpt-4-turbo-preview",
                "name": "GPT-4 Turbo",
                "provider": "openai",
                "capabilities": ["text", "vision", "analysis"],
                "contextWindow": 128000,
                "costPer1kTokens": {"input": 0.01, "output": 0.03},
                "type": "api"
            },
            {
                "id": "gpt-3.5-turbo",
                "name": "GPT-3.5 Turbo",
                "provider": "openai",
                "capabilities": ["text"],
                "contextWindow": 16385,
                "costPer1kTokens": {"input": 0.0005, "output": 0.0015},
                "type": "api"
            }
        ])
    
    # Check Gemini models
    if os.getenv("GOOGLE_API_KEY"):
        models.extend([
            {
                "id": "gemini-2.0-flash-exp",
                "name": "Gemini 2.0 Flash",
                "provider": "gemini",
                "capabilities": ["text", "vision", "analysis"],
                "contextWindow": 1048576,
                "costPer1kTokens": {"input": 0.00035, "output": 0.0014},
                "type": "api"
            },
            {
                "id": "gemini-1.5-pro",
                "name": "Gemini 1.5 Pro",
                "provider": "gemini",
                "capabilities": ["text", "vision", "analysis"],
                "contextWindow": 2097152,
                "costPer1kTokens": {"input": 0.00125, "output": 0.005},
                "type": "api"
            }
        ])
    
    # Always include local models (Ollama)
    models.extend([
        {
            "id": "mistral",
            "name": "Mistral 7B",
            "provider": "llama",
            "capabilities": ["text"],
            "contextWindow": 8192,
            "type": "local"
        },
        {
            "id": "mixtral",
            "name": "Mixtral 8x7B",
            "provider": "llama",
            "capabilities": ["text"],
            "contextWindow": 32768,
            "type": "local"
        }
    ])
    
    return {"models": models}

@app.get("/models/{provider}")
async def get_provider_models(provider: str):
    """Get models for a specific provider"""
    try:
        provider_map = {
            "claude": LLMProvider.CLAUDE,
            "openai": LLMProvider.OPENAI,
            "gemini": LLMProvider.GEMINI,
            "llama": LLMProvider.LLAMA
        }
        
        provider_enum = provider_map.get(provider)
        if not provider_enum:
            raise HTTPException(status_code=404, detail=f"Unknown provider: {provider}")
        
        provider_instance = ProviderFactory.get_provider(provider_enum)
        models = list(provider_instance.MODELS.keys())
        
        return {"models": models}
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

def start_api_server():
    """Start the FastAPI server"""
    port = int(os.getenv("LLM_SERVICE_PORT", "5002"))
    host = os.getenv("LLM_SERVICE_HOST", "0.0.0.0")
    
    uvicorn.run(
        app,
        host=host,
        port=port,
        reload=os.getenv("LOG_LEVEL") == "DEBUG",
        log_level=os.getenv("LOG_LEVEL", "INFO").lower()
    )

if __name__ == "__main__":
    start_api_server()