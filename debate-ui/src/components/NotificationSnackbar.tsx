import React, { useEffect } from "react";
import { Toast, ToastClose, ToastDescription, ToastProvider, ToastViewport } from "@zamaz/ui";
import { useAppSelector, useAppDispatch } from "../store";
import { removeNotification } from "../store/slices/uiSlice";

const NotificationSnackbar: React.FC = () => {
  const dispatch = useAppDispatch();
  const { notifications } = useAppSelector((state) => state.ui);
  const currentNotification = notifications[0];

  useEffect(() => {
    if (currentNotification) {
      const timer = setTimeout(() => {
        dispatch(removeNotification(currentNotification.id));
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [currentNotification, dispatch]);

  const getToastVariant = (type: string) => {
    switch (type) {
      case 'success':
        return 'success';
      case 'error':
        return 'error';
      case 'warning':
        return 'warning';
      case 'info':
      default:
        return 'default';
    }
  };

  return (
    <ToastProvider>
      {currentNotification && (
        <Toast
          open={true}
          onOpenChange={(open) => {
            if (!open) {
              dispatch(removeNotification(currentNotification.id));
            }
          }}
          variant={getToastVariant(currentNotification.type)}
        >
          <ToastDescription>{currentNotification.message}</ToastDescription>
          <ToastClose />
        </Toast>
      )}
      <ToastViewport />
    </ToastProvider>
  );
};

export default NotificationSnackbar;
