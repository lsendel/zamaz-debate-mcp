import React, { useState, useEffect } from "react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
  CardFooter,
  Button,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Input,
  Textarea,
  Badge,
  Alert,
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
  DataTable,
  type Column,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  FormField,
} from "@zamaz/ui";
import {
  Plus,
  Edit,
  Trash2,
  MoreVertical,
  Building2,
  Users,
  Key,
  Settings,
} from "lucide-react";
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

const OrganizationManagementPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { organizations, currentOrganization, loading } = useAppSelector(
    (state) => state.organization,
  );
  const { user } = useAppSelector((state) => state.auth);

  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [addUserDialogOpen, setAddUserDialogOpen] = useState(false);
  const [users, setUsers] = useState<User[]>([]);

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
      setUsers([]);
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

  if (!isAdmin) {
    return (
      <div className="p-6">
        <Alert variant="error">
          You don't have permission to access organization management.
        </Alert>
      </div>
    );
  }

  const userColumns: Column<User>[] = [
    {
      key: 'username',
      header: 'Username',
      cell: (user) => user.username,
    },
    {
      key: 'email',
      header: 'Email',
      cell: (user) => user.email,
    },
    {
      key: 'role',
      header: 'Role',
      cell: (user) => (
        <Badge variant={user.role === "admin" ? "primary" : "default"}>
          {user.role}
        </Badge>
      ),
    },
    {
      key: 'createdAt',
      header: 'Created',
      cell: (user) => new Date(user.createdAt).toLocaleDateString(),
    },
    {
      key: 'actions',
      header: 'Actions',
      cell: (user) => (
        <div className="flex gap-2">
          <Button variant="ghost" size="sm">
            <Edit className="h-4 w-4" />
          </Button>
          <Button variant="ghost" size="sm" className="text-red-600 hover:text-red-700">
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold">Organization Management</h1>
        <Button
          variant="primary"
          onClick={() => setCreateDialogOpen(true)}
          leftIcon={<Plus className="h-4 w-4" />}
        >
          Create Organization
        </Button>
      </div>

      <Tabs defaultValue="organizations" className="w-full">
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="organizations" className="flex items-center gap-2">
            <Building2 className="h-4 w-4" />
            Organizations
          </TabsTrigger>
          <TabsTrigger value="users" className="flex items-center gap-2">
            <Users className="h-4 w-4" />
            Users
          </TabsTrigger>
          <TabsTrigger value="api-keys" className="flex items-center gap-2">
            <Key className="h-4 w-4" />
            API Keys
          </TabsTrigger>
          <TabsTrigger value="settings" className="flex items-center gap-2">
            <Settings className="h-4 w-4" />
            Settings
          </TabsTrigger>
        </TabsList>

        <TabsContent value="organizations" className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {organizations.map((org) => (
              <Card key={org.id}>
                <CardHeader>
                  <div className="flex justify-between items-start">
                    <div>
                      <CardTitle>{org.name}</CardTitle>
                      <CardDescription>
                        {org.description || "No description"}
                      </CardDescription>
                    </div>
                    <Button variant="ghost" size="sm">
                      <MoreVertical className="h-4 w-4" />
                    </Button>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="flex gap-2">
                    <Badge
                      variant={
                        org.id === currentOrganization?.id
                          ? "primary"
                          : "default"
                      }
                    >
                      {org.id === currentOrganization?.id
                        ? "Current"
                        : "Available"}
                    </Badge>
                    {org.apiKey && (
                      <Badge variant="secondary">API Key</Badge>
                    )}
                  </div>
                  <p className="text-sm text-gray-500 mt-2">
                    Created: {new Date(org.createdAt).toLocaleDateString()}
                  </p>
                </CardContent>
                <CardFooter className="flex gap-2">
                  <Button variant="secondary" size="sm">
                    Edit
                  </Button>
                  <Button variant="danger" size="sm">
                    Delete
                  </Button>
                </CardFooter>
              </Card>
            ))}
          </div>
        </TabsContent>

        <TabsContent value="users" className="space-y-4">
          <div className="flex justify-between items-center">
            <h2 className="text-xl font-semibold">
              Users in {currentOrganization?.name}
            </h2>
            <Button
              variant="primary"
              size="sm"
              onClick={() => setAddUserDialogOpen(true)}
              leftIcon={<Plus className="h-4 w-4" />}
            >
              Add User
            </Button>
          </div>

          <Card>
            <CardContent className="p-0">
              <DataTable data={users} columns={userColumns} />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="api-keys" className="space-y-4">
          <div className="mb-6">
            <h2 className="text-xl font-semibold mb-2">API Key Management</h2>
            <p className="text-gray-600">
              Generate and manage API keys for programmatic access to your
              organization.
            </p>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Current API Key</CardTitle>
            </CardHeader>
            <CardContent>
              {currentOrganization?.apiKey ? (
                <div>
                  <p className="font-mono text-sm bg-gray-100 p-2 rounded mb-4">
                    {currentOrganization.apiKey}
                  </p>
                  <Button variant="danger" size="sm">
                    Revoke Key
                  </Button>
                </div>
              ) : (
                <div>
                  <p className="text-gray-600 mb-4">
                    No API key generated yet.
                  </p>
                  <Button
                    variant="primary"
                    onClick={handleGenerateApiKey}
                    leftIcon={<Key className="h-4 w-4" />}
                  >
                    Generate API Key
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="settings" className="space-y-4">
          <h2 className="text-xl font-semibold mb-4">Organization Settings</h2>
          <p className="text-gray-600">
            Configure organization-specific settings and preferences.
          </p>
          <Card>
            <CardContent>
              <p className="text-gray-500">Settings coming soon...</p>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Create Organization Dialog */}
      <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create New Organization</DialogTitle>
            <DialogDescription>
              Create a new organization to manage debates and users
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <FormField label="Organization Name" required>
              <Input
                value={newOrgForm.name}
                onChange={(e) =>
                  setNewOrgForm({ ...newOrgForm, name: e.target.value })
                }
                fullWidth
              />
            </FormField>
            <FormField label="Description">
              <Textarea
                value={newOrgForm.description}
                onChange={(e) =>
                  setNewOrgForm({ ...newOrgForm, description: e.target.value })
                }
                rows={3}
              />
            </FormField>
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setCreateDialogOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreateOrg} variant="primary">
              Create
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Add User Dialog */}
      <Dialog open={addUserDialogOpen} onOpenChange={setAddUserDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add User</DialogTitle>
            <DialogDescription>
              Add a new user to your organization
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <FormField label="Username" required>
              <Input
                value={newUserForm.username}
                onChange={(e) =>
                  setNewUserForm({ ...newUserForm, username: e.target.value })
                }
                fullWidth
              />
            </FormField>
            <FormField label="Email" required>
              <Input
                type="email"
                value={newUserForm.email}
                onChange={(e) =>
                  setNewUserForm({ ...newUserForm, email: e.target.value })
                }
                fullWidth
              />
            </FormField>
            <FormField label="Password" required>
              <Input
                type="password"
                value={newUserForm.password}
                onChange={(e) =>
                  setNewUserForm({ ...newUserForm, password: e.target.value })
                }
                fullWidth
              />
            </FormField>
            <FormField label="Role">
              <Select
                value={newUserForm.role}
                onValueChange={(value) =>
                  setNewUserForm({ ...newUserForm, role: value })
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select a role" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="member">Member</SelectItem>
                  <SelectItem value="admin">Admin</SelectItem>
                </SelectContent>
              </Select>
            </FormField>
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setAddUserDialogOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleAddUser} variant="primary">
              Add User
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default OrganizationManagementPage;