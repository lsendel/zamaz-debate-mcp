// TODO: Refactor to reduce cognitive complexity (SonarCloud S3776)
// Consider breaking down complex functions into smaller, more focused functions

import { useEffect, useRef } from 'react';
import { useAppDispatch } from '../store';
import { fetchDebate } from '../store/slices/debateSlice';
import { Debate } from '../api/debateClient';

interface UseDebatePollingOptions {
  debateId: string;
  enabled: boolean;
  interval?: number;
  onUpdate?: (debate: Debate) => void;
}

export const useDebatePolling = ({
  debateId,
  enabled,
  interval = 2000,
  onUpdate,
}: UseDebatePollingOptions) => {
  const dispatch = useAppDispatch();
  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const isPollingRef = useRef(false);
  const onUpdateRef = useRef(onUpdate);

  // Update the ref when onUpdate changes
  useEffect(() => {
    onUpdateRef.current = onUpdate;
  }, [onUpdate]);

  useEffect(() => {
    if (!enabled || !debateId) {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
        isPollingRef.current = false;
      }
      return;
    }

    const pollDebate = async () => {
      if (isPollingRef.current) return; // Prevent overlapping requests

      isPollingRef.current = true;
      try {
        const action = await dispatch(fetchDebate(debateId));
        if (fetchDebate.fulfilled.match(action)) {
          const debate = action.payload;

          // Call onUpdate callback if provided
          if (onUpdateRef.current) {
            onUpdateRef.current(debate);
          }

          // Stop polling if debate is completed or cancelled
          if (debate.status === 'COMPLETED' || debate.status === 'CANCELLED') {
            if (intervalRef.current) {
              clearInterval(intervalRef.current);
              intervalRef.current = null;
            }
          }
        }
      } finally {
        isPollingRef.current = false;
      }
    };

    // Initial poll
    pollDebate();

    // Set up interval polling
    intervalRef.current = setInterval(pollDebate, interval);

    // Cleanup
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
        isPollingRef.current = false;
      }
    };
  }, [debateId, enabled, interval, dispatch]);

  return {
    isPolling: isPollingRef.current,
  };
};
