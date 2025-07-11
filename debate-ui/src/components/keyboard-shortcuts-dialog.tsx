'use client';

import { useEffect, useState } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Keyboard } from 'lucide-react';
import { DEFAULT_SHORTCUTS } from '@/hooks/use-keyboard-shortcuts';

export function KeyboardShortcutsDialog() {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const handleShowShortcuts = () => setOpen(true);
    window.addEventListener('showKeyboardShortcuts', handleShowShortcuts);
    return () => window.removeEventListener('showKeyboardShortcuts', handleShowShortcuts);
  }, []);

  const formatKey = (shortcut: typeof DEFAULT_SHORTCUTS[0]) => {
    const keys = [];
    if (shortcut.ctrl) keys.push('Ctrl');
    if (shortcut.shift) keys.push('Shift');
    if (shortcut.alt) keys.push('Alt');
    keys.push(shortcut.key.toUpperCase());
    return keys;
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <div className="flex items-center gap-3">
            <div className="inline-flex items-center justify-center w-10 h-10 rounded-lg bg-gradient-to-br from-blue-500 to-purple-500 text-white">
              <Keyboard className="h-5 w-5" />
            </div>
            <div>
              <DialogTitle>Keyboard Shortcuts</DialogTitle>
              <DialogDescription>
                Quick actions to navigate and control the debate system
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>
        
        <div className="space-y-4 mt-4">
          {DEFAULT_SHORTCUTS.map((shortcut, index) => (
            <div key={index} className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
              <span className="text-sm font-medium">{shortcut.description}</span>
              <div className="flex gap-1">
                {formatKey(shortcut).map((key, i) => (
                  <Badge key={i} variant="secondary" className="font-mono">
                    {key}
                  </Badge>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="mt-6 p-4 rounded-lg bg-blue-50 dark:bg-blue-950/50 text-sm">
          <p className="font-medium text-blue-900 dark:text-blue-100 mb-1">Pro tip:</p>
          <p className="text-blue-700 dark:text-blue-300">
            Press <Badge variant="secondary" className="font-mono mx-1">Ctrl + /</Badge> at any time to show this dialog
          </p>
        </div>
      </DialogContent>
    </Dialog>
  );
}