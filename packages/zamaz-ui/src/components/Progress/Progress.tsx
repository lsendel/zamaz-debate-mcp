import React from 'react';
import { cn } from '../../utils/cn';

export interface ProgressProps {
  value: number;
  max?: number;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'default' | 'primary' | 'success' | 'warning' | 'error';
  showLabel?: boolean;
  className?: string;
}

export const Progress: React.FC<ProgressProps> = ({
  value,
  max = 100,
  size = 'md',
  variant = 'primary',
  showLabel = false,
  className,
}) => {
  const percentage = Math.min(Math.max((value / max) * 100, 0), 100);

  const sizeClasses = {
    sm: 'h-1',
    md: 'h-2',
    lg: 'h-3',
  };

  const variantClasses = {
    default: 'bg-gray-500',
    primary: 'bg-primary-500',
    success: 'bg-green-500',
    warning: 'bg-yellow-500',
    error: 'bg-red-500',
  };

  return (
    <div className={cn('w-full', className)}>
      <div className="flex items-center gap-2">
        <div className="flex-1">
          <div
            className={cn(
              'w-full bg-gray-200 rounded-full overflow-hidden',
              sizeClasses[size]
            )}
          >
            <div
              className={cn(
                'h-full transition-all duration-300 ease-out rounded-full',
                variantClasses[variant]
              )}
              style={{ width: `${percentage}%` }}
            />
          </div>
        </div>
        {showLabel && (
          <span className="text-sm font-medium text-gray-700 whitespace-nowrap">
            {Math.round(percentage)}%
          </span>
        )}
      </div>
    </div>
  );
};

export interface CircularProgressProps {
  value?: number;
  size?: number;
  strokeWidth?: number;
  variant?: 'determinate' | 'indeterminate';
  className?: string;
}

export const CircularProgress: React.FC<CircularProgressProps> = ({
  value = 0,
  size = 40,
  strokeWidth = 4,
  variant = 'indeterminate',
  className,
}) => {
  const radius = (size - strokeWidth) / 2;
  const circumference = radius * 2 * Math.PI;
  const offset = circumference - (value / 100) * circumference;

  return (
    <svg
      className={cn(
        variant === 'indeterminate' && 'animate-spin',
        className
      )}
      width={size}
      height={size}
    >
      <circle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        fill="none"
        stroke="currentColor"
        strokeWidth={strokeWidth}
        className="text-gray-200"
      />
      <circle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        fill="none"
        stroke="currentColor"
        strokeWidth={strokeWidth}
        strokeDasharray={circumference}
        strokeDashoffset={variant === 'determinate' ? offset : circumference * 0.75}
        className="text-primary-500 transition-all duration-300"
        style={{
          transform: 'rotate(-90deg)',
          transformOrigin: '50% 50%',
        }}
      />
    </svg>
  );
};