import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store';
import { login, register, clearError } from '../store/slices/authSlice';
import { Button, Input, Card, Alert, Form, Typography, Tabs } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, TeamOutlined } from '@ant-design/icons';
import { colors, spacing, typography as typographyStyles, shadows, borderRadius } from '../styles';

const { Title, Text } = Typography;
const { TabPane } = Tabs;

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { loading, error } = useAppSelector((state) => state.auth);

  const [loginForm] = Form.useForm();
  const [registerForm] = Form.useForm();
  const [isLogin, setIsLogin] = useState(true);

  const handleTabChange = (key: string) => {
    setIsLogin(key === 'login');
    dispatch(clearError());
  };

  const handleLogin = async (values: any) => {
    const result = await dispatch(login({ username: values.username, password: values.password }));
    if (login.fulfilled.match(result)) {
      navigate('/');
    }
  };

  const handleRegister = async (values: any) => {
    const result = await dispatch(register(values));
    if (register.fulfilled.match(result)) {
      navigate('/');
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      background: `linear-gradient(135deg, ${colors.primary[50]} 0%, ${colors.primary[100]} 100%)`,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: `${spacing[12]}px ${spacing[4]}px`
    }}>
      <div style={{ width: '100%', maxWidth: '448px' }}>
        <div style={{ textAlign: 'center', marginBottom: spacing[8] }}>
          <Title 
            level={1} 
            style={{ 
              fontSize: typographyStyles.fontSize['4xl'],
              fontWeight: typographyStyles.fontWeight.semibold,
              marginBottom: spacing[2],
              color: colors.text.primary
            }}
          >
            Zamaz Debate System
          </Title>
          <Text style={{ 
            fontSize: typographyStyles.fontSize.lg,
            color: colors.text.secondary 
          }}>
            Welcome back
          </Text>
        </div>

        <Card 
          style={{ 
            boxShadow: shadows.lg,
            borderRadius: borderRadius.card,
            border: 'none'
          }}
        >
          <Tabs 
            defaultActiveKey="login" 
            onChange={handleTabChange}
            centered
            size="large"
          >
            <TabPane tab="Login" key="login">
              {error && (
                <Alert
                  message={error}
                  type="error"
                  showIcon
                  closable
                  style={{ marginBottom: spacing[4] }}
                  onClose={() => dispatch(clearError())}
                />
              )}

              {process.env.NODE_ENV === 'development' && (
                <Alert
                  message="Development Mode"
                  description={
                    <>
                      Username: <code style={{ 
                        backgroundColor: colors.background.tertiary, 
                        padding: `${spacing[1]}px ${spacing[2]}px`, 
                        borderRadius: borderRadius.sm,
                        color: colors.primary[600],
                        fontSize: typographyStyles.fontSize.sm,
                        fontFamily: typographyStyles.fontFamily.mono
                      }}>demo</code>
                      <br />
                      Password: <code style={{ 
                        backgroundColor: colors.background.tertiary, 
                        padding: `${spacing[1]}px ${spacing[2]}px`, 
                        borderRadius: borderRadius.sm,
                        color: colors.primary[600],
                        fontSize: typographyStyles.fontSize.sm,
                        fontFamily: typographyStyles.fontFamily.mono
                      }}>demo123</code>
                    </>
                  }
                  type="info"
                  showIcon
                  style={{ marginBottom: spacing[4] }}
                />
              )}

              <Form
                form={loginForm}
                onFinish={handleLogin}
                layout="vertical"
                autoComplete="off"
                size="large"
              >
                <Form.Item
                  label={<span style={{ 
                    fontSize: typographyStyles.fontSize.base,
                    fontWeight: typographyStyles.fontWeight.medium,
                    color: colors.text.primary
                  }}>Username</span>}
                  name="username"
                  rules={[{ required: true, message: 'Please input your username!' }]}
                >
                  <Input 
                    prefix={<UserOutlined style={{ color: colors.text.tertiary }} />} 
                    placeholder="Enter your username"
                    size="large"
                    autoFocus
                    style={{ fontSize: typographyStyles.fontSize.base }}
                  />
                </Form.Item>

                <Form.Item
                  label={<span style={{ 
                    fontSize: typographyStyles.fontSize.base,
                    fontWeight: typographyStyles.fontWeight.medium,
                    color: colors.text.primary
                  }}>Password</span>}
                  name="password"
                  rules={[{ required: true, message: 'Please input your password!' }]}
                >
                  <Input.Password
                    prefix={<LockOutlined style={{ color: colors.text.tertiary }} />}
                    placeholder="Enter your password"
                    size="large"
                    style={{ fontSize: typographyStyles.fontSize.base }}
                  />
                </Form.Item>

                <Form.Item style={{ marginTop: spacing[6] }}>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                    size="large"
                    block
                    style={{ 
                      height: 48,
                      fontSize: typographyStyles.fontSize.base,
                      fontWeight: typographyStyles.fontWeight.medium 
                    }}
                  >
                    {loading ? 'Logging in...' : 'Login'}
                  </Button>
                </Form.Item>
              </Form>
            </TabPane>

            <TabPane tab="Register" key="register">
              {error && (
                <Alert
                  message={error}
                  type="error"
                  showIcon
                  closable
                  style={{ marginBottom: spacing[4] }}
                  onClose={() => dispatch(clearError())}
                />
              )}

              <Form
                form={registerForm}
                onFinish={handleRegister}
                layout="vertical"
                autoComplete="off"
                size="large"
              >
                <Form.Item
                  label={<span style={{ 
                    fontSize: typographyStyles.fontSize.base,
                    fontWeight: typographyStyles.fontWeight.medium,
                    color: colors.text.primary
                  }}>Organization Name</span>}
                  name="organizationName"
                  rules={[{ required: true, message: 'Please input your organization name!' }]}
                >
                  <Input
                    prefix={<TeamOutlined style={{ color: colors.text.tertiary }} />}
                    placeholder="Enter your organization name"
                    size="large"
                    autoFocus
                    style={{ fontSize: typographyStyles.fontSize.base }}
                  />
                </Form.Item>

                <Form.Item
                  label={<span style={{ 
                    fontSize: typographyStyles.fontSize.base,
                    fontWeight: typographyStyles.fontWeight.medium,
                    color: colors.text.primary
                  }}>Username</span>}
                  name="username"
                  rules={[{ required: true, message: 'Please input your username!' }]}
                >
                  <Input
                    prefix={<UserOutlined style={{ color: colors.text.tertiary }} />}
                    placeholder="Choose a username"
                    size="large"
                    style={{ fontSize: typographyStyles.fontSize.base }}
                  />
                </Form.Item>

                <Form.Item
                  label={<span style={{ 
                    fontSize: typographyStyles.fontSize.base,
                    fontWeight: typographyStyles.fontWeight.medium,
                    color: colors.text.primary
                  }}>Email</span>}
                  name="email"
                  rules={[
                    { required: true, message: 'Please input your email!' },
                    { type: 'email', message: 'Please enter a valid email!' }
                  ]}
                >
                  <Input
                    prefix={<MailOutlined style={{ color: colors.text.tertiary }} />}
                    placeholder="Enter your email address"
                    size="large"
                    style={{ fontSize: typographyStyles.fontSize.base }}
                  />
                </Form.Item>

                <Form.Item
                  label={<span style={{ 
                    fontSize: typographyStyles.fontSize.base,
                    fontWeight: typographyStyles.fontWeight.medium,
                    color: colors.text.primary
                  }}>Password</span>}
                  name="password"
                  rules={[
                    { required: true, message: 'Please input your password!' },
                    { min: 6, message: 'Password must be at least 6 characters!' }
                  ]}
                >
                  <Input.Password
                    prefix={<LockOutlined style={{ color: colors.text.tertiary }} />}
                    placeholder="Create a password"
                    size="large"
                    style={{ fontSize: typographyStyles.fontSize.base }}
                  />
                </Form.Item>

                <Form.Item style={{ marginTop: spacing[6] }}>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                    size="large"
                    block
                    style={{ 
                      height: 48,
                      fontSize: typographyStyles.fontSize.base,
                      fontWeight: typographyStyles.fontWeight.medium 
                    }}
                  >
                    {loading ? 'Creating organization...' : 'Register'}
                  </Button>
                </Form.Item>
              </Form>
            </TabPane>
          </Tabs>
        </Card>
      </div>
    </div>
  );
};

export default LoginPage;