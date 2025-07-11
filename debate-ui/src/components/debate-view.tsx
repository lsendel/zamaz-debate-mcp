'use client';

import { useState, useEffect, useRef } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Separator } from '@/components/ui/separator';
import { MCPClient } from '@/lib/mcp-client';
import { Debate, Turn } from '@/types/debate';
import { ImplementationTracker } from '@/components/implementation-tracker';
import { useOrganization } from '@/hooks/use-organization';
import ReactMarkdown from 'react-markdown';
import { 
  Play, 
  Pause, 
  SkipForward, 
  Square, 
  MessageSquare, 
  Clock, 
  Users,
  Brain,
  Loader2,
  ChevronRight,
  Sparkles
} from 'lucide-react';

interface DebateViewProps {
  debate: Debate;
  onUpdate: () => void;
}

export function DebateView({ debate, onUpdate }: DebateViewProps) {
  const [turns, setTurns] = useState<Turn[]>([]);
  const [isRunning, setIsRunning] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [currentSpeaker, setCurrentSpeaker] = useState<string | null>(null);
  const [showImplementation, setShowImplementation] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);
  const { currentOrg, addHistoryEntry } = useOrganization();
  
  const debateClient = new MCPClient('debate');

  useEffect(() => {
    loadTurns();
  }, [debate.id]);

  useEffect(() => {
    // Auto-scroll to bottom when new turns are added
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [turns]);

  const loadTurns = async () => {
    try {
      const response = await debateClient.readResource(`debate://debates/${debate.id}/turns`);
      setTurns(response.turns || []);
    } catch (error) {
      console.error('Failed to load turns:', error);
    }
  };

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
      console.error('Failed to start debate:', error);
    }
  };

  const pauseDebate = async () => {
    try {
      await debateClient.callTool('pause_debate', { debate_id: debate.id });
      setIsRunning(false);
      onUpdate();
    } catch (error) {
      console.error('Failed to pause debate:', error);
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
      console.error('Failed to resume debate:', error);
    }
  };

  const getNextTurn = async () => {
    if (!isRunning) return;
    
    try {
      setIsLoading(true);
      const participant = debate.participants.find(p => p.id === debate.nextParticipantId);
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
      if (debate.rules.maxRounds && newTurn.roundNumber >= debate.rules.maxRounds) {
        setIsRunning(false);
        setCurrentSpeaker(null);
      } else if (isRunning) {
        // Continue with next turn after a delay
        setTimeout(() => getNextTurn(), 2000);
      }
    } catch (error) {
      console.error('Failed to get next turn:', error);
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
    const colors = [
      'from-blue-500 to-blue-600',
      'from-purple-500 to-purple-600',
      'from-green-500 to-green-600',
      'from-orange-500 to-orange-600',
      'from-pink-500 to-pink-600',
      'from-teal-500 to-teal-600'
    ];
    return colors[index % colors.length];
  };

  const getParticipant = (participantId: string) => {
    return debate.participants.find(p => p.id === participantId);
  };

  return (
    <div className="space-y-6">
      {/* Debate Header */}
      <Card className="border-0 shadow-lg">
        <CardHeader>
          <div className="flex items-start justify-between">
            <div>
              <CardTitle className="text-2xl">{debate.name}</CardTitle>
              <CardDescription className="text-base mt-2">{debate.topic}</CardDescription>
            </div>
            <Badge 
              className={`${
                debate.status === 'active' ? 'bg-emerald-500' : 
                debate.status === 'paused' ? 'bg-amber-500' : 
                debate.status === 'completed' ? 'bg-blue-500' : 
                'bg-gray-500'
              } text-white`}
            >
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
              Round {Math.ceil(turns.length / debate.participants.length) || 1} of {debate.rules.maxRounds}
            </div>
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4" />
              {turns.length} Turns
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {/* Control Buttons */}
          <div className="flex items-center gap-3">
            {debate.status === 'draft' && (
              <Button onClick={startDebate} className="bg-emerald-600 hover:bg-emerald-700 text-white">
                <Play className="h-4 w-4 mr-2" />
                Start Debate
              </Button>
            )}
            
            {debate.status === 'active' && (
              <>
                <Button onClick={pauseDebate} variant="outline">
                  <Pause className="h-4 w-4 mr-2" />
                  Pause
                </Button>
                <Button onClick={skipTurn} variant="outline" disabled={isLoading}>
                  <SkipForward className="h-4 w-4 mr-2" />
                  Skip Turn
                </Button>
              </>
            )}
            
            {debate.status === 'paused' && (
              <Button onClick={resumeDebate} className="bg-amber-600 hover:bg-amber-700 text-white">
                <Play className="h-4 w-4 mr-2" />
                Resume
              </Button>
            )}
            
            {debate.status === 'completed' && (
              <Button variant="outline">
                <Sparkles className="h-4 w-4 mr-2" />
                View Summary
              </Button>
            )}
          </div>

          {/* Current Speaker Indicator */}
          {currentSpeaker && (
            <div className="mt-4 p-3 rounded-lg bg-gradient-to-r from-blue-50 to-purple-50 dark:from-blue-950 dark:to-purple-950 flex items-center gap-3">
              <Loader2 className="h-4 w-4 animate-spin text-blue-600" />
              <span className="text-sm font-medium">
                {currentSpeaker} is thinking...
              </span>
            </div>
          )}
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
            {debate.participants.map((participant) => (
              <div 
                key={participant.id}
                className={`p-4 rounded-lg border ${
                  currentSpeaker === participant.name 
                    ? 'border-blue-500 bg-blue-50 dark:bg-blue-950' 
                    : 'border-gray-200 dark:border-gray-700'
                }`}
              >
                <div className="flex items-start justify-between">
                  <div>
                    <h4 className="font-semibold flex items-center gap-2">
                      {participant.name}
                      {currentSpeaker === participant.name && (
                        <Badge className="bg-blue-600 text-white text-xs">
                          Speaking
                        </Badge>
                      )}
                    </h4>
                    {participant.position && (
                      <p className="text-sm text-muted-foreground mt-1">{participant.position}</p>
                    )}
                  </div>
                  <Badge variant="outline" className="text-xs">
                    {participant.role}
                  </Badge>
                </div>
                <div className="flex items-center gap-2 mt-3 text-xs text-muted-foreground">
                  <Brain className="h-3 w-3" />
                  <span>{participant.llm_config.provider}</span>
                  <span>•</span>
                  <span>{participant.llm_config.model}</span>
                </div>
              </div>
            ))}
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
                  if (!participant) return null;
                  
                  return (
                    <div key={turn.id} className="animate-in fade-in slide-in-from-bottom-3">
                      <div className="flex items-start gap-3">
                        <div className={`w-10 h-10 rounded-full bg-gradient-to-br ${getParticipantColor(turn.participantId)} flex items-center justify-center text-white font-semibold`}>
                          {participant.name[0]}
                        </div>
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-1">
                            <span className="font-semibold">{participant.name}</span>
                            <Badge variant="outline" className="text-xs">
                              {turn.turnType}
                            </Badge>
                            <span className="text-xs text-muted-foreground">
                              Round {turn.roundNumber} • Turn {turn.turnNumber}
                            </span>
                          </div>
                          <div className="prose prose-sm dark:prose-invert max-w-none">
                            <ReactMarkdown>{turn.content}</ReactMarkdown>
                          </div>
                        </div>
                      </div>
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
          console.log('New implementation tracked:', impl);
        }}
      />
    </div>
  );
}