import React, { useState, useEffect } from "react";
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  IconButton,
  Chip,
  Menu,
  MenuItem,
  Alert,
  Tabs,
  Tab,
  Grid,
  Card,
  CardContent,
  CardActions,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
} from "@mui/material";
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  MoreVert as MoreVertIcon,
  Business as BusinessIcon,
  People as PeopleIcon,
  Key as KeyIcon,
  Settings as SettingsIcon,
} from "@mui/icons-material";
import { useAppSelector, useAppDispatch } from "../store";
import {
  fetchOrganizations,
  createOrganization,
  generateApiKey,
} from "../store/slices/organizationSlice";
import { addNotification } from "../store/slices/uiSlice";
import organizationClient, {
  Organization,
  User,
} from "../api/organizationClient";

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`org-tabpanel-${index}`}
      aria-labelledby={`org-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const OrganizationManagementPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { organizations, currentOrganization, loading } = useAppSelector(
    (state) => state.organization,
  );
  const { user } = useAppSelector((state) => state.auth);

  const [tabValue, setTabValue] = useState(0);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [selectedOrg, setSelectedOrg] = useState<Organization | null>(null);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [addUserDialogOpen, setAddUserDialogOpen] = useState(false);

  const [newOrgForm, setNewOrgForm] = useState({
    name: "",
    description: "",
  });

  const [newUserForm, setNewUserForm] = useState({
    username: "",
    email: "",
    password: "",
    role: "member",
  });

  // Check if user is admin
  const isAdmin = user?.role === "admin" || user?.role === "superadmin";

  useEffect(() => {
    dispatch(fetchOrganizations());
  }, [dispatch]);

  useEffect(() => {
    if (currentOrganization) {
      loadUsers();
    }
  }, [currentOrganization]);

  const loadUsers = async () => {
    try {
      const userList = await organizationClient.listUsers();
      setUsers(userList);
    } catch (error) {
      console.error("Failed to load users:", error);
      // Mock users for development
      setUsers([
        {
          id: "user-1",
          username: "demo",
          email: "demo@example.com",
          organizationId: currentOrganization?.id || "",
          role: "admin",
          createdAt: "2024-01-01T00:00:00Z",
        },
        {
          id: "user-2",
          username: "john",
          email: "john@example.com",
          organizationId: currentOrganization?.id || "",
          role: "member",
          createdAt: "2024-01-02T00:00:00Z",
        },
      ]);
    }
  };

  const handleCreateOrg = async () => {
    try {
      await dispatch(createOrganization(newOrgForm));
      dispatch(
        addNotification({
          type: "success",
          message: "Organization created successfully",
        }),
      );
      setCreateDialogOpen(false);
      setNewOrgForm({ name: "", description: "" });
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message: "Failed to create organization",
        }),
      );
    }
  };

  const handleGenerateApiKey = async () => {
    try {
      await dispatch(generateApiKey());
      dispatch(
        addNotification({
          type: "success",
          message: "API key generated successfully",
        }),
      );
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message: "Failed to generate API key",
        }),
      );
    }
  };

  const handleAddUser = async () => {
    try {
      await organizationClient.addUser(newUserForm);
      dispatch(
        addNotification({
          type: "success",
          message: "User added successfully",
        }),
      );
      setAddUserDialogOpen(false);
      setNewUserForm({ username: "", email: "", password: "", role: "member" });
      loadUsers();
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message: "Failed to add user",
        }),
      );
    }
  };

  const handleMenuClick = (
    event: React.MouseEvent<HTMLElement>,
    org: Organization,
  ) => {
    setAnchorEl(event.currentTarget);
    setSelectedOrg(org);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedOrg(null);
  };

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  if (!isAdmin) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">
          You don't have permission to access organization management.
        </Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: "flex", justifyContent: "space-between", mb: 3 }}>
        <Typography variant="h4" component="h1">
          Organization Management
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setCreateDialogOpen(true)}
        >
          Create Organization
        </Button>
      </Box>

      <Box sx={{ borderBottom: 1, borderColor: "divider" }}>
        <Tabs value={tabValue} onChange={handleTabChange}>
          <Tab label="Organizations" icon={<BusinessIcon />} />
          <Tab label="Users" icon={<PeopleIcon />} />
          <Tab label="API Keys" icon={<KeyIcon />} />
          <Tab label="Settings" icon={<SettingsIcon />} />
        </Tabs>
      </Box>

      <TabPanel value={tabValue} index={0}>
        <Grid container spacing={3}>
          {organizations.map((org) => (
            <Grid item xs={12} md={6} lg={4} key={org.id}>
              <Card>
                <CardContent>
                  <Box
                    sx={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "flex-start",
                    }}
                  >
                    <Box>
                      <Typography variant="h6" component="h2">
                        {org.name}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {org.description || "No description"}
                      </Typography>
                    </Box>
                    <IconButton
                      onClick={(e) => handleMenuClick(e, org)}
                      size="small"
                    >
                      <MoreVertIcon />
                    </IconButton>
                  </Box>
                  <Box sx={{ mt: 2 }}>
                    <Chip
                      size="small"
                      label={
                        org.id === currentOrganization?.id
                          ? "Current"
                          : "Available"
                      }
                      color={
                        org.id === currentOrganization?.id
                          ? "primary"
                          : "default"
                      }
                    />
                    {org.apiKey && (
                      <Chip
                        size="small"
                        label="API Key"
                        color="secondary"
                        sx={{ ml: 1 }}
                      />
                    )}
                  </Box>
                  <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                    Created: {new Date(org.createdAt).toLocaleDateString()}
                  </Typography>
                </CardContent>
                <CardActions>
                  <Button size="small" onClick={() => setEditDialogOpen(true)}>
                    Edit
                  </Button>
                  <Button size="small" color="error">
                    Delete
                  </Button>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      </TabPanel>

      <TabPanel value={tabValue} index={1}>
        <Box sx={{ display: "flex", justifyContent: "space-between", mb: 2 }}>
          <Typography variant="h6">
            Users in {currentOrganization?.name}
          </Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setAddUserDialogOpen(true)}
          >
            Add User
          </Button>
        </Box>

        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Username</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Role</TableCell>
                <TableCell>Created</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {users.map((user) => (
                <TableRow key={user.id}>
                  <TableCell>{user.username}</TableCell>
                  <TableCell>{user.email}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={user.role}
                      color={user.role === "admin" ? "primary" : "default"}
                    />
                  </TableCell>
                  <TableCell>
                    {new Date(user.createdAt).toLocaleDateString()}
                  </TableCell>
                  <TableCell>
                    <IconButton size="small">
                      <EditIcon />
                    </IconButton>
                    <IconButton size="small" color="error">
                      <DeleteIcon />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </TabPanel>

      <TabPanel value={tabValue} index={2}>
        <Box sx={{ mb: 3 }}>
          <Typography variant="h6" gutterBottom>
            API Key Management
          </Typography>
          <Typography variant="body2" color="text.secondary" paragraph>
            Generate and manage API keys for programmatic access to your
            organization.
          </Typography>
        </Box>

        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Current API Key
            </Typography>
            {currentOrganization?.apiKey ? (
              <Box>
                <Typography
                  variant="body2"
                  sx={{ fontFamily: "monospace", mb: 2 }}
                >
                  {currentOrganization.apiKey}
                </Typography>
                <Button variant="outlined" color="error" size="small">
                  Revoke Key
                </Button>
              </Box>
            ) : (
              <Box>
                <Typography variant="body2" color="text.secondary" paragraph>
                  No API key generated yet.
                </Typography>
                <Button
                  variant="contained"
                  onClick={handleGenerateApiKey}
                  startIcon={<KeyIcon />}
                >
                  Generate API Key
                </Button>
              </Box>
            )}
          </CardContent>
        </Card>
      </TabPanel>

      <TabPanel value={tabValue} index={3}>
        <Typography variant="h6" gutterBottom>
          Organization Settings
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Configure organization-specific settings and preferences.
        </Typography>
        {/* Add settings form here */}
      </TabPanel>

      {/* Create Organization Dialog */}
      <Dialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
      >
        <DialogTitle>Create New Organization</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Organization Name"
            fullWidth
            variant="outlined"
            value={newOrgForm.name}
            onChange={(e) =>
              setNewOrgForm({ ...newOrgForm, name: e.target.value })
            }
          />
          <TextField
            margin="dense"
            label="Description"
            fullWidth
            multiline
            rows={3}
            variant="outlined"
            value={newOrgForm.description}
            onChange={(e) =>
              setNewOrgForm({ ...newOrgForm, description: e.target.value })
            }
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleCreateOrg} variant="contained">
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {/* Add User Dialog */}
      <Dialog
        open={addUserDialogOpen}
        onClose={() => setAddUserDialogOpen(false)}
      >
        <DialogTitle>Add User</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Username"
            fullWidth
            variant="outlined"
            value={newUserForm.username}
            onChange={(e) =>
              setNewUserForm({ ...newUserForm, username: e.target.value })
            }
          />
          <TextField
            margin="dense"
            label="Email"
            type="email"
            fullWidth
            variant="outlined"
            value={newUserForm.email}
            onChange={(e) =>
              setNewUserForm({ ...newUserForm, email: e.target.value })
            }
          />
          <TextField
            margin="dense"
            label="Password"
            type="password"
            fullWidth
            variant="outlined"
            value={newUserForm.password}
            onChange={(e) =>
              setNewUserForm({ ...newUserForm, password: e.target.value })
            }
          />
          <TextField
            margin="dense"
            label="Role"
            select
            fullWidth
            variant="outlined"
            value={newUserForm.role}
            onChange={(e) =>
              setNewUserForm({ ...newUserForm, role: e.target.value })
            }
          >
            <MenuItem value="member">Member</MenuItem>
            <MenuItem value="admin">Admin</MenuItem>
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddUserDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleAddUser} variant="contained">
            Add User
          </Button>
        </DialogActions>
      </Dialog>

      {/* Context Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={handleMenuClose}>
          <EditIcon sx={{ mr: 1 }} />
          Edit
        </MenuItem>
        <MenuItem onClick={handleMenuClose}>
          <DeleteIcon sx={{ mr: 1 }} />
          Delete
        </MenuItem>
      </Menu>
    </Box>
  );
};

export default OrganizationManagementPage;
