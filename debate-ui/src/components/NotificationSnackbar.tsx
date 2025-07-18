import React, { useEffect } from "react";
import { Snackbar, Alert } from "@mui/material";
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

  if (!currentNotification) {
    return null;
  }

  return (
    <Snackbar
      open={true}
      anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      onClose={() => dispatch(removeNotification(currentNotification.id))}
    >
      <Alert
        onClose={() => dispatch(removeNotification(currentNotification.id))}
        severity={currentNotification.type}
        sx={{ width: "100%" }}
      >
        {currentNotification.message}
      </Alert>
    </Snackbar>
  );
};

export default NotificationSnackbar;
