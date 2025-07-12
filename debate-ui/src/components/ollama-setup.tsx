'use client';

import { useState, useEffect, useCallback, useMemo } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { OllamaClient } from '@/lib/mcp-client';
import { OLLAMA_MODELS } from '@/types/debate';
import { logger } from '@/lib/logger';
import { 
  Download, 
  CheckCircle2, 
  AlertCircle, 
  Cpu, 
  HardDrive,
  Zap,
  RefreshCw,
  Server,
  Loader2
} from 'lucide-react';

export function OllamaSetup() {
  const [installedModels, setInstalledModels] = useState<any[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [pullingModel, setPullingModel] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const ollamaClient = useMemo(() => new OllamaClient(), []);

  const checkConnection = useCallback(async () => {
    try {
      setIsLoading(true);
      const models = await ollamaClient.listModels();
      setInstalledModels(models);
      setIsConnected(true);
      setError(null);
    } catch (error) {
      setIsConnected(false);
      setError('Cannot connect to Ollama. Make sure Ollama is running on http://localhost:11434');
    } finally {
      setIsLoading(false);
    }
  }, [ollamaClient]);

  useEffect(() => {
    checkConnection();
  }, [checkConnection]);

  const pullModel = async (modelName: string) => {
    try {
      setPullingModel(modelName);
      await ollamaClient.pullModel(modelName);
      await checkConnection(); // Refresh the list
    } catch (error) {
      logger.error('Failed to pull model', error as Error, { modelName });
      setError(`Failed to pull model ${modelName}`);
    } finally {
      setPullingModel(null);
    }
  };

  const isModelInstalled = (modelName: string) => {
    return installedModels.some(m => m.name === modelName || m.name.startsWith(modelName));
  };

  const getModelSize = (model: { size?: number }) => {
    if (!model.size) return 'Unknown';
    const gb = model.size / (1024 * 1024 * 1024);
    return `${gb.toFixed(1)} GB`;
  };

  return (
    <div className="space-y-6">
      {/* Connection Status */}
      <Card className="border-0 shadow-lg">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className={`h-10 w-10 rounded-lg flex items-center justify-center ${
                isConnected ? 'bg-emerald-100 dark:bg-emerald-900' : 'bg-red-100 dark:bg-red-900'
              }`}>
                <Server className={`h-5 w-5 ${
                  isConnected ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400'
                }`} />
              </div>
              <div>
                <CardTitle>Ollama Connection</CardTitle>
                <CardDescription>
                  {isConnected ? 'Connected to Ollama service' : 'Not connected to Ollama'}
                </CardDescription>
              </div>
            </div>
            <Button 
              onClick={checkConnection} 
              variant="outline"
              disabled={isLoading}
            >
              {isLoading ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <RefreshCw className="h-4 w-4" />
              )}
              <span className="ml-2">Refresh</span>
            </Button>
          </div>
        </CardHeader>
        {error && (
          <CardContent>
            <Alert className="border-red-200 bg-red-50 dark:border-red-800 dark:bg-red-950">
              <AlertCircle className="h-4 w-4 text-red-600 dark:text-red-400" />
              <AlertDescription className="text-red-800 dark:text-red-200">
                {error}
              </AlertDescription>
            </Alert>
          </CardContent>
        )}
      </Card>

      {/* Installed Models */}
      {isConnected && installedModels.length > 0 && (
        <Card className="border-0 shadow-lg">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <CheckCircle2 className="h-5 w-5 text-emerald-600" />
              Installed Models
            </CardTitle>
            <CardDescription>
              Models available for use in debates
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-3">
              {installedModels.map((model) => (
                <div 
                  key={model.name}
                  className="flex items-center justify-between p-4 rounded-lg bg-gradient-to-r from-emerald-50 to-emerald-100 dark:from-emerald-950 dark:to-emerald-900"
                >
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-lg bg-emerald-200 dark:bg-emerald-800 flex items-center justify-center">
                      <Cpu className="h-5 w-5 text-emerald-700 dark:text-emerald-300" />
                    </div>
                    <div>
                      <p className="font-medium">{model.name}</p>
                      <div className="flex items-center gap-3 text-sm text-muted-foreground">
                        <span className="flex items-center gap-1">
                          <HardDrive className="h-3 w-3" />
                          {getModelSize(model)}
                        </span>
                        <span className="flex items-center gap-1">
                          <Zap className="h-3 w-3" />
                          Ready
                        </span>
                      </div>
                    </div>
                  </div>
                  <Badge className="bg-emerald-600 text-white">
                    Installed
                  </Badge>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Available Models */}
      <Card className="border-0 shadow-lg">
        <CardHeader>
          <CardTitle>Available Models</CardTitle>
          <CardDescription>
            Popular open-source models for AI debates
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2">
            {OLLAMA_MODELS.map((model) => {
              const installed = isModelInstalled(model.name);
              return (
                <div
                  key={model.name}
                  className={`p-4 rounded-lg border ${
                    installed 
                      ? 'bg-gradient-to-r from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 border-gray-200 dark:border-gray-700' 
                      : 'bg-gradient-to-r from-blue-50 to-purple-50 dark:from-blue-950 dark:to-purple-950 border-blue-200 dark:border-blue-800'
                  }`}
                >
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex items-center gap-3">
                      <div className={`h-10 w-10 rounded-lg flex items-center justify-center ${
                        installed
                          ? 'bg-gray-200 dark:bg-gray-700'
                          : 'bg-gradient-to-br from-blue-500 to-purple-500'
                      }`}>
                        <Cpu className={`h-5 w-5 ${
                          installed ? 'text-gray-600 dark:text-gray-400' : 'text-white'
                        }`} />
                      </div>
                      <div>
                        <h4 className="font-semibold">{model.name}</h4>
                        <p className="text-sm text-muted-foreground flex items-center gap-2">
                          <HardDrive className="h-3 w-3" />
                          {model.size}
                        </p>
                      </div>
                    </div>
                  </div>
                  <p className="text-sm text-muted-foreground mb-3">
                    {model.description}
                  </p>
                  {installed ? (
                    <Badge variant="secondary" className="w-full justify-center">
                      <CheckCircle2 className="h-3 w-3 mr-1" />
                      Installed
                    </Badge>
                  ) : (
                    <Button
                      className="w-full"
                      variant="outline"
                      onClick={() => pullModel(model.name)}
                      disabled={!isConnected || pullingModel === model.name}
                    >
                      {pullingModel === model.name ? (
                        <>
                          <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                          Downloading...
                        </>
                      ) : (
                        <>
                          <Download className="h-4 w-4 mr-2" />
                          Install Model
                        </>
                      )}
                    </Button>
                  )}
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>

      {/* Getting Started */}
      {!isConnected && (
        <Card className="border-0 shadow-lg bg-gradient-to-br from-blue-50 to-purple-50 dark:from-blue-950 dark:to-purple-950">
          <CardHeader>
            <CardTitle>Getting Started with Ollama</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-3">
              <h4 className="font-medium">1. Install Ollama</h4>
              <p className="text-sm text-muted-foreground">
                Visit <a href="https://ollama.ai" target="_blank" rel="noopener noreferrer" className="text-blue-600 hover:underline">ollama.ai</a> to download Ollama for your operating system.
              </p>
            </div>
            <div className="space-y-3">
              <h4 className="font-medium">2. Start Ollama</h4>
              <pre className="bg-gray-100 dark:bg-gray-800 p-3 rounded-lg text-sm">
                <code>ollama serve</code>
              </pre>
            </div>
            <div className="space-y-3">
              <h4 className="font-medium">3. Install a Model</h4>
              <pre className="bg-gray-100 dark:bg-gray-800 p-3 rounded-lg text-sm">
                <code>ollama pull llama3</code>
              </pre>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}