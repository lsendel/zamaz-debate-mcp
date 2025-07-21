import React from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from '../store';
import { toggleSidebar } from '../store/slices/uiSlice';
import { logout } from '../store/slices/authSlice';
import OrganizationSwitcher from './OrganizationSwitcher';
import { Layout as AntLayout, Menu, Avatar, Dropdown, Typography, Badge } from 'antd';
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

const { Header, Sider, Content } = AntLayout;
const { Text } = Typography;

const Layout: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const location = useLocation();
  const { sidebarOpen } = useAppSelector((state) => state.ui);
  const { user } = useAppSelector((state) => state.auth);

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
          <Text type="secondary">{user?.email || ''}</Text>
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
        width={256}
        style={{
          background: '#fff',
          borderRight: '1px solid #f0f0f0',
        }}
      >
        <div style={{ padding: '16px', borderBottom: '1px solid #f0f0f0' }}>
          <OrganizationSwitcher />
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ borderRight: 0 }}
        />
      </Sider>
      <AntLayout>
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid #f0f0f0',
          }}
        >
          <MenuOutlined
            style={{ fontSize: '20px', cursor: 'pointer' }}
            onClick={() => dispatch(toggleSidebar())}
          />
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <Avatar
              style={{ cursor: 'pointer' }}
              icon={<UserOutlined />}
            />
          </Dropdown>
        </Header>
        <Content style={{ padding: '24px', background: '#f5f5f5' }}>
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  );
};

export default Layout;