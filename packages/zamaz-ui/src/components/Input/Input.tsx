import * as React from 'react';
import { clsx } from 'clsx';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  error?: boolean;
  fullWidth?: boolean;
}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, error, fullWidth, type, ...props }, ref) => {
    return (
      <input
        type={type}
        className={clsx(
          'flex h-11 rounded-md border bg-white px-3 py-2 text-base',
          'placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2',
          'disabled:cursor-not-allowed disabled:opacity-50',
          'transition-all duration-150',
          'file:border-0 file:bg-transparent file:text-sm file:font-medium',
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

Input.displayName = 'Input';

export { Input };