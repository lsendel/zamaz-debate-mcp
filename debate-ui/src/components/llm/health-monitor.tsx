'use client';

import React, { useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { useLLM } from '@/hooks/use-llm';
import { 
  CheckCircle2, 
  XCircle, 
  AlertCircle, 
  RefreshCw, 
  Activity,
  Brain,
  Zap,
  Sparkles,
  Server,
  Cloud
} from 'lucide-react';
import { cn } from '@/lib/utils';

export function LLMHealthMonitor() {
  const { health, providers, checkHealth, loading } = useLLM();

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'healthy':
        return <CheckCircle2 className="h-5 w-5 text-green-500" />;
      case 'degraded':
        return <AlertCircle className="h-5 w-5 text-yellow-500" />;
      case 'unhealthy':
        return <XCircle className="h-5 w-5 text-red-500" />;
      default:
        return <AlertCircle className="h-5 w-5 text-gray-500" />;
    }
  };

  const getProviderIcon = (provider: string) => {
    switch (provider.toLowerCase()) {
      case 'claude':
      case 'anthropic':
        return <Brain className="h-4 w-4" />;
      case 'openai':
        return <Zap className="h-4 w-4" />;
      case 'gemini':
      case 'google':
        return <Sparkles className="h-4 w-4" />;
      case 'ollama':
      case 'local':
        return <Server className="h-4 w-4" />;
      default:
        return <Cloud className="h-4 w-4" />;
    }
  };

  const getStatusBadgeVariant = (available: boolean) => {
    return available ? 'default' : 'destructive';
  };

  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp);
    return date.toLocaleString();
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Activity className="h-5 w-5" />
              LLM Service Health
            </CardTitle>
            <CardDescription>
              Monitor the status of AI model providers
            </CardDescription>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => checkHealth()}
            disabled={loading}
          >
            <RefreshCw className={cn("h-4 w-4 mr-2", loading && "animate-spin")} />
            Refresh
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {health ? (
          <div className="space-y-4">
            {/* Overall Status */}
            <div className="flex items-center justify-between p-3 bg-muted rounded-md">
              <div className="flex items-center gap-2">
                {getStatusIcon(health.status)}
                <span className="font-medium">Overall Status</span>
              </div>
              <Badge variant={health.status === 'healthy' ? 'default' : health.status === 'degraded' ? 'secondary' : 'destructive'}>
                {health.status.toUpperCase()}
              </Badge>
            </div>

            {/* Provider Details */}
            <div className="space-y-2">
              <h4 className="text-sm font-medium text-muted-foreground">Providers</h4>
              <div className="grid gap-2">
                {Object.entries(health.providers).map(([provider, status]) => (
                  <div
                    key={provider}
                    className={cn(
                      "flex items-center justify-between p-3 border rounded-md",
                      status.available ? "bg-green-50 dark:bg-green-950/20" : "bg-red-50 dark:bg-red-950/20"
                    )}
                  >
                    <div className="flex items-center gap-3">
                      {getProviderIcon(provider)}
                      <div>
                        <div className="font-medium capitalize">{provider}</div>
                        {status.error && (
                          <div className="text-xs text-red-600 dark:text-red-400 mt-1">
                            {status.error}
                          </div>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge variant={getStatusBadgeVariant(status.available)}>
                        {status.available ? 'Available' : 'Unavailable'}
                      </Badge>
                      {status.models && status.models.length > 0 && (
                        <Badge variant="outline" className="text-xs">
                          {status.models.length} models
                        </Badge>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Last Updated */}
            <div className="text-xs text-muted-foreground text-right">
              Last updated: {formatTimestamp(health.timestamp)}
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-center py-8 text-muted-foreground">
            <Activity className="h-8 w-8 animate-pulse" />
            <span className="ml-2">Checking health status...</span>
          </div>
        )}

        {/* Available Models Summary */}
        {providers.length > 0 && (
          <div className="mt-6 pt-6 border-t">
            <h4 className="text-sm font-medium text-muted-foreground mb-2">
              Available Models ({providers.reduce((acc, p) => acc + p.models.length, 0)})
            </h4>
            <div className="flex flex-wrap gap-2">
              {providers.flatMap(p => p.models.map(m => (
                <Badge key={m.id} variant="secondary" className="text-xs">
                  {m.name}
                </Badge>
              )))}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}