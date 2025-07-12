'use client';

import { useState, useEffect, useCallback } from 'react';
import { logger } from '@/lib/logger';

export interface LLMProvider {
  id: string;
  name: string;
  type: 'api' | 'local';
  models: LLMModel[];
  isAvailable: boolean;
  requiredConfig?: string[];
}

export interface LLMModel {
  id: string;
  name: string;
  provider: string;
  capabilities: string[];
  contextWindow: number;
  costPer1kTokens?: {
    input: number;
    output: number;
  };
}

export interface LLMHealth {
  status: 'healthy' | 'degraded' | 'unhealthy';
  providers: Record<string, {
    available: boolean;
    error?: string;
    models: string[];
  }>;
  timestamp: string;
}

export function useLLM() {
  const [providers, setProviders] = useState<LLMProvider[]>([]);
  const [models, setModels] = useState<LLMModel[]>([]);
  const [health, setHealth] = useState<LLMHealth | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch available providers
  const fetchProviders = useCallback(async () => {
    try {
      const response = await fetch('/api/llm/providers');
      if (!response.ok) throw new Error('Failed to fetch providers');
      const data = await response.json();
      setProviders(data.providers || []);
    } catch (err) {
      logger.error('Error fetching providers', err as Error);
      setError('Failed to load LLM providers');
    }
  }, []);

  // Fetch available models
  const fetchModels = useCallback(async () => {
    try {
      const response = await fetch('/api/llm/models');
      if (!response.ok) throw new Error('Failed to fetch models');
      const data = await response.json();
      setModels(data.models || []);
    } catch (err) {
      logger.error('Error fetching models', err as Error);
      setError('Failed to load LLM models');
    }
  }, []);

  // Check health status
  const checkHealth = useCallback(async () => {
    try {
      const response = await fetch('/api/llm/health');
      if (!response.ok) throw new Error('Failed to check health');
      const data = await response.json();
      setHealth(data);
    } catch (err) {
      logger.error('Error checking health', err as Error);
      setHealth({
        status: 'unhealthy',
        providers: {},
        timestamp: new Date().toISOString()
      });
    }
  }, []);

  // Generate completion
  const generateCompletion = useCallback(async (
    prompt: string,
    options?: {
      model?: string;
      temperature?: number;
      maxTokens?: number;
      systemPrompt?: string;
    }
  ) => {
    try {
      const response = await fetch('/api/llm/completions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          prompt,
          model: options?.model,
          temperature: options?.temperature ?? 0.7,
          max_tokens: options?.maxTokens ?? 1000,
          system_prompt: options?.systemPrompt
        })
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to generate completion');
      }

      return await response.json();
    } catch (err) {
      logger.error('Error generating completion', err as Error, { prompt: prompt.substring(0, 100) });
      throw err;
    }
  }, []);

  // Stream completion
  const streamCompletion = useCallback(async function* (
    prompt: string,
    options?: {
      model?: string;
      temperature?: number;
      maxTokens?: number;
      systemPrompt?: string;
    }
  ) {
    try {
      const response = await fetch('/api/llm/completions/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          prompt,
          model: options?.model,
          temperature: options?.temperature ?? 0.7,
          max_tokens: options?.maxTokens ?? 1000,
          system_prompt: options?.systemPrompt,
          stream: true
        })
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to stream completion');
      }

      const reader = response.body?.getReader();
      if (!reader) throw new Error('No response body');

      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.trim() === '') continue;
          if (line.startsWith('data: ')) {
            const data = line.slice(6);
            if (data === '[DONE]') return;
            try {
              const parsed = JSON.parse(data);
              yield parsed;
            } catch (e) {
              logger.warn('Error parsing SSE data', { error: e, data });
            }
          }
        }
      }
    } catch (err) {
      logger.error('Error streaming completion', err as Error, { prompt: prompt.substring(0, 100) });
      throw err;
    }
  }, []);

  // Configure provider
  const configureProvider = useCallback(async (
    provider: string,
    config: Record<string, any>
  ) => {
    try {
      const response = await fetch(`/api/llm/providers/${provider}/config`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config)
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to configure provider');
      }

      // Refresh providers and health after configuration
      await Promise.all([fetchProviders(), checkHealth()]);
      
      return await response.json();
    } catch (err) {
      logger.error('Error configuring provider', err as Error, { provider });
      throw err;
    }
  }, [fetchProviders, checkHealth]);

  // Initial load
  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        await Promise.all([
          fetchProviders(),
          fetchModels(),
          checkHealth()
        ]);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [fetchProviders, fetchModels, checkHealth]);

  // Set up health check interval
  useEffect(() => {
    const interval = setInterval(checkHealth, 30000); // Check every 30 seconds
    return () => clearInterval(interval);
  }, [checkHealth]);

  return {
    providers,
    models,
    health,
    loading,
    error,
    generateCompletion,
    streamCompletion,
    configureProvider,
    refreshProviders: fetchProviders,
    refreshModels: fetchModels,
    checkHealth
  };
}

// Helper hook for model selection
export function useModelSelection(defaultModel?: string) {
  const { models, loading } = useLLM();
  const [selectedModel, setSelectedModel] = useState<string>(defaultModel || '');

  useEffect(() => {
    if (!selectedModel && models.length > 0 && !loading) {
      // Auto-select first available model
      setSelectedModel(models[0].id);
    }
  }, [models, loading, selectedModel]);

  const getSelectedModel = useCallback(() => {
    return models.find(m => m.id === selectedModel);
  }, [models, selectedModel]);

  return {
    models,
    selectedModel,
    setSelectedModel,
    getSelectedModel,
    loading
  };
}