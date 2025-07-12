'use client';

import { ScrollArea } from '@/components/ui/scroll-area';
import { Button } from '@/components/ui/button';
import { useNotifications } from '@/contexts/notification-context';
import { NotificationItem } from './notification-item';
import { Inbox, Trash2 } from 'lucide-react';

export function NotificationList() {
  const { notifications, clearAll } = useNotifications();

  if (notifications.length === 0) {
    return (
      <div className="p-8 text-center">
        <Inbox className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
        <p className="text-sm text-muted-foreground">No notifications yet</p>
      </div>
    );
  }

  return (
    <>
      <ScrollArea className="h-[400px]">
        <div className="p-2">
          {notifications.map((notification) => (
            <NotificationItem
              key={notification.id}
              notification={notification}
            />
          ))}
        </div>
      </ScrollArea>
      {notifications.length > 0 && (
        <div className="p-2 border-t">
          <Button
            variant="ghost"
            size="sm"
            className="w-full"
            onClick={clearAll}
          >
            <Trash2 className="h-4 w-4 mr-2" />
            Clear all
          </Button>
        </div>
      )}
    </>
  );
}