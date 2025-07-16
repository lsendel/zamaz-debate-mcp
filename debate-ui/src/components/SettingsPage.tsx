import React, { useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Divider,
  Alert,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Chip,
} from '@mui/material';
import {
  Delete as DeleteIcon,
  Add as AddIcon,
  ContentCopy as ContentCopyIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
} from '@mui/icons-material';
import { useAppSelector, useAppDispatch } from '../store';
import {
  generateApiKey,
  createOrganization,
} from '../store/slices/organizationSlice';
import { addNotification } from '../store/slices/uiSlice';
import organizationClient from '../api/organizationClient';

const SettingsPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { currentOrganization } = useAppSelector((state) => state.organization);
  const { user } = useAppSelector((state) => state.auth);
  
  const [showApiKey, setShowApiKey] = useState(false);
  const [users, setUsers] = useState<any[]>([]);
  const [addUserDialogOpen, setAddUserDialogOpen] = useState(false);
  const [newUser, setNewUser] = useState({
    username: '',
    email: '',
    password: '',
    role: 'member',
  });
  const [orgSettings, setOrgSettings] = useState({
    name: currentOrganization?.name || '',
    description: currentOrganization?.description || '',
  });

  React.useEffect(() => {
    const loadUsers = async () => {
      if (currentOrganization) {
        try {
          const userList = await organizationClient.listUsers();
          setUsers(userList);
        } catch (error) {
          console.error('Failed to load users:', error);
        }
      }
    };
    loadUsers();
  }, [currentOrganization]);

  const handleGenerateApiKey = async () => {
    try {
      await dispatch(generateApiKey()).unwrap();
      dispatch(addNotification({
        type: 'success',
        message: 'API key generated successfully',
      }));
    } catch (error) {
      dispatch(addNotification({
        type: 'error',
        message: 'Failed to generate API key',
      }));
    }
  };

  const handleCopyApiKey = () => {
    if (currentOrganization?.apiKey) {
      navigator.clipboard.writeText(currentOrganization.apiKey);
      dispatch(addNotification({
        type: 'success',
        message: 'API key copied to clipboard',
      }));
    }
  };

  const handleAddUser = async () => {
    try {
      const user = await organizationClient.addUser(newUser);
      setUsers([...users, user]);
      setAddUserDialogOpen(false);
      setNewUser({ username: '', email: '', password: '', role: 'member' });
      dispatch(addNotification({
        type: 'success',
        message: 'User added successfully',
      }));
    } catch (error) {
      dispatch(addNotification({
        type: 'error',
        message: 'Failed to add user',
      }));
    }
  };

  const handleRemoveUser = async (userId: string) => {
    try {
      await organizationClient.removeUser(userId);
      setUsers(users.filter(u => u.id !== userId));
      dispatch(addNotification({
        type: 'success',
        message: 'User removed successfully',
      }));
    } catch (error) {
      dispatch(addNotification({
        type: 'error',
        message: 'Failed to remove user',
      }));
    }
  };

  const handleUpdateOrganization = async () => {
    if (!currentOrganization) return;
    
    try {
      await organizationClient.updateOrganization(currentOrganization.id, orgSettings);
      dispatch(addNotification({
        type: 'success',
        message: 'Organization settings updated',
      }));
    } catch (error) {
      dispatch(addNotification({
        type: 'error',
        message: 'Failed to update organization settings',
      }));
    }
  };

  if (!currentOrganization) {
    return (
      <Box>
        <Typography variant="h4" gutterBottom>
          Settings
        </Typography>
        <Alert severity="warning">No organization selected</Alert>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Settings
      </Typography>

      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          Organization Settings
        </Typography>
        <TextField
          fullWidth
          label="Organization Name"
          value={orgSettings.name}
          onChange={(e) => setOrgSettings({ ...orgSettings, name: e.target.value })}
          margin="normal"
        />
        <TextField
          fullWidth
          label="Description"
          value={orgSettings.description}
          onChange={(e) => setOrgSettings({ ...orgSettings, description: e.target.value })}
          margin="normal"
          multiline
          rows={3}
        />
        <Button
          variant="contained"
          onClick={handleUpdateOrganization}
          sx={{ mt: 2 }}
        >
          Update Settings
        </Button>
      </Paper>

      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          API Configuration
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <TextField
            fullWidth
            label="API Key"
            value={currentOrganization.apiKey || 'No API key generated'}
            type={showApiKey ? 'text' : 'password'}
            InputProps={{
              readOnly: true,
              endAdornment: (
                <>
                  <IconButton
                    onClick={() => setShowApiKey(!showApiKey)}
                    edge="end"
                  >
                    {showApiKey ? <VisibilityOffIcon /> : <VisibilityIcon />}
                  </IconButton>
                  {currentOrganization.apiKey && (
                    <IconButton onClick={handleCopyApiKey} edge="end">
                      <ContentCopyIcon />
                    </IconButton>
                  )}
                </>
              ),
            }}
          />
          <Button
            variant="outlined"
            onClick={handleGenerateApiKey}
          >
            {currentOrganization.apiKey ? 'Regenerate' : 'Generate'}
          </Button>
        </Box>
        {currentOrganization.apiKey && (
          <Alert severity="warning" sx={{ mt: 2 }}>
            Keep your API key secure. It provides full access to your organization's resources.
          </Alert>
        )}
      </Paper>

      <Paper sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            Users
          </Typography>
          <Button
            startIcon={<AddIcon />}
            variant="outlined"
            onClick={() => setAddUserDialogOpen(true)}
          >
            Add User
          </Button>
        </Box>
        <Divider sx={{ mb: 2 }} />
        <List>
          {users.map((u) => (
            <ListItem key={u.id}>
              <ListItemText
                primary={u.username}
                secondary={u.email}
              />
              <Chip
                label={u.role}
                size="small"
                color={u.role === 'admin' ? 'primary' : 'default'}
                sx={{ mr: 2 }}
              />
              <ListItemSecondaryAction>
                {u.id !== user?.id && (
                  <IconButton
                    edge="end"
                    onClick={() => handleRemoveUser(u.id)}
                    color="error"
                  >
                    <DeleteIcon />
                  </IconButton>
                )}
              </ListItemSecondaryAction>
            </ListItem>
          ))}
        </List>
      </Paper>

      <Dialog
        open={addUserDialogOpen}
        onClose={() => setAddUserDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Add User</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Username"
            value={newUser.username}
            onChange={(e) => setNewUser({ ...newUser, username: e.target.value })}
            margin="normal"
          />
          <TextField
            fullWidth
            label="Email"
            type="email"
            value={newUser.email}
            onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
            margin="normal"
          />
          <TextField
            fullWidth
            label="Password"
            type="password"
            value={newUser.password}
            onChange={(e) => setNewUser({ ...newUser, password: e.target.value })}
            margin="normal"
          />
          <TextField
            fullWidth
            select
            label="Role"
            value={newUser.role}
            onChange={(e) => setNewUser({ ...newUser, role: e.target.value })}
            margin="normal"
            SelectProps={{
              native: true,
            }}
          >
            <option value="member">Member</option>
            <option value="admin">Admin</option>
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddUserDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleAddUser}
            variant="contained"
            disabled={!newUser.username || !newUser.email || !newUser.password}
          >
            Add User
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SettingsPage;