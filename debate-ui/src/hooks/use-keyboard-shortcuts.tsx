'use client';

import { useEffect } from 'react';

interface ShortcutHandler {
  key: string;
  ctrl?: boolean;
  shift?: boolean;
  alt?: boolean;
  handler: () => void;
  description: string;
}

export function useKeyboardShortcuts(shortcuts: ShortcutHandler[]) {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // Don't trigger shortcuts when typing in inputs
      if (
        event.target instanceof HTMLInputElement ||
        event.target instanceof HTMLTextAreaElement ||
        event.target instanceof HTMLSelectElement
      ) {
        return;
      }

      shortcuts.forEach(shortcut => {
        const ctrlPressed = shortcut.ctrl ? (event.ctrlKey || event.metaKey) : true;
        const shiftPressed = shortcut.shift ? event.shiftKey : !event.shiftKey;
        const altPressed = shortcut.alt ? event.altKey : !event.altKey;

        if (
          event.key.toLowerCase() === shortcut.key.toLowerCase() &&
          ctrlPressed &&
          shiftPressed &&
          altPressed
        ) {
          event.preventDefault();
          shortcut.handler();
        }
      });
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [shortcuts]);
}

export const DEFAULT_SHORTCUTS: ShortcutHandler[] = [
  {
    key: 'n',
    ctrl: true,
    description: 'Create new debate',
    handler: () => {
      const newDebateButton = document.querySelector('button:has(svg + span:contains("New Debate"))') as HTMLElement;
      if (newDebateButton) newDebateButton.click();
    }
  },
  {
    key: 'o',
    ctrl: true,
    description: 'Switch organization',
    handler: () => {
      const orgSwitcher = document.querySelector('[aria-expanded]') as HTMLElement;
      if (orgSwitcher) orgSwitcher.click();
    }
  },
  {
    key: 'h',
    ctrl: true,
    description: 'View history',
    handler: () => {
      const orgSwitcher = document.querySelector('[aria-expanded]') as HTMLElement;
      if (orgSwitcher) {
        orgSwitcher.click();
        setTimeout(() => {
          const historyButton = Array.from(document.querySelectorAll('button')).find(
            btn => btn.textContent?.includes('View History')
          ) as HTMLElement;
          if (historyButton) historyButton.click();
        }, 100);
      }
    }
  },
  {
    key: '/',
    ctrl: true,
    description: 'Show keyboard shortcuts',
    handler: () => {
      const event = new CustomEvent('showKeyboardShortcuts');
      window.dispatchEvent(event);
    }
  }
];