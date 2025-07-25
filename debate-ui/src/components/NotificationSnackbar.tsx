import React, { useEffect } from 'react';
import { notification } from 'antd';
import { useAppSelector, useAppDispatch } from '../store';
import { removeNotification } from '../store/slices/uiSlice';

const NotificationSnackbar: React.FC = () => {
  const dispatch = useAppDispatch();
  const { notifications } = useAppSelector(state => state.ui);
  const currentNotification = notifications[0];

  useEffect(() => {
    if (currentNotification) {
      const { type, message, id } = currentNotification;

      // Show notification using Ant Design
      notification[type as keyof typeof notification]({
        message: type.charAt(0).toUpperCase() + type.slice(1),
        description: message,
        placement: 'topRight',
        duration: 5,
        onClose: () => {
          dispatch(removeNotification(id));
        },
      });

      // Also remove from Redux store after 5 seconds
      const timer = setTimeout(() => {
        dispatch(removeNotification(id));
      }, 5000);

      return () => clearTimeout(timer);
    }
  }, [currentNotification, dispatch]);

  // This component doesn't render anything visible itself
  // It just manages showing Ant Design notifications based on Redux state
  return null;
};

export default NotificationSnackbar;
