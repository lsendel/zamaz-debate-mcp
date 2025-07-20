import React from 'react';
import { cn } from '../../utils/cn';

export interface DividerProps {
  orientation?: 'horizontal' | 'vertical';
  variant?: 'solid' | 'dashed' | 'dotted';
  className?: string;
  children?: React.ReactNode;
}

export const Divider: React.FC<DividerProps> = ({
  orientation = 'horizontal',
  variant = 'solid',
  className,
  children,
}) => {
  const baseClasses = cn(
    'bg-gray-200',
    variant === 'dashed' && 'border-dashed',
    variant === 'dotted' && 'border-dotted'
  );

  if (children) {
    return (
      <div
        className={cn(
          'relative flex items-center',
          orientation === 'vertical' && 'flex-col h-full',
          className
        )}
      >
        <div
          className={cn(
            baseClasses,
            orientation === 'horizontal' ? 'flex-1 h-px' : 'flex-1 w-px'
          )}
        />
        <span
          className={cn(
            'px-3 text-sm text-gray-500',
            orientation === 'vertical' && 'py-3 px-0'
          )}
        >
          {children}
        </span>
        <div
          className={cn(
            baseClasses,
            orientation === 'horizontal' ? 'flex-1 h-px' : 'flex-1 w-px'
          )}
        />
      </div>
    );
  }

  return (
    <div
      className={cn(
        baseClasses,
        orientation === 'horizontal' ? 'w-full h-px' : 'h-full w-px',
        className
      )}
    />
  );
};