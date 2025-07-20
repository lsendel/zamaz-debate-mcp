import React from 'react';
import { cn } from '../../utils/cn';

export interface AvatarProps {
  src?: string;
  alt?: string;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  children?: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}

export const Avatar: React.FC<AvatarProps> = ({
  src,
  alt = '',
  size = 'md',
  children,
  className,
  style,
}) => {
  const sizeClasses = {
    xs: 'w-6 h-6 text-xs',
    sm: 'w-8 h-8 text-sm',
    md: 'w-10 h-10 text-base',
    lg: 'w-12 h-12 text-lg',
    xl: 'w-16 h-16 text-xl',
  };

  return (
    <div
      className={cn(
        'relative inline-flex items-center justify-center rounded-full bg-gray-300 font-medium text-white overflow-hidden',
        sizeClasses[size],
        className
      )}
      style={style}
    >
      {src ? (
        <img
          src={src}
          alt={alt}
          className="w-full h-full object-cover"
        />
      ) : (
        children
      )}
    </div>
  );
};

export interface AvatarGroupProps {
  children: React.ReactNode;
  max?: number;
  spacing?: 'tight' | 'normal' | 'loose';
  className?: string;
}

export const AvatarGroup: React.FC<AvatarGroupProps> = ({
  children,
  max = 3,
  spacing = 'normal',
  className,
}) => {
  const childrenArray = React.Children.toArray(children);
  const visibleChildren = max ? childrenArray.slice(0, max) : childrenArray;
  const remainingCount = childrenArray.length - visibleChildren.length;

  const spacingClasses = {
    tight: '-space-x-2',
    normal: '-space-x-3',
    loose: '-space-x-4',
  };

  return (
    <div className={cn('flex', spacingClasses[spacing], className)}>
      {visibleChildren.map((child, index) => (
        <div key={index} className="relative ring-2 ring-white rounded-full">
          {child}
        </div>
      ))}
      {remainingCount > 0 && (
        <div className="relative ring-2 ring-white rounded-full">
          <Avatar size="md" className="bg-gray-400">
            +{remainingCount}
          </Avatar>
        </div>
      )}
    </div>
  );
};