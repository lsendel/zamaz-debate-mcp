import React from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from '../store';
import { toggleSidebar } from '../store/slices/uiSlice';
import { logout } from '../store/slices/authSlice';
import OrganizationSwitcher from './OrganizationSwitcher';
import {
  MenuOutlined,
  MessageOutlined,
  BranchesOutlined,
  BarChartOutlined,
  SettingOutlined,
  LogoutOutlined,
  BankOutlined,
  SafetyOutlined,
  UserOutlined,
} from '@ant-design/icons';
import {
  Layout as AntLayout,
  Menu,
  Typography,
  Button,
  Space,
  Badge,
  Avatar,
  Dropdown,
} from 'antd';
import { colors, spacing, componentSpacing } from '../styles';

const { Header, Sider, Content } = AntLayout;
const { Text } = Typography;

const Layout: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const location = useLocation();
  const { sidebarOpen } = useAppSelector(state => state.ui);
  const { user } = useAppSelector(state => state.auth);

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const menuItems = [
    {
      key: '/',
      icon: <MessageOutlined />,
      label: 'Debates',
    },
    {
      key: '/workflow',
      icon: <BranchesOutlined />,
      label: 'Workflow Editor',
    },
    {
      key: '/analytics',
      icon: <BarChartOutlined />,
      label: 'Analytics',
    },
    {
      key: 'admin',
      icon: <SafetyOutlined />,
      label: 'Admin',
      children: [
        {
          key: '/organization-management',
          icon: <BankOutlined />,
          label: 'Organizations',
          disabled: user?.role !== 'ADMIN',
        },
        {
          key: '/settings',
          icon: <SettingOutlined />,
          label: 'Settings',
        },
      ],
    },
  ];

  const userMenuItems = [
    {
      key: 'profile',
      label: (
        <div>
          <Text strong>{user?.username || 'User'}</Text>
          <br />
          <Text style={{ color: colors.text.secondary }}>{user?.email || ''}</Text>
        </div>
      ),
      disabled: true,
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Logout',
      onClick: handleLogout,
    },
  ];

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={!sidebarOpen}
        width={componentSpacing.sidebarWidth}
        style={{
          background: colors.background.primary,
          borderRight: `1px solid ${colors.border.light}`,
        }}
      >
        <div style={{ padding: spacing[4], borderBottom: `1px solid ${colors.border.light}` }}>
          <OrganizationSwitcher />
        </div>
        <Menu
          mode='inline'
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{
            borderRight: 0,
            fontSize: 15,
            fontWeight: 500,
          }}
        />
      </Sider>
      <AntLayout>
        <Header
          style={{
            background: colors.background.primary,
            padding: `0 ${spacing[6]}px`,
            height: componentSpacing.headerHeight,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: `1px solid ${colors.border.light}`,
            boxShadow: '0 2px 4px rgba(0, 0, 0, 0.02)',
          }}
        >
          <MenuOutlined
            style={{
              fontSize: 24,
              cursor: 'pointer',
              color: colors.text.primary,
              padding: spacing[2],
              margin: -spacing[2],
            }}
            onClick={() => dispatch(toggleSidebar())}
          />
          <Dropdown menu={{ items: userMenuItems }} placement='bottomRight'>
            <Avatar style={{ cursor: 'pointer' }} icon={<UserOutlined />} />
          </Dropdown>
        </Header>
        <Content
          style={{
            padding: componentSpacing.pageMargin,
            background: colors.background.secondary,
            minHeight: `calc(100vh - ${componentSpacing.headerHeight}px)`,
          }}
        >
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  );
};

export default Layout;
