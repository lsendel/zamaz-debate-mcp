import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store';
import { login, register, clearError } from '../store/slices/authSlice';
import {
  Button,
  Input,
  FormField,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Alert,
  AlertDescription,
} from '@zamaz/ui';

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { loading, error } = useAppSelector((state) => state.auth);

  const [isLogin, setIsLogin] = useState(true);
  const [loginForm, setLoginForm] = useState({ username: '', password: '' });
  const [registerForm, setRegisterForm] = useState({
    username: '',
    email: '',
    password: '',
    organizationName: '',
  });

  const handleTabChange = (showLogin: boolean) => {
    setIsLogin(showLogin);
    dispatch(clearError());
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    const result = await dispatch(login(loginForm));
    if (login.fulfilled.match(result)) {
      navigate('/');
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    const result = await dispatch(register(registerForm));
    if (register.fulfilled.match(result)) {
      navigate('/');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <h1 className="text-4xl font-bold text-gray-900">Zamaz Debate System</h1>
          <p className="mt-2 text-gray-600">Welcome back</p>
        </div>

        <Card className="mt-8">
          <CardHeader>
            <div className="flex border-b border-gray-200">
              <button
                className={`flex-1 py-3 text-center font-medium transition-colors ${
                  isLogin
                    ? 'text-primary-600 border-b-2 border-primary-600'
                    : 'text-gray-500 hover:text-gray-700'
                }`}
                onClick={() => handleTabChange(true)}
              >
                Login
              </button>
              <button
                className={`flex-1 py-3 text-center font-medium transition-colors ${
                  !isLogin
                    ? 'text-primary-600 border-b-2 border-primary-600'
                    : 'text-gray-500 hover:text-gray-700'
                }`}
                onClick={() => handleTabChange(false)}
              >
                Register
              </button>
            </div>
          </CardHeader>

          <CardContent className="p-6">
            {error && (
              <Alert variant="error" className="mb-4">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            {isLogin ? (
              <form onSubmit={handleLogin} className="space-y-4">
                {process.env.NODE_ENV === 'development' && (
                  <Alert variant="info" className="mb-4">
                    <AlertDescription>
                      <strong>Development Mode</strong>
                      <br />
                      Username: <code className="bg-gray-100 px-1 rounded">demo</code>
                      <br />
                      Password: <code className="bg-gray-100 px-1 rounded">demo123</code>
                    </AlertDescription>
                  </Alert>
                )}

                <FormField label="Username" required>
                  <Input
                    type="text"
                    autoComplete="username"
                    autoFocus
                    value={loginForm.username}
                    onChange={(e) =>
                      setLoginForm({ ...loginForm, username: e.target.value })
                    }
                    fullWidth
                  />
                </FormField>

                <FormField label="Password" required>
                  <Input
                    type="password"
                    autoComplete="current-password"
                    value={loginForm.password}
                    onChange={(e) =>
                      setLoginForm({ ...loginForm, password: e.target.value })
                    }
                    fullWidth
                  />
                </FormField>

                <Button
                  type="submit"
                  fullWidth
                  loading={loading}
                  disabled={loading}
                  className="mt-6"
                >
                  {loading ? 'Logging in...' : 'Login'}
                </Button>
              </form>
            ) : (
              <form onSubmit={handleRegister} className="space-y-4">
                <FormField label="Organization Name" required>
                  <Input
                    type="text"
                    autoFocus
                    value={registerForm.organizationName}
                    onChange={(e) =>
                      setRegisterForm({
                        ...registerForm,
                        organizationName: e.target.value,
                      })
                    }
                    fullWidth
                  />
                </FormField>

                <FormField label="Username" required>
                  <Input
                    type="text"
                    autoComplete="username"
                    value={registerForm.username}
                    onChange={(e) =>
                      setRegisterForm({ ...registerForm, username: e.target.value })
                    }
                    fullWidth
                  />
                </FormField>

                <FormField label="Email" required>
                  <Input
                    type="email"
                    autoComplete="email"
                    value={registerForm.email}
                    onChange={(e) =>
                      setRegisterForm({ ...registerForm, email: e.target.value })
                    }
                    fullWidth
                  />
                </FormField>

                <FormField label="Password" required>
                  <Input
                    type="password"
                    autoComplete="new-password"
                    value={registerForm.password}
                    onChange={(e) =>
                      setRegisterForm({ ...registerForm, password: e.target.value })
                    }
                    fullWidth
                  />
                </FormField>

                <Button
                  type="submit"
                  fullWidth
                  loading={loading}
                  disabled={loading}
                  className="mt-6"
                >
                  {loading ? 'Creating account...' : 'Register'}
                </Button>
              </form>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default LoginPage;