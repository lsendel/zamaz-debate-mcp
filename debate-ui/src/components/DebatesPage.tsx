import React, { useEffect } from 'react';
import {
  Box,
  Button,
  Paper,
  Typography,
  Grid,
  Card,
  CardContent,
  CardActions,
  Chip,
  IconButton,
  Tooltip,
  LinearProgress,
} from '@mui/material';
import {
  Add as AddIcon,
  PlayArrow as PlayIcon,
  Pause as PauseIcon,
  Stop as StopIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from '../store';
import { fetchDebates, startDebate, pauseDebate, cancelDebate } from '../store/slices/debateSlice';
import { openCreateDebateDialog } from '../store/slices/uiSlice';
import CreateDebateDialog from './CreateDebateDialog';

const DebatesPage: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { debates, loading } = useAppSelector((state) => state.debate);
  const { currentOrganization } = useAppSelector((state) => state.organization);

  useEffect(() => {
    if (currentOrganization) {
      dispatch(fetchDebates());
    }
  }, [dispatch, currentOrganization]);

  const handleRefresh = () => {
    dispatch(fetchDebates());
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'created':
        return 'default';
      case 'in_progress':
        return 'primary';
      case 'completed':
        return 'success';
      case 'cancelled':
        return 'error';
      default:
        return 'default';
    }
  };

  const handleDebateAction = async (e: React.MouseEvent, debateId: string, action: string) => {
    e.stopPropagation();
    switch (action) {
      case 'start':
        await dispatch(startDebate(debateId));
        break;
      case 'pause':
        await dispatch(pauseDebate(debateId));
        break;
      case 'cancel':
        await dispatch(cancelDebate(debateId));
        break;
    }
  };

  if (loading) {
    return <LinearProgress />;
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4" component="h1">
          Debates
        </Typography>
        <Box>
          <Tooltip title="Refresh">
            <IconButton onClick={handleRefresh} sx={{ mr: 1 }}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => dispatch(openCreateDebateDialog())}
          >
            Create Debate
          </Button>
        </Box>
      </Box>

      {debates.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h6" color="text.secondary" gutterBottom>
            No debates yet
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Create your first debate to get started
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => dispatch(openCreateDebateDialog())}
          >
            Create Debate
          </Button>
        </Paper>
      ) : (
        <Grid container spacing={3}>
          {debates.map((debate) => (
            <Grid item xs={12} md={6} lg={4} key={debate.id}>
              <Card
                sx={{
                  cursor: 'pointer',
                  '&:hover': {
                    boxShadow: 3,
                  },
                }}
                onClick={() => navigate(`/debates/${debate.id}`)}
              >
                <CardContent>
                  <Typography variant="h6" gutterBottom noWrap>
                    {debate.topic}
                  </Typography>
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{
                      mb: 2,
                      height: 40,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical',
                    }}
                  >
                    {debate.description || 'No description'}
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
                    <Chip
                      label={debate.status.replace('_', ' ')}
                      size="small"
                      color={getStatusColor(debate.status) as any}
                    />
                    <Chip
                      label={`Round ${debate.currentRound}/${debate.maxRounds}`}
                      size="small"
                      variant="outlined"
                    />
                  </Box>
                  <Typography variant="caption" color="text.secondary">
                    {debate.participants.length} participants
                  </Typography>
                </CardContent>
                <CardActions>
                  {debate.status === 'created' && (
                    <IconButton
                      size="small"
                      color="primary"
                      onClick={(e) => handleDebateAction(e, debate.id, 'start')}
                    >
                      <PlayIcon />
                    </IconButton>
                  )}
                  {debate.status === 'in_progress' && (
                    <IconButton
                      size="small"
                      color="primary"
                      onClick={(e) => handleDebateAction(e, debate.id, 'pause')}
                    >
                      <PauseIcon />
                    </IconButton>
                  )}
                  {(debate.status === 'created' || debate.status === 'in_progress') && (
                    <IconButton
                      size="small"
                      color="error"
                      onClick={(e) => handleDebateAction(e, debate.id, 'cancel')}
                    >
                      <StopIcon />
                    </IconButton>
                  )}
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <CreateDebateDialog />
    </Box>
  );
};

export default DebatesPage;