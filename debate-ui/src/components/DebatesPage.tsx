import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from '../store';
import {
  fetchDebates,
  startDebate,
  pauseDebate,
  cancelDebate,
} from '../store/slices/debateSlice';
import { openCreateDebateDialog } from '../store/slices/uiSlice';
import CreateDebateDialog from './CreateDebateDialog';
import {
  Button,
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
  CardDescription,
  Badge,
} from '@zamaz/ui';
import { Plus, Play, Pause, StopCircle, RefreshCw } from 'lucide-react';

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

  const getStatusVariant = (status: string) => {
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

  const handleDebateAction = async (
    e: React.MouseEvent,
    debateId: string,
    action: string
  ) => {
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
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
      </div>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Debates</h1>
        <div className="flex gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={handleRefresh}
            leftIcon={<RefreshCw className="h-4 w-4" />}
          >
            Refresh
          </Button>
          <Button
            variant="primary"
            onClick={() => dispatch(openCreateDebateDialog())}
            leftIcon={<Plus className="h-4 w-4" />}
          >
            Create Debate
          </Button>
        </div>
      </div>

      {debates.length === 0 ? (
        <Card className="text-center p-12">
          <CardContent>
            <h2 className="text-xl font-semibold text-gray-900 mb-2">
              No debates yet
            </h2>
            <p className="text-gray-600 mb-6">
              Create your first debate to get started
            </p>
            <Button
              variant="primary"
              onClick={() => dispatch(openCreateDebateDialog())}
              leftIcon={<Plus className="h-4 w-4" />}
            >
              Create Debate
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {debates.map((debate) => (
            <Card
              key={debate.id}
              variant="interactive"
              onClick={() => navigate(`/debates/${debate.id}`)}
              className="overflow-hidden"
            >
              <CardHeader>
                <CardTitle className="line-clamp-1">{debate.topic}</CardTitle>
                <CardDescription className="line-clamp-2">
                  {debate.description || 'No description'}
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="flex flex-wrap gap-2 mb-3">
                  <Badge variant={getStatusVariant(debate.status)}>
                    {debate.status.replace('_', ' ')}
                  </Badge>
                  <Badge variant="outline">
                    Round {debate.currentRound}/{debate.maxRounds}
                  </Badge>
                </div>
                <p className="text-sm text-gray-600">
                  {debate.participants.length} participants
                </p>
              </CardContent>
              <CardFooter className="bg-gray-50 border-t">
                <div className="flex gap-2">
                  {debate.status === 'created' && (
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={(e) => handleDebateAction(e, debate.id, 'start')}
                      leftIcon={<Play className="h-4 w-4" />}
                    >
                      Start
                    </Button>
                  )}
                  {debate.status === 'in_progress' && (
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={(e) => handleDebateAction(e, debate.id, 'pause')}
                      leftIcon={<Pause className="h-4 w-4" />}
                    >
                      Pause
                    </Button>
                  )}
                  {(debate.status === 'created' ||
                    debate.status === 'in_progress') && (
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={(e) => handleDebateAction(e, debate.id, 'cancel')}
                      leftIcon={<StopCircle className="h-4 w-4" />}
                      className="text-red-600 hover:text-red-700 hover:bg-red-50"
                    >
                      Cancel
                    </Button>
                  )}
                </div>
              </CardFooter>
            </Card>
          ))}
        </div>
      )}

      <CreateDebateDialog />
    </div>
  );
};

export default DebatesPage;