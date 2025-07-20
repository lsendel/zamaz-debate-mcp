import React from 'react';
import {
  Progress,
  CircularProgress,
  Card,
  CardContent,
  Badge,
  Alert,
  Stepper,
  Step,
  StepLabel,
  StepContent,
} from '@zamaz/ui';
import {
  CheckCircle,
  Circle,
  Loader2,
  AlertCircle,
} from 'lucide-react';
import { Debate } from '../api/debateClient';

interface DebateProgressProps {
  debate: Debate;
  isPolling?: boolean;
}

const DebateProgress: React.FC<DebateProgressProps> = ({ debate, isPolling }) => {
  const getRoundStatus = (roundNumber: number) => {
    const round = debate.rounds?.find(r => r.roundNumber === roundNumber);
    if (!round) return 'pending';
    if (round.status === 'completed') return 'completed';
    if (round.status === 'in_progress') return 'in_progress';
    return 'pending';
  };

  const getStepIcon = (roundNumber: number) => {
    const status = getRoundStatus(roundNumber);
    switch (status) {
      case 'completed':
        return <CheckCircle className="w-5 h-5 text-green-500" />;
      case 'in_progress':
        return <CircularProgress size={20} variant="indeterminate" />;
      default:
        return <Circle className="w-5 h-5 text-gray-400" />;
    }
  };

  const getProgressPercentage = () => {
    if (!debate.maxRounds || debate.maxRounds === 0) return 0;
    const completedRounds = debate.rounds?.filter(r => r.status === 'completed').length || 0;
    return (completedRounds / debate.maxRounds) * 100;
  };

  const isDebateActive = debate.status === 'IN_PROGRESS' || debate.status === 'CREATED';
  const isGenerating = debate.status === 'IN_PROGRESS' && debate.rounds?.some(r => r.status === 'in_progress');

  return (
    <div className="mb-6">
      {/* Status Header */}
      <div className="flex items-center mb-4 gap-4">
        <h2 className="text-xl font-semibold">Debate Progress</h2>
        {isPolling && (
          <Badge variant="success" className="animate-pulse">
            <CircularProgress size={14} variant="indeterminate" className="mr-1" />
            Live
          </Badge>
        )}
        {isGenerating && (
          <p className="text-sm text-gray-500">
            Generating responses...
          </p>
        )}
      </div>

      {/* Progress Bar */}
      <Card className="mb-4">
        <CardContent className="p-4">
          <div className="flex items-center mb-2">
            <div className="flex-1 mr-2">
              <Progress 
                value={getProgressPercentage()} 
                variant="primary"
                size="lg"
              />
            </div>
            <div className="min-w-[45px]">
              <span className="text-sm font-medium text-gray-700">
                {Math.round(getProgressPercentage())}%
              </span>
            </div>
          </div>
          <p className="text-sm text-gray-500">
            {debate.rounds?.filter(r => r.status === 'completed').length || 0} of {debate.maxRounds || 0} rounds completed
          </p>
        </CardContent>
      </Card>

      {/* Round Steps */}
      <Card variant="outlined">
        <CardContent className="p-6">
          <div className="space-y-6">
            {Array.from({ length: debate.maxRounds || 3 }, (_, i) => i + 1).map((roundNumber) => {
              const round = debate.rounds?.find(r => r.roundNumber === roundNumber);
              const status = getRoundStatus(roundNumber);
              const isActive = debate.currentRound === roundNumber;
              
              return (
                <div key={roundNumber} className="relative">
                  {/* Connector Line */}
                  {roundNumber < (debate.maxRounds || 3) && (
                    <div 
                      className={`absolute top-10 left-5 bottom-0 w-0.5 ${
                        status === 'completed' ? 'bg-primary-500' : 'bg-gray-200'
                      }`} 
                    />
                  )}
                  
                  {/* Step */}
                  <div className="relative z-10 flex">
                    {/* Step Icon */}
                    <div
                      className={`w-10 h-10 rounded-full flex items-center justify-center font-medium transition-all ${
                        status === 'completed'
                          ? 'bg-primary-500 text-white'
                          : isActive
                          ? 'bg-primary-100 text-primary-700 border-2 border-primary-500'
                          : 'bg-gray-100 text-gray-400 border-2 border-gray-200'
                      }`}
                    >
                      {getStepIcon(roundNumber)}
                    </div>

                    {/* Content */}
                    <div className="ml-4 flex-1">
                      <div className={`font-medium ${isActive ? 'text-gray-900' : 'text-gray-500'}`}>
                        Round {roundNumber}
                      </div>
                      {status === 'in_progress' && (
                        <p className="text-sm text-primary-600 mt-1">
                          Generating responses...
                        </p>
                      )}
                      {round && round.responses && round.responses.length > 0 && (
                        <div className="ml-2 mt-2">
                          <p className="text-sm text-gray-500">
                            {round.responses.length} response{round.responses.length !== 1 ? 's' : ''} generated
                          </p>
                        </div>
                      )}
                      {status === 'in_progress' && (
                        <Alert variant="info" className="mt-2">
                          AI participants are formulating their responses...
                        </Alert>
                      )}
                      {status === 'pending' && debate.status === 'IN_PROGRESS' && (
                        <p className="text-sm text-gray-500 mt-2">
                          Waiting to start...
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>

      {/* Status Messages */}
      {debate.status === 'CREATED' && (
        <Alert variant="info" className="mt-4">
          Click "Start" to begin the debate. AI participants will generate responses for each round.
        </Alert>
      )}
      
      {debate.status === 'COMPLETED' && (
        <Alert variant="success" className="mt-4">
          Debate completed! All rounds have been generated.
        </Alert>
      )}
      
      {debate.status === 'CANCELLED' && (
        <Alert variant="warning" className="mt-4">
          This debate was cancelled.
        </Alert>
      )}
    </div>
  );
};

export default DebateProgress;