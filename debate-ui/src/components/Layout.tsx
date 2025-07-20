import React from 'react';
import { Outlet } from 'react-router-dom';
import { useNavigate } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from '../store';
import { toggleSidebar } from '../store/slices/uiSlice';
import { logout } from '../store/slices/authSlice';
import OrganizationSwitcher from './OrganizationSwitcher';
import { Navigation, NavigationHeader, NavigationSection, Badge } from '@zamaz/ui';
import {
  Menu,
  MessageSquare,
  GitBranch,
  BarChart3,
  Settings,
  LogOut,
  Building2,
  ShieldCheck,
} from 'lucide-react';

const Layout: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { sidebarOpen } = useAppSelector((state) => state.ui);
  const { user } = useAppSelector((state) => state.auth);

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  // Check if user is admin
  const isAdmin = user?.role === 'admin' || user?.role === 'superadmin';

  const menuItems = [
    { text: 'Debates', icon: MessageSquare, path: '/debates' },
    { text: 'Workflow Editor', icon: GitBranch, path: '/workflow-editor' },
    { text: 'Analytics', icon: BarChart3, path: '/analytics' },
    { text: 'Settings', icon: Settings, path: '/settings' },
  ];

  const adminMenuItems = [
    { text: 'Organization Management', icon: Building2, path: '/organization-management' },
  ];

  const navItems = menuItems.map(item => ({
    label: item.text,
    href: item.path,
    icon: item.icon,
    onClick: () => navigate(item.path),
  }));

  const adminNavItems = adminMenuItems.map(item => ({
    label: item.text,
    href: item.path,
    icon: item.icon,
    onClick: () => navigate(item.path),
  }));

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Header */}
      <header className={`fixed top-0 right-0 left-0 z-30 bg-white border-b border-gray-200 transition-all duration-300 ${
        sidebarOpen ? 'md:left-64' : ''
      }`}>
        <div className="flex items-center justify-between h-16 px-4">
          <div className="flex items-center">
            <button
              onClick={() => dispatch(toggleSidebar())}
              className="p-2 rounded-md hover:bg-gray-100 transition-colors"
              aria-label="Toggle sidebar"
            >
              <Menu className="h-5 w-5 text-gray-600" />
            </button>
            <h1 className="ml-4 text-xl font-semibold text-gray-900">
              Zamaz Debate System
            </h1>
          </div>
          
          {user && (
            <div className="flex items-center space-x-3">
              {isAdmin && (
                <ShieldCheck className="h-5 w-5 text-yellow-500" />
              )}
              <span className="text-sm text-gray-700">
                {user.username}
              </span>
              {isAdmin && (
                <Badge variant="warning" className="text-xs">
                  ADMIN
                </Badge>
              )}
            </div>
          )}
        </div>
      </header>

      {/* Sidebar */}
      <aside className={`fixed left-0 top-0 bottom-0 w-64 bg-white border-r border-gray-200 transform transition-transform duration-300 z-20 ${
        sidebarOpen ? 'translate-x-0' : '-translate-x-full'
      }`}>
        <div className="h-16 border-b border-gray-200" /> {/* Spacer for header */}
        
        <div className="flex flex-col h-full">
          <NavigationHeader>
            <OrganizationSwitcher />
          </NavigationHeader>

          <nav className="flex-1 overflow-y-auto">
            <NavigationSection>
              <ul className="space-y-1 px-2">
                {navItems.map((item) => (
                  <li key={item.href}>
                    <button
                      onClick={item.onClick}
                      className="w-full flex items-center gap-3 px-3 py-2 rounded-md transition-colors hover:bg-gray-100 text-gray-700 hover:text-gray-900"
                    >
                      {item.icon && <item.icon className="h-5 w-5" />}
                      <span>{item.label}</span>
                    </button>
                  </li>
                ))}
              </ul>
            </NavigationSection>

            {isAdmin && (
              <NavigationSection title="Administration">
                <ul className="space-y-1 px-2">
                  {adminNavItems.map((item) => (
                    <li key={item.href}>
                      <button
                        onClick={item.onClick}
                        className="w-full flex items-center gap-3 px-3 py-2 rounded-md transition-colors hover:bg-gray-100 text-gray-700 hover:text-gray-900"
                      >
                        {item.icon && <item.icon className="h-5 w-5" />}
                        <span>{item.label}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              </NavigationSection>
            )}
          </nav>

          <div className="border-t border-gray-200 p-4">
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-3 px-3 py-2 rounded-md transition-colors hover:bg-gray-100 text-gray-700 hover:text-gray-900"
            >
              <LogOut className="h-5 w-5" />
              <span>Logout</span>
            </button>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className={`flex-1 transition-all duration-300 ${
        sidebarOpen ? 'md:ml-64' : ''
      }`}>
        <div className="h-16" /> {/* Spacer for fixed header */}
        <div className="p-6">
          <Outlet />
        </div>
      </main>
    </div>
  );
};

export default Layout;