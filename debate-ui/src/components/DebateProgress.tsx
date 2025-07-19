import React from 'react';
import {
  Box,
  LinearProgress,
  Typography,
  Chip,
  Card,
  CardContent,
  Stepper,
  Step,
  StepLabel,
  StepContent,
  CircularProgress,
  Alert,
  Paper
} from '@mui/material';
import {
  CheckCircle as CheckIcon,
  RadioButtonUnchecked as PendingIcon,
  Loop as InProgressIcon,
  Error as ErrorIcon
} from '@mui/icons-material';
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
        return <CheckIcon color="success" />;
      case 'in_progress':
        return <CircularProgress size={20} />;
      default:
        return <PendingIcon color="disabled" />;
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
    <Box sx={{ mb: 3 }}>
      {/* Status Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2, gap: 2 }}>
        <Typography variant="h6">Debate Progress</Typography>
        {isPolling && (
          <Chip
            label="Live"
            color="success"
            size="small"
            icon={<CircularProgress size={14} sx={{ color: 'white !important' }} />}
            sx={{
              animation: 'pulse 2s infinite',
              '@keyframes pulse': {
                '0%': { opacity: 1 },
                '50%': { opacity: 0.7 },
                '100%': { opacity: 1 },
              },
            }}
          />
        )}
        {isGenerating && (
          <Typography variant="body2" color="text.secondary">
            Generating responses...
          </Typography>
        )}
      </Box>

      {/* Progress Bar */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
          <Box sx={{ width: '100%', mr: 1 }}>
            <LinearProgress 
              variant="determinate" 
              value={getProgressPercentage()} 
              sx={{ height: 8, borderRadius: 4 }}
            />
          </Box>
          <Box sx={{ minWidth: 35 }}>
            <Typography variant="body2" color="text.secondary">
              {Math.round(getProgressPercentage())}%
            </Typography>
          </Box>
        </Box>
        <Typography variant="caption" color="text.secondary">
          {debate.rounds?.filter(r => r.status === 'completed').length || 0} of {debate.maxRounds || 0} rounds completed
        </Typography>
      </Paper>

      {/* Round Steps */}
      <Card variant="outlined">
        <CardContent>
          <Stepper orientation="vertical" activeStep={debate.currentRound ? debate.currentRound - 1 : 0}>
            {Array.from({ length: debate.maxRounds || 3 }, (_, i) => i + 1).map((roundNumber) => {
              const round = debate.rounds?.find(r => r.roundNumber === roundNumber);
              const status = getRoundStatus(roundNumber);
              
              return (
                <Step key={roundNumber} completed={status === 'completed'}>
                  <StepLabel
                    StepIconComponent={() => getStepIcon(roundNumber)}
                    optional={
                      status === 'in_progress' && (
                        <Typography variant="caption" color="primary">
                          Generating responses...
                        </Typography>
                      )
                    }
                  >
                    Round {roundNumber}
                  </StepLabel>
                  <StepContent>
                    {round && round.responses && round.responses.length > 0 && (
                      <Box sx={{ ml: 2, mt: 1 }}>
                        <Typography variant="caption" color="text.secondary">
                          {round.responses.length} response{round.responses.length !== 1 ? 's' : ''} generated
                        </Typography>
                      </Box>
                    )}
                    {status === 'in_progress' && (
                      <Alert severity="info" sx={{ mt: 1 }}>
                        AI participants are formulating their responses...
                      </Alert>
                    )}
                    {status === 'pending' && debate.status === 'IN_PROGRESS' && (
                      <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                        Waiting to start...
                      </Typography>
                    )}
                  </StepContent>
                </Step>
              );
            })}
          </Stepper>
        </CardContent>
      </Card>

      {/* Status Messages */}
      {debate.status === 'CREATED' && (
        <Alert severity="info" sx={{ mt: 2 }}>
          Click "Start" to begin the debate. AI participants will generate responses for each round.
        </Alert>
      )}
      
      {debate.status === 'COMPLETED' && (
        <Alert severity="success" sx={{ mt: 2 }}>
          Debate completed! All rounds have been generated.
        </Alert>
      )}
      
      {debate.status === 'CANCELLED' && (
        <Alert severity="warning" sx={{ mt: 2 }}>
          This debate was cancelled.
        </Alert>
      )}
    </Box>
  );
};

export default DebateProgress;