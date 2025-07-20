import * as React from 'react';
import { clsx } from 'clsx';

export interface TextareaProps
  extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  error?: boolean;
  fullWidth?: boolean;
}

const Textarea = React.forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, error, fullWidth, ...props }, ref) => {
    return (
      <textarea
        className={clsx(
          'flex min-h-[120px] w-full rounded-md border bg-white px-3 py-2 text-base',
          'placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2',
          'disabled:cursor-not-allowed disabled:opacity-50',
          'transition-all duration-150',
          'resize-vertical',
          error
            ? 'border-red-500 focus:ring-red-500'
            : 'border-gray-300 hover:border-gray-400 focus:border-primary-500 focus:ring-primary-500',
          fullWidth && 'w-full',
          className
        )}
        ref={ref}
        {...props}
      />
    );
  }
);

Textarea.displayName = 'Textarea';

export { Textarea };