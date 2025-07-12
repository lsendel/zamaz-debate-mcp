'use client';

import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Card } from '@/components/ui/card';
import { Brain } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { Turn, Participant } from '@/types/debate';

interface TurnMessageProps {
  turn: Turn;
  participant: Participant | undefined;
  color: string;
  isLeft: boolean;
}

export function TurnMessage({ turn, participant, color, isLeft }: TurnMessageProps) {
  return (
    <div className={`flex gap-3 ${isLeft ? '' : 'flex-row-reverse'}`}>
      <Avatar className="h-8 w-8 flex-shrink-0">
        <AvatarFallback className={`bg-gradient-to-br ${color}`}>
          <Brain className="h-4 w-4 text-white" />
        </AvatarFallback>
      </Avatar>
      
      <Card className={`flex-1 ${isLeft ? 'mr-12' : 'ml-12'}`}>
        <div className="p-4">
          <div className="flex items-baseline gap-2 mb-2">
            <span className="font-semibold">
              {participant?.name || 'Unknown'}
            </span>
            <span className="text-xs text-muted-foreground">
              Round {turn.roundNumber}
            </span>
          </div>
          
          <div className="prose prose-sm dark:prose-invert max-w-none">
            <ReactMarkdown>{turn.content}</ReactMarkdown>
          </div>
          
          <div className="mt-3 text-xs text-muted-foreground">
            {new Date(turn.createdAt).toLocaleTimeString()}
          </div>
        </div>
      </Card>
    </div>
  );
}