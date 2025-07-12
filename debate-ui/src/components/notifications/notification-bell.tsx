'use client';

import { useState } from 'react';
import { Bell, BellOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { useNotifications, useNotificationPermission } from '@/contexts/notification-context';
import { NotificationList } from './notification-list';

export function NotificationBell() {
  const { unreadCount, markAllAsRead } = useNotifications();
  const { permission, requestPermission } = useNotificationPermission();
  const [isOpen, setIsOpen] = useState(false);

  const handleOpenChange = (open: boolean) => {
    setIsOpen(open);
    if (!open && unreadCount > 0) {
      // Mark all as read when closing the popover
      setTimeout(() => markAllAsRead(), 300);
    }
  };

  const handleEnableNotifications = async () => {
    await requestPermission();
  };

  return (
    <Popover open={isOpen} onOpenChange={handleOpenChange}>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="relative">
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <Badge
              variant="destructive"
              className="absolute -top-1 -right-1 h-5 w-5 p-0 flex items-center justify-center text-xs"
            >
              {unreadCount > 9 ? '9+' : unreadCount}
            </Badge>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-96 p-0" align="end">
        <div className="flex items-center justify-between p-4 border-b">
          <h3 className="font-semibold">Notifications</h3>
          {permission === 'default' && (
            <Button
              variant="outline"
              size="sm"
              onClick={handleEnableNotifications}
            >
              Enable
            </Button>
          )}
          {permission === 'denied' && (
            <div className="flex items-center gap-1 text-sm text-muted-foreground">
              <BellOff className="h-4 w-4" />
              Disabled
            </div>
          )}
        </div>
        <NotificationList />
      </PopoverContent>
    </Popover>
  );
}