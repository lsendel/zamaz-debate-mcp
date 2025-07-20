import * as React from 'react';
import { clsx } from 'clsx';

export interface NavItem {
  label: string;
  href: string;
  icon?: React.ComponentType<{ className?: string }>;
  isActive?: boolean;
  onClick?: () => void;
}

export interface NavigationProps {
  items: NavItem[];
  logo?: React.ReactNode;
  className?: string;
}

const Navigation: React.FC<NavigationProps> = ({ items, logo, className }) => {
  return (
    <nav className={clsx('w-64 bg-white border-r border-gray-200 h-full dark:bg-gray-900 dark:border-gray-800', className)}>
      {logo && (
        <div className="p-4 border-b border-gray-200 dark:border-gray-800">
          {logo}
        </div>
      )}
      <ul className="space-y-1 p-2">
        {items.map((item) => {
          const Icon = item.icon;
          return (
            <li key={item.href}>
              <a
                href={item.href}
                onClick={item.onClick}
                className={clsx(
                  'flex items-center gap-3 px-3 py-2 rounded-md transition-colors',
                  'hover:bg-gray-100 dark:hover:bg-gray-800',
                  item.isActive
                    ? 'bg-primary-50 text-primary-700 font-medium dark:bg-primary-900/20 dark:text-primary-400'
                    : 'text-gray-700 dark:text-gray-300'
                )}
              >
                {Icon && <Icon className="h-5 w-5" />}
                <span>{item.label}</span>
              </a>
            </li>
          );
        })}
      </ul>
    </nav>
  );
};

const NavigationHeader: React.FC<{ children: React.ReactNode; className?: string }> = ({ 
  children, 
  className 
}) => (
  <div className={clsx('px-4 py-6 border-b border-gray-200 dark:border-gray-800', className)}>
    {children}
  </div>
);

const NavigationSection: React.FC<{ 
  title?: string; 
  children: React.ReactNode; 
  className?: string 
}> = ({ title, children, className }) => (
  <div className={clsx('py-4', className)}>
    {title && (
      <h3 className="px-4 mb-2 text-xs font-semibold text-gray-500 uppercase tracking-wider">
        {title}
      </h3>
    )}
    {children}
  </div>
);

export { Navigation, NavigationHeader, NavigationSection };