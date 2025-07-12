'use client';

import { useState } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { ModelSelector } from '@/components/llm/model-selector';
import { useLLM } from '@/hooks/use-llm';
import { Loader2, Zap, AlertCircle, CheckCircle } from 'lucide-react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card } from '@/components/ui/card';

interface LLMTestDialogProps {
  open: boolean;
  onOpenChange: (_open: boolean) => void;
}

export function LLMTestDialog({ open, onOpenChange }: LLMTestDialogProps) {
  const { models } = useLLM();
  const [selectedProvider, setSelectedProvider] = useState('');
  const [selectedModel, setSelectedModel] = useState('');
  const [testPrompt, setTestPrompt] = useState('Hello! Can you briefly introduce yourself and confirm you are working?');
  const [response, setResponse] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [responseTime, setResponseTime] = useState<number | null>(null);

  const handleTest = async () => {
    if (!selectedProvider || !selectedModel) {
      setError('Please select a provider and model');
      return;
    }

    setLoading(true);
    setError('');
    setResponse('');
    setResponseTime(null);

    const startTime = Date.now();

    try {
      const res = await fetch('/api/llm/completions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          provider: selectedProvider,
          model: selectedModel,
          messages: [
            {
              role: 'user',
              content: testPrompt
            }
          ],
          temperature: 0.7,
          max_tokens: 500
        }),
      });

      const data = await res.json();
      const endTime = Date.now();
      setResponseTime(endTime - startTime);

      if (!res.ok) {
        throw new Error(data.error || 'Failed to get response');
      }

      setResponse(data.choices[0].text);
    } catch (err) {
      setError((err as Error).message || 'Failed to connect to LLM service');
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = () => {
    if (loading) return 'text-blue-600';
    if (error) return 'text-red-600';
    if (response) return 'text-green-600';
    return 'text-gray-600';
  };

  const getStatusIcon = () => {
    if (loading) return <Loader2 className="h-4 w-4 animate-spin" />;
    if (error) return <AlertCircle className="h-4 w-4" />;
    if (response) return <CheckCircle className="h-4 w-4" />;
    return <Zap className="h-4 w-4" />;
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Zap className="h-5 w-5 text-yellow-500" />
            Test LLM Connection
          </DialogTitle>
          <DialogDescription>
            Test the connection and response from different LLM providers
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {/* Model Selection */}
          <div className="space-y-2">
            <Label>Select Model</Label>
            <ModelSelector
              value={selectedModel}
              onChange={(modelId) => {
                setSelectedModel(modelId);
                const model = models.find(m => m.id === modelId);
                if (model) {
                  setSelectedProvider(model.provider);
                }
              }}
              required
            />
          </div>

          {/* Test Prompt */}
          <div className="space-y-2">
            <Label htmlFor="prompt">Test Prompt</Label>
            <Textarea
              id="prompt"
              value={testPrompt}
              onChange={(e) => setTestPrompt(e.target.value)}
              placeholder="Enter a test prompt..."
              className="min-h-[100px]"
            />
          </div>

          {/* Status */}
          <div className={`flex items-center gap-2 ${getStatusColor()}`}>
            {getStatusIcon()}
            <span className="text-sm font-medium">
              {loading && 'Sending request...'}
              {error && 'Connection failed'}
              {response && responseTime && `Response received in ${responseTime}ms`}
              {!loading && !error && !response && 'Ready to test'}
            </span>
          </div>

          {/* Error Alert */}
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* Response */}
          {response && (
            <Card className="p-4 bg-gray-50 dark:bg-gray-900">
              <Label className="text-sm text-muted-foreground mb-2 block">Response:</Label>
              <div className="whitespace-pre-wrap text-sm">{response}</div>
              {responseTime && (
                <div className="mt-2 text-xs text-muted-foreground">
                  Token usage: ~{response.split(' ').length} tokens
                </div>
              )}
            </Card>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Close
          </Button>
          <Button 
            onClick={handleTest} 
            disabled={loading || !selectedProvider || !selectedModel}
            className="min-w-[100px]"
          >
            {loading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Testing...
              </>
            ) : (
              'Test Connection'
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}