'use client';

import React from 'react';
import { useModelSelection } from '@/hooks/use-llm';
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from '@/components/ui/select';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Loader2, AlertCircle } from 'lucide-react';
import { cn } from '@/lib/utils';

interface ModelSelectorProps {
  value?: string;
  onChange: (_modelId: string) => void;
  label?: string;
  required?: boolean;
  className?: string;
  participantName?: string;
}

export function ModelSelector({
  value,
  onChange,
  label = "AI Model",
  required = false,
  className,
  participantName
}: ModelSelectorProps) {
  const { models, loading, selectedModel, setSelectedModel } = useModelSelection(value);

  const handleChange = (modelId: string) => {
    setSelectedModel(modelId);
    onChange(modelId);
  };

  if (loading) {
    return (
      <div className={cn("space-y-2", className)}>
        <Label>{label}</Label>
        <div className="flex items-center justify-center h-10 border rounded-md bg-muted">
          <Loader2 className="h-4 w-4 animate-spin" />
          <span className="ml-2 text-sm text-muted-foreground">Loading models...</span>
        </div>
      </div>
    );
  }

  if (models.length === 0) {
    return (
      <div className={cn("space-y-2", className)}>
        <Label>{label}</Label>
        <div className="flex items-center p-3 border rounded-md bg-destructive/10 text-destructive">
          <AlertCircle className="h-4 w-4 mr-2" />
          <span className="text-sm">No models available. Please check LLM service configuration.</span>
        </div>
      </div>
    );
  }

  const groupedModels = models.reduce((acc, model) => {
    if (!acc[model.provider]) {
      acc[model.provider] = [];
    }
    acc[model.provider].push(model);
    return acc;
  }, {} as Record<string, typeof models>);

  return (
    <div className={cn("space-y-2", className)}>
      <Label htmlFor={`model-${participantName}`}>
        {label} {participantName && `for ${participantName}`}
        {required && <span className="text-destructive ml-1">*</span>}
      </Label>
      <Select value={selectedModel || value} onValueChange={handleChange}>
        <SelectTrigger id={`model-${participantName}`}>
          <SelectValue placeholder="Select a model" />
        </SelectTrigger>
        <SelectContent>
          {Object.entries(groupedModels).map(([provider, providerModels]) => (
            <div key={provider}>
              <div className="px-2 py-1.5 text-sm font-semibold text-muted-foreground">
                {provider}
              </div>
              {providerModels.map((model) => (
                <SelectItem key={model.id} value={model.id}>
                  <div className="flex items-center justify-between w-full">
                    <span>{model.name}</span>
                    <div className="flex items-center gap-1 ml-2">
                      {model.capabilities.includes('streaming') && (
                        <Badge variant="secondary" className="text-xs">Stream</Badge>
                      )}
                      {model.provider === 'llama' && (
                        <Badge variant="outline" className="text-xs">Local</Badge>
                      )}
                      {model.costPer1kTokens && (
                        <Badge variant="outline" className="text-xs">
                          ${model.costPer1kTokens.input}/1k
                        </Badge>
                      )}
                    </div>
                  </div>
                </SelectItem>
              ))}
            </div>
          ))}
        </SelectContent>
      </Select>
      {selectedModel && (
        <p className="text-xs text-muted-foreground">
          {(() => {
            const model = models.find(m => m.id === selectedModel);
            return model ? `Context window: ${model.contextWindow.toLocaleString()} tokens` : '';
          })()}
        </p>
      )}
    </div>
  );
}