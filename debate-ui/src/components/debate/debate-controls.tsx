'use client';

import { Button } from '@/components/ui/button';
import { Play, Pause, SkipForward, Sparkles, Loader2 } from 'lucide-react';
import { Debate } from '@/types/debate';

interface DebateControlsProps {
  debate: Debate;
  isLoading: boolean;
  currentSpeaker: string | null;
  onStart: () => void;
  onPause: () => void;
  onResume: () => void;
  onSkipTurn: () => void;
}

export function DebateControls({ 
  debate, 
  isLoading, 
  currentSpeaker,
  onStart, 
  onPause, 
  onResume, 
  onSkipTurn 
}: DebateControlsProps) {
  return (
    <div className="space-y-4">
      {/* Control Buttons */}
      <div className="flex items-center gap-3">
        {debate.status === 'draft' && (
          <Button onClick={onStart} className="bg-emerald-600 hover:bg-emerald-700 text-white">
            <Play className="h-4 w-4 mr-2" />
            Start Debate
          </Button>
        )}
        
        {debate.status === 'active' && (
          <>
            <Button onClick={onPause} variant="outline">
              <Pause className="h-4 w-4 mr-2" />
              Pause
            </Button>
            <Button onClick={onSkipTurn} variant="outline" disabled={isLoading}>
              <SkipForward className="h-4 w-4 mr-2" />
              Skip Turn
            </Button>
          </>
        )}
        
        {debate.status === 'paused' && (
          <Button onClick={onResume} className="bg-amber-600 hover:bg-amber-700 text-white">
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
        <div className="p-3 rounded-lg bg-gradient-to-r from-blue-50 to-purple-50 dark:from-blue-950 dark:to-purple-950 flex items-center gap-3">
          <Loader2 className="h-4 w-4 animate-spin text-blue-600" />
          <span className="text-sm font-medium">
            {currentSpeaker} is thinking...
          </span>
        </div>
      )}
    </div>
  );
}