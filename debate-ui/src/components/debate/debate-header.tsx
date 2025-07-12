'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Users, MessageSquare, Clock } from 'lucide-react';
import { Debate, Turn } from '@/types/debate';

interface DebateHeaderProps {
  debate: Debate;
  turns: Turn[];
}

export function DebateHeader({ debate, turns }: DebateHeaderProps) {
  const currentRound = Math.ceil(turns.length / debate.participants.length) || 1;
  
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active': return 'bg-emerald-500';
      case 'paused': return 'bg-amber-500';
      case 'completed': return 'bg-blue-500';
      default: return 'bg-gray-500';
    }
  };

  return (
    <Card className="border-0 shadow-lg">
      <CardHeader>
        <div className="flex items-start justify-between">
          <div>
            <CardTitle className="text-2xl">{debate.name}</CardTitle>
            <CardDescription className="text-base mt-2">{debate.topic}</CardDescription>
          </div>
          <Badge className={`${getStatusColor(debate.status)} text-white`}>
            {debate.status}
          </Badge>
        </div>
        
        <div className="flex items-center gap-6 mt-4 text-sm text-muted-foreground">
          <div className="flex items-center gap-2">
            <Users className="h-4 w-4" />
            {debate.participants.length} Participants
          </div>
          <div className="flex items-center gap-2">
            <MessageSquare className="h-4 w-4" />
            Round {currentRound} of {debate.rules.maxRounds}
          </div>
          <div className="flex items-center gap-2">
            <Clock className="h-4 w-4" />
            {turns.length} Turns
          </div>
        </div>
      </CardHeader>
    </Card>
  );
}