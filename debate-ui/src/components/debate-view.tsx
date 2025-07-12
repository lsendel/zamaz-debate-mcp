'use client';

import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Separator } from '@/components/ui/separator';
import { MCPClient } from '@/lib/mcp-client';
import { Debate, Turn } from '@/types/debate';
import { ImplementationTracker } from '@/components/implementation-tracker';
import { useOrganization } from '@/hooks/use-organization';
import { logger } from '@/lib/logger';
import { Users, MessageSquare, Loader2 } from 'lucide-react';

// Import new components
import { DebateHeader } from './debate/debate-header';
import { DebateControls } from './debate/debate-controls';
import { ParticipantCard } from './debate/participant-card';
import { TurnMessage } from './debate/turn-message';

// Constants
const NEXT_TURN_DELAY_MS = 2000;
const PARTICIPANT_COLORS = [
  'from-blue-500 to-blue-600',
  'from-purple-500 to-purple-600',
  'from-green-500 to-green-600',
  'from-orange-500 to-orange-600',
  'from-pink-500 to-pink-600',
  'from-teal-500 to-teal-600'
];

interface DebateViewProps {
  debate: Debate;
  onUpdate: () => void;
}

export function DebateView({ debate, onUpdate }: DebateViewProps) {
  const [turns, setTurns] = useState<Turn[]>([]);
  const [isRunning, setIsRunning] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [currentSpeaker, setCurrentSpeaker] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const { currentOrg, addHistoryEntry } = useOrganization();
  
  const debateClient = useMemo(() => new MCPClient('debate'), []);

  const loadTurns = useCallback(async () => {
    try {
      const response = await debateClient.readResource(`debate://debates/${debate.id}/turns`);
      setTurns(response.turns || []);
    } catch (error) {
      logger.error('Failed to load turns', error as Error, { debateId: debate.id });
    }
  }, [debate.id, debateClient]);

  useEffect(() => {
    loadTurns();
  }, [loadTurns]);

  useEffect(() => {
    // Auto-scroll to bottom when new turns are added
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [turns]);

  const startDebate = async () => {
    try {
      await debateClient.callTool('start_debate', { debate_id: debate.id });
      setIsRunning(true);
      onUpdate();
      
      // Add to organization history
      if (currentOrg) {
        addHistoryEntry({
          organizationId: currentOrg.id,
          action: 'debate_started',
          description: `Started debate: ${debate.name}`,
          metadata: {
            debateId: debate.id,
            topic: debate.topic
          }
        });
      }
      
      // Start the first turn
      await getNextTurn();
    } catch (error) {
      logger.error('Failed to start debate', error as Error, { debateId: debate.id });
    }
  };

  const pauseDebate = async () => {
    try {
      await debateClient.callTool('pause_debate', { debate_id: debate.id });
      setIsRunning(false);
      onUpdate();
    } catch (error) {
      logger.error('Failed to pause debate', error as Error, { debateId: debate.id });
    }
  };

  const resumeDebate = async () => {
    try {
      await debateClient.callTool('resume_debate', { debate_id: debate.id });
      setIsRunning(true);
      onUpdate();
      // Continue with next turn
      await getNextTurn();
    } catch (error) {
      logger.error('Failed to resume debate', error as Error, { debateId: debate.id });
    }
  };

  const getNextTurn = async () => {
    if (!isRunning) return;
    
    try {
      setIsLoading(true);
      const participant = debate.nextParticipantId 
        ? debate.participants.find(p => p.id === debate.nextParticipantId)
        : undefined;
      setCurrentSpeaker(participant?.name || null);
      
      const response = await debateClient.callTool('get_next_turn', {
        debate_id: debate.id,
        include_rag: false
      });
      
      // Add the new turn to our local state
      const newTurn: Turn = {
        id: response.turn_id,
        debateId: debate.id,
        participantId: response.participant_id,
        turnNumber: response.turn_number || turns.length + 1,
        roundNumber: Math.ceil((turns.length + 1) / debate.participants.length),
        turnType: response.turn_type || 'argument',
        content: response.content,
        createdAt: new Date().toISOString()
      };
      
      setTurns([...turns, newTurn]);
      onUpdate();
      
      // Check if debate is complete
      if (debate.rules?.maxRounds && newTurn.roundNumber >= debate.rules.maxRounds) {
        setIsRunning(false);
        setCurrentSpeaker(null);
      } else if (isRunning) {
        // Continue with next turn after a delay
        setTimeout(() => getNextTurn(), NEXT_TURN_DELAY_MS);
      }
    } catch (error) {
      logger.error('Failed to get next turn', error as Error, { 
        debateId: debate.id,
        currentSpeaker 
      });
      setIsRunning(false);
    } finally {
      setIsLoading(false);
      if (!isRunning) {
        setCurrentSpeaker(null);
      }
    }
  };

  const skipTurn = async () => {
    await getNextTurn();
  };

  const getParticipantColor = (participantId: string) => {
    const index = debate.participants.findIndex(p => p.id === participantId);
    return PARTICIPANT_COLORS[index % PARTICIPANT_COLORS.length];
  };

  const getParticipant = (participantId: string) => {
    return debate.participants.find(p => p.id === participantId);
  };

  return (
    <div className="space-y-6">
      {/* Debate Header */}
      <DebateHeader debate={debate} turns={turns} />
      
      {/* Control Section */}
      <Card className="border-0 shadow-lg">
        <CardContent className="pt-6">
          <DebateControls
            debate={debate}
            isLoading={isLoading}
            currentSpeaker={currentSpeaker}
            onStart={startDebate}
            onPause={pauseDebate}
            onResume={resumeDebate}
            onSkipTurn={skipTurn}
          />
        </CardContent>
      </Card>

      {/* Participants */}
      <Card className="border-0 shadow-lg">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Users className="h-5 w-5" />
            Participants
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-3 md:grid-cols-2">
            {debate.participants.map((participant) => {
              const turnCount = participant.id 
                ? turns.filter(t => t.participantId === participant.id).length
                : 0;
              return (
                <ParticipantCard
                  key={participant.id}
                  participant={participant}
                  turnCount={turnCount}
                  isSpeaking={currentSpeaker === participant.name}
                  color={getParticipantColor(participant.id || '')}
                />
              );
            })}
          </div>
        </CardContent>
      </Card>

      {/* Debate Transcript */}
      <Card className="border-0 shadow-lg">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <MessageSquare className="h-5 w-5" />
            Debate Transcript
          </CardTitle>
        </CardHeader>
        <CardContent>
          <ScrollArea className="h-[600px] pr-4" ref={scrollRef}>
            {turns.length === 0 ? (
              <div className="text-center py-12 text-muted-foreground">
                <MessageSquare className="h-12 w-12 mx-auto mb-4 opacity-20" />
                <p>No messages yet. Start the debate to begin.</p>
              </div>
            ) : (
              <div className="space-y-4">
                {turns.map((turn, index) => {
                  const participant = getParticipant(turn.participantId);
                  const participantIndex = debate.participants.findIndex(p => p.id === turn.participantId);
                  const isLeft = participantIndex % 2 === 0;
                  
                  return (
                    <div key={turn.id} className="animate-in fade-in slide-in-from-bottom-3">
                      <TurnMessage
                        turn={turn}
                        participant={participant}
                        color={getParticipantColor(turn.participantId || '')}
                        isLeft={isLeft}
                      />
                      {index < turns.length - 1 && <Separator className="my-4" />}
                    </div>
                  );
                })}
                
                {isLoading && (
                  <div className="flex items-center justify-center py-4">
                    <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                  </div>
                )}
              </div>
            )}
          </ScrollArea>
        </CardContent>
      </Card>

      {/* Implementation Tracking */}
      <ImplementationTracker 
        debateId={debate.id}
        onImplementationAdded={(impl) => {
          logger.info('New implementation tracked', { implementation: impl });
        }}
      />
    </div>
  );
}