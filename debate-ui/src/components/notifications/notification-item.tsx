'use client';

import { formatDistanceToNow } from 'date-fns';
import { Button } from '@/components/ui/button';
import { useNotifications, Notification, NotificationType } from '@/contexts/notification-context';
import {
  Info,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  MessageSquare,
  X
} from 'lucide-react';

const notificationIcons: Record<NotificationType, React.ReactNode> = {
  info: <Info className="h-4 w-4" />,
  success: <CheckCircle2 className="h-4 w-4" />,
  warning: <AlertTriangle className="h-4 w-4" />,
  error: <XCircle className="h-4 w-4" />,
  debate: <MessageSquare className="h-4 w-4" />
};

const notificationColors: Record<NotificationType, string> = {
  info: 'text-blue-600 bg-blue-50 dark:text-blue-400 dark:bg-blue-950',
  success: 'text-green-600 bg-green-50 dark:text-green-400 dark:bg-green-950',
  warning: 'text-amber-600 bg-amber-50 dark:text-amber-400 dark:bg-amber-950',
  error: 'text-red-600 bg-red-50 dark:text-red-400 dark:bg-red-950',
  debate: 'text-purple-600 bg-purple-50 dark:text-purple-400 dark:bg-purple-950'
};

interface NotificationItemProps {
  notification: Notification;
}

export function NotificationItem({ notification }: NotificationItemProps) {
  const { markAsRead, removeNotification } = useNotifications();

  const handleClick = () => {
    if (!notification.read) {
      markAsRead(notification.id);
    }
    if (notification.action) {
      notification.action.onClick();
    }
  };

  const handleRemove = (e: React.MouseEvent) => {
    e.stopPropagation();
    removeNotification(notification.id);
  };

  return (
    <div
      className={`relative p-3 rounded-lg mb-2 cursor-pointer transition-colors hover:bg-muted/50 ${
        !notification.read ? 'bg-muted/20' : ''
      }`}
      onClick={handleClick}
    >
      <div className="flex items-start gap-3">
        <div className={`p-2 rounded-full ${notificationColors[notification.type]}`}>
          {notificationIcons[notification.type]}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <p className="font-medium text-sm">{notification.title}</p>
              {notification.message && (
                <p className="text-sm text-muted-foreground mt-1">
                  {notification.message}
                </p>
              )}
              {notification.action && (
                <Button
                  variant="link"
                  size="sm"
                  className="p-0 h-auto mt-2 text-xs"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleClick();
                  }}
                >
                  {notification.action.label}
                </Button>
              )}
            </div>
            <Button
              variant="ghost"
              size="icon"
              className="h-6 w-6 -mr-1"
              onClick={handleRemove}
            >
              <X className="h-3 w-3" />
            </Button>
          </div>
          <p className="text-xs text-muted-foreground mt-2">
            {formatDistanceToNow(notification.timestamp, { addSuffix: true })}
          </p>
        </div>
      </div>
      {!notification.read && (
        <div className="absolute top-3 right-3 h-2 w-2 bg-blue-600 rounded-full" />
      )}
    </div>
  );
}