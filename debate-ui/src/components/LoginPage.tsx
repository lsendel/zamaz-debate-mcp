import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store';
import { login, register, clearError } from '../store/slices/authSlice';
import { Button, Input, Card, Alert, Form, Typography, Tabs } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, TeamOutlined } from '@ant-design/icons';

const { Title } = Typography;
const { TabPane } = Tabs;

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { loading, error } = useAppSelector((state) => state.auth);

//   const [isLogin, setIsLogin] = useState(true); // SonarCloud: removed useless assignment
  const [loginForm] = Form.useForm();
  const [registerForm] = Form.useForm();

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
    <div className="min-h-screen bg-gradient-to-br from-primary-50 to-primary-100 flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <Title level={1}>Zamaz Debate System</Title>
          <p className="mt-2 text-gray-600">Welcome back</p>
        </div>

        <Card>
          <Tabs 
            defaultActiveKey="login" 
            onChange={handleTabChange}
            centered
          >
            <TabPane tab="Login" key="login">
              {error && (
                <Alert
                  message={error}
                  type="error"
                  showIcon
                  closable
                  className="mb-4"
                  onClose={() => dispatch(clearError())}
                />
              )}

              {process.env.NODE_ENV === 'development' && (
                <Alert
                  message="Development Mode"
                  description={
                    <>
                      Username: <code style={{ backgroundColor: '#f0f0f0', padding: '2px 4px', borderRadius: '2px' }}>demo</code>
                      <br />
                      Password: <code style={{ backgroundColor: '#f0f0f0', padding: '2px 4px', borderRadius: '2px' }}>demo123</code>
                    </>
                  }
                  type="info"
                  showIcon
                  className="mb-4"
                />
              )}

              <Form
                form={loginForm}
                onFinish={handleLogin}
                layout="vertical"
                autoComplete="off"
              >
                <Form.Item
                  label="Username"
                  name="username"
                  rules={[{ required: true, message: 'Please input your username!' }]}
                >
                  <Input 
                    prefix={<UserOutlined />} 
                    placeholder="Username"
                    size="large"
                    autoFocus
                  />
                </Form.Item>

                <Form.Item
                  label="Password"
                  name="password"
                  rules={[{ required: true, message: 'Please input your password!' }]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="Password"
                    size="large"
                  />
                </Form.Item>

                <Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                    size="large"
                    block
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
                  className="mb-4"
                  onClose={() => dispatch(clearError())}
                />
              )}

              <Form
                form={registerForm}
                onFinish={handleRegister}
                layout="vertical"
                autoComplete="off"
              >
                <Form.Item
                  label="Organization Name"
                  name="organizationName"
                  rules={[{ required: true, message: 'Please input your organization name!' }]}
                >
                  <Input
                    prefix={<TeamOutlined />}
                    placeholder="Organization Name"
                    size="large"
                    autoFocus
                  />
                </Form.Item>

                <Form.Item
                  label="Username"
                  name="username"
                  rules={[{ required: true, message: 'Please input your username!' }]}
                >
                  <Input
                    prefix={<UserOutlined />}
                    placeholder="Username"
                    size="large"
                  />
                </Form.Item>

                <Form.Item
                  label="Email"
                  name="email"
                  rules={[
                    { required: true, message: 'Please input your email!' },
                    { type: 'email', message: 'Please enter a valid email!' }
                  ]}
                >
                  <Input
                    prefix={<MailOutlined />}
                    placeholder="Email"
                    size="large"
                  />
                </Form.Item>

                <Form.Item
                  label="Password"
                  name="password"
                  rules={[
                    { required: true, message: 'Please input your password!' },
                    { min: 6, message: 'Password must be at least 6 characters!' }
                  ]}
                >
                  <Input.Password
                    prefix={<LockOutlined />}
                    placeholder="Password"
                    size="large"
                  />
                </Form.Item>

                <Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                    size="large"
                    block
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