'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Progress } from '@/components/ui/progress';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Separator } from '@/components/ui/separator';
import { Avatar } from '@/components/ui/avatar';
import { 
  MessageSquare, 
  Users, 
  Brain, 
  Target, 
  CheckCircle2, 
  XCircle, 
  AlertTriangle, 
  Download, 
  Share2,
  TrendingUp,
  Clock,
  BarChart,
  FileText
} from 'lucide-react';
import { Debate, Turn, Participant } from '@/types/debate';

interface DebateResultsProps {
  debate: Debate;
  turns: Turn[];
  onClose?: () => void;
}

interface ParticipantAnalysis {
  participantId: string;
  participant: Participant;
  turnCount: number;
  avgTurnLength: number;
  keyPoints: string[];
  position: string;
  conclusion?: string;
}

interface ConsensusPoint {
  point: string;
  agreedBy: string[];
  strength: 'strong' | 'moderate' | 'weak';
}

interface DisagreementPoint {
  point: string;
  positions: Record<string, string>;
}

export function DebateResults({ debate, turns, onClose }: DebateResultsProps) {
  const [activeTab, setActiveTab] = useState('summary');
  const [participantAnalyses, setParticipantAnalyses] = useState<ParticipantAnalysis[]>([]);
  const [consensusPoints, setConsensusPoints] = useState<ConsensusPoint[]>([]);
  const [disagreementPoints, setDisagreementPoints] = useState<DisagreementPoint[]>([]);
  const [resolution, setResolution] = useState<string>('');

  useEffect(() => {
    analyzeDebate();
  }, [debate, turns]);

  const analyzeDebate = () => {
    // Analyze each participant's contributions
    const analyses: ParticipantAnalysis[] = debate.participants.map(participant => {
      const participantTurns = turns.filter(t => t.participantId === participant.id);
      const totalLength = participantTurns.reduce((sum, turn) => sum + turn.content.length, 0);
      
      return {
        participantId: participant.id || '',
        participant,
        turnCount: participantTurns.length,
        avgTurnLength: participantTurns.length > 0 ? Math.round(totalLength / participantTurns.length) : 0,
        keyPoints: extractKeyPoints(participantTurns),
        position: participant.position || 'Not specified',
        conclusion: debate.conclusions?.[participant.id || '']
      };
    });
    
    setParticipantAnalyses(analyses);
    
    // Extract consensus and disagreement points
    extractConsensusAndDisagreements(turns, debate.participants);
    
    // Generate resolution if available
    if (debate.resolution) {
      setResolution(debate.resolution);
    } else {
      generateResolution(analyses);
    }
  };

  const extractKeyPoints = (turns: Turn[]): string[] => {
    // Simple extraction - in real implementation, this would use NLP
    return turns
      .filter(turn => turn.turnType === 'argument' || turn.turnType === 'closing')
      .slice(0, 3)
      .map(turn => {
        const firstSentence = turn.content.split('.')[0];
        return firstSentence.length > 100 
          ? firstSentence.substring(0, 100) + '...' 
          : firstSentence;
      });
  };

  const extractConsensusAndDisagreements = (turns: Turn[], participants: Participant[]) => {
    // Mock implementation - in real system, this would analyze turn content
    const mockConsensus: ConsensusPoint[] = [
      {
        point: 'AI development requires some form of oversight',
        agreedBy: participants.map(p => p.name),
        strength: 'strong'
      },
      {
        point: 'Innovation should be balanced with safety considerations',
        agreedBy: participants.slice(0, -1).map(p => p.name),
        strength: 'moderate'
      }
    ];
    
    const mockDisagreements: DisagreementPoint[] = [
      {
        point: 'Level of government regulation needed',
        positions: {
          [participants[0]?.name || 'Participant 1']: 'Minimal regulation to encourage innovation',
          [participants[1]?.name || 'Participant 2']: 'Comprehensive regulatory framework needed'
        }
      }
    ];
    
    setConsensusPoints(mockConsensus);
    setDisagreementPoints(mockDisagreements);
  };

  const generateResolution = (analyses: ParticipantAnalysis[]) => {
    // Simple resolution generation
    const positions = analyses.map(a => a.position).join(', ');
    setResolution(
      `After examining ${debate.question?.mainQuestion || debate.topic}, considering perspectives including ${positions}, ` +
      `the debate highlights the complexity of the issue and the need for balanced approaches.`
    );
  };

  const exportResults = () => {
    const resultsData = {
      debate: {
        name: debate.name,
        question: debate.question,
        context: debate.context,
        subject: debate.subject
      },
      participants: participantAnalyses,
      consensus: consensusPoints,
      disagreements: disagreementPoints,
      resolution,
      statistics: {
        totalTurns: turns.length,
        totalRounds: debate.currentRound,
        duration: debate.completedAt && debate.startedAt 
          ? new Date(debate.completedAt).getTime() - new Date(debate.startedAt).getTime()
          : 0
      }
    };
    
    const blob = new Blob([JSON.stringify(resultsData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `debate-results-${debate.id}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const getStrengthColor = (strength: string) => {
    switch (strength) {
      case 'strong': return 'text-green-600 bg-green-50';
      case 'moderate': return 'text-yellow-600 bg-yellow-50';
      case 'weak': return 'text-orange-600 bg-orange-50';
      default: return 'text-gray-600 bg-gray-50';
    }
  };

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">{debate.name} - Results</h1>
          <p className="text-muted-foreground mt-1">
            {debate.subject && <Badge variant="outline" className="mr-2">{debate.subject}</Badge>}
            Completed {debate.completedAt ? new Date(debate.completedAt).toLocaleString() : 'In Progress'}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={exportResults}>
            <Download className="h-4 w-4 mr-2" />
            Export
          </Button>
          <Button variant="outline">
            <Share2 className="h-4 w-4 mr-2" />
            Share
          </Button>
          {onClose && (
            <Button onClick={onClose}>Close</Button>
          )}
        </div>
      </div>

      {/* Main Question Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <MessageSquare className="h-5 w-5" />
            Debate Question
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-lg font-medium">{debate.question?.mainQuestion || debate.topic}</p>
          {debate.expectedOutcome && (
            <p className="text-sm text-muted-foreground mt-2">
              <strong>Expected Outcome:</strong> {debate.expectedOutcome}
            </p>
          )}
        </CardContent>
      </Card>

      {/* Results Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid grid-cols-4 w-full">
          <TabsTrigger value="summary">
            <Target className="h-4 w-4 mr-2" />
            Summary
          </TabsTrigger>
          <TabsTrigger value="participants">
            <Users className="h-4 w-4 mr-2" />
            Participants
          </TabsTrigger>
          <TabsTrigger value="consensus">
            <CheckCircle2 className="h-4 w-4 mr-2" />
            Consensus
          </TabsTrigger>
          <TabsTrigger value="statistics">
            <BarChart className="h-4 w-4 mr-2" />
            Statistics
          </TabsTrigger>
        </TabsList>

        {/* Summary Tab */}
        <TabsContent value="summary" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Resolution</CardTitle>
              <CardDescription>
                The overall outcome and key findings from the debate
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="prose dark:prose-invert max-w-none">
                <p className="text-lg leading-relaxed">{resolution}</p>
              </div>
              
              {debate.question?.successCriteria && debate.question.successCriteria.length > 0 && (
                <div className="mt-6">
                  <h4 className="font-semibold mb-3">Success Criteria Assessment</h4>
                  <div className="space-y-2">
                    {debate.question.successCriteria.map((criteria, index) => (
                      <div key={index} className="flex items-center gap-2">
                        <CheckCircle2 className="h-4 w-4 text-green-600" />
                        <span className="text-sm">{criteria}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          <div className="grid md:grid-cols-2 gap-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-green-600">Points of Agreement</CardTitle>
              </CardHeader>
              <CardContent>
                <ul className="space-y-2">
                  {consensusPoints.map((point, index) => (
                    <li key={index} className="flex items-start gap-2">
                      <CheckCircle2 className="h-4 w-4 text-green-600 mt-0.5" />
                      <span className="text-sm">{point.point}</span>
                    </li>
                  ))}
                </ul>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-orange-600">Points of Disagreement</CardTitle>
              </CardHeader>
              <CardContent>
                <ul className="space-y-2">
                  {disagreementPoints.map((point, index) => (
                    <li key={index} className="flex items-start gap-2">
                      <AlertTriangle className="h-4 w-4 text-orange-600 mt-0.5" />
                      <span className="text-sm">{point.point}</span>
                    </li>
                  ))}
                </ul>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        {/* Participants Tab */}
        <TabsContent value="participants" className="space-y-4">
          {participantAnalyses.map((analysis) => (
            <Card key={analysis.participantId}>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle className="flex items-center gap-3">
                    <Avatar className="h-10 w-10 bg-gradient-to-r from-blue-500 to-purple-500" />
                    {analysis.participant.name}
                  </CardTitle>
                  <div className="flex gap-2">
                    <Badge variant="outline">{analysis.participant.llm_config.provider}</Badge>
                    <Badge variant="outline">{analysis.participant.llm_config.model}</Badge>
                  </div>
                </div>
                <CardDescription>{analysis.position}</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="grid md:grid-cols-2 gap-4">
                  <div>
                    <h4 className="font-semibold mb-2">Key Arguments</h4>
                    <ul className="space-y-1">
                      {analysis.keyPoints.map((point, index) => (
                        <li key={index} className="text-sm text-muted-foreground">
                          • {point}
                        </li>
                      ))}
                    </ul>
                  </div>
                  <div>
                    <h4 className="font-semibold mb-2">Final Position</h4>
                    <p className="text-sm text-muted-foreground">
                      {analysis.conclusion || 'No final conclusion provided'}
                    </p>
                  </div>
                </div>
                <div className="mt-4 flex gap-4 text-sm text-muted-foreground">
                  <span>Turns: {analysis.turnCount}</span>
                  <span>Avg. Length: {analysis.avgTurnLength} chars</span>
                </div>
              </CardContent>
            </Card>
          ))}
        </TabsContent>

        {/* Consensus Tab */}
        <TabsContent value="consensus" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Consensus Analysis</CardTitle>
              <CardDescription>
                Areas where participants reached agreement or maintained differences
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h4 className="font-semibold mb-3 flex items-center gap-2">
                  <CheckCircle2 className="h-5 w-5 text-green-600" />
                  Points of Consensus
                </h4>
                <div className="space-y-3">
                  {consensusPoints.map((point, index) => (
                    <div key={index} className="border rounded-lg p-4">
                      <p className="font-medium">{point.point}</p>
                      <div className="mt-2 flex items-center gap-2">
                        <Badge className={getStrengthColor(point.strength)}>
                          {point.strength} consensus
                        </Badge>
                        <span className="text-sm text-muted-foreground">
                          Agreed by: {point.agreedBy.join(', ')}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <Separator />

              <div>
                <h4 className="font-semibold mb-3 flex items-center gap-2">
                  <XCircle className="h-5 w-5 text-orange-600" />
                  Points of Disagreement
                </h4>
                <div className="space-y-3">
                  {disagreementPoints.map((point, index) => (
                    <div key={index} className="border rounded-lg p-4">
                      <p className="font-medium mb-3">{point.point}</p>
                      <div className="space-y-2">
                        {Object.entries(point.positions).map(([participant, position]) => (
                          <div key={participant} className="flex gap-2">
                            <span className="font-medium text-sm">{participant}:</span>
                            <span className="text-sm text-muted-foreground">{position}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Statistics Tab */}
        <TabsContent value="statistics" className="space-y-4">
          <div className="grid md:grid-cols-3 gap-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Total Turns</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-3xl font-bold">{turns.length}</p>
              </CardContent>
            </Card>
            
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Total Rounds</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-3xl font-bold">{debate.currentRound || 0}</p>
              </CardContent>
            </Card>
            
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Duration</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-3xl font-bold">
                  {debate.completedAt && debate.startedAt 
                    ? `${Math.round((new Date(debate.completedAt).getTime() - new Date(debate.startedAt).getTime()) / 60000)} min`
                    : 'N/A'
                  }
                </p>
              </CardContent>
            </Card>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Participation Breakdown</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {participantAnalyses.map((analysis) => {
                  const percentage = (analysis.turnCount / turns.length) * 100;
                  return (
                    <div key={analysis.participantId}>
                      <div className="flex justify-between mb-1">
                        <span className="text-sm font-medium">{analysis.participant.name}</span>
                        <span className="text-sm text-muted-foreground">
                          {analysis.turnCount} turns ({percentage.toFixed(1)}%)
                        </span>
                      </div>
                      <Progress value={percentage} className="h-2" />
                    </div>
                  );
                })}
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}