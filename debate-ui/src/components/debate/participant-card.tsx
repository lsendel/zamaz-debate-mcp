'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Brain } from 'lucide-react';
import { Participant } from '@/types/debate';

interface ParticipantCardProps {
  participant: Participant;
  turnCount: number;
  isSpeaking: boolean;
  color: string;
}

export function ParticipantCard({ participant, turnCount, isSpeaking, color }: ParticipantCardProps) {
  return (
    <Card className={`transition-all ${isSpeaking ? 'ring-2 ring-blue-500' : ''}`}>
      <CardContent className="p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className={`w-10 h-10 rounded-full bg-gradient-to-br ${color} flex items-center justify-center`}>
              <Brain className="h-5 w-5 text-white" />
            </div>
            <div>
              <h4 className="font-semibold">{participant.name}</h4>
              <p className="text-sm text-muted-foreground">{participant.position || 'No position stated'}</p>
            </div>
          </div>
          <div className="text-right">
            <p className="text-sm text-muted-foreground">{turnCount} turns</p>
            <Badge variant="outline" className="text-xs">
              {participant.llm_config?.model || 'Unknown model'}
            </Badge>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}