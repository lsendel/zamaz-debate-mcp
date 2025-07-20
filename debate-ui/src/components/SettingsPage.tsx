import React, { useState } from "react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Input,
  Button,
  Divider,
  Alert,
  Badge,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  FormField,
  Textarea,
} from "@zamaz/ui";
import {
  Trash2,
  Plus,
  Copy,
  Eye,
  EyeOff,
} from "lucide-react";
import { useAppSelector, useAppDispatch } from "../store";
import { generateApiKey } from "../store/slices/organizationSlice";
import { addNotification } from "../store/slices/uiSlice";
import organizationClient from "../api/organizationClient";

const SettingsPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { currentOrganization } = useAppSelector((state) => state.organization);
  const { user } = useAppSelector((state) => state.auth);

  const [showApiKey, setShowApiKey] = useState(false);
  const [users, setUsers] = useState<any[]>([]);
  const [addUserDialogOpen, setAddUserDialogOpen] = useState(false);
  const [newUser, setNewUser] = useState({
    username: "",
    email: "",
    password: "",
    role: "member",
  });
  const [orgSettings, setOrgSettings] = useState({
    name: currentOrganization?.name || "",
    description: currentOrganization?.description || "",
  });

  React.useEffect(() => {
    const loadUsers = async () => {
      if (currentOrganization) {
        try {
          const userList = await organizationClient.listUsers();
          setUsers(userList);
        } catch (error) {
          console.error("Failed to load users:", error);
        }
      }
    };
    loadUsers();
  }, [currentOrganization]);

  const handleGenerateApiKey = async () => {
    try {
      await dispatch(generateApiKey()).unwrap();
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

  const handleCopyApiKey = () => {
    if (currentOrganization?.apiKey) {
      navigator.clipboard.writeText(currentOrganization.apiKey);
      dispatch(
        addNotification({
          type: "success",
          message: "API key copied to clipboard",
        }),
      );
    }
  };

  const handleAddUser = async () => {
    try {
      const user = await organizationClient.addUser(newUser);
      setUsers([...users, user]);
      setAddUserDialogOpen(false);
      setNewUser({ username: "", email: "", password: "", role: "member" });
      dispatch(
        addNotification({
          type: "success",
          message: "User added successfully",
        }),
      );
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message: "Failed to add user",
        }),
      );
    }
  };

  const handleRemoveUser = async (userId: string) => {
    try {
      await organizationClient.removeUser(userId);
      setUsers(users.filter((u) => u.id !== userId));
      dispatch(
        addNotification({
          type: "success",
          message: "User removed successfully",
        }),
      );
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message: "Failed to remove user",
        }),
      );
    }
  };

  const handleUpdateOrganization = async () => {
    if (!currentOrganization) return;

    try {
      await organizationClient.updateOrganization(
        currentOrganization.id,
        orgSettings,
      );
      dispatch(
        addNotification({
          type: "success",
          message: "Organization settings updated",
        }),
      );
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message: "Failed to update organization settings",
        }),
      );
    }
  };

  if (!currentOrganization) {
    return (
      <div>
        <h1 className="text-3xl font-bold mb-4">Settings</h1>
        <Alert variant="warning">No organization selected</Alert>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Settings</h1>

      <Card>
        <CardHeader>
          <CardTitle>Organization Settings</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <FormField label="Organization Name">
            <Input
              value={orgSettings.name}
              onChange={(e) =>
                setOrgSettings({ ...orgSettings, name: e.target.value })
              }
              fullWidth
            />
          </FormField>
          <FormField label="Description">
            <Textarea
              value={orgSettings.description}
              onChange={(e) =>
                setOrgSettings({ ...orgSettings, description: e.target.value })
              }
              rows={3}
            />
          </FormField>
          <Button
            variant="primary"
            onClick={handleUpdateOrganization}
          >
            Update Settings
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>API Configuration</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-2">
            <div className="relative flex-1">
              <Input
                value={currentOrganization.apiKey || "No API key generated"}
                type={showApiKey ? "text" : "password"}
                readOnly
                fullWidth
                className="pr-20"
              />
              <div className="absolute right-2 top-1/2 -translate-y-1/2 flex gap-1">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setShowApiKey(!showApiKey)}
                >
                  {showApiKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </Button>
                {currentOrganization.apiKey && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={handleCopyApiKey}
                  >
                    <Copy className="h-4 w-4" />
                  </Button>
                )}
              </div>
            </div>
            <Button variant="secondary" onClick={handleGenerateApiKey}>
              {currentOrganization.apiKey ? "Regenerate" : "Generate"}
            </Button>
          </div>
          {currentOrganization.apiKey && (
            <Alert variant="warning" className="mt-4">
              Keep your API key secure. It provides full access to your
              organization's resources.
            </Alert>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Users</CardTitle>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setAddUserDialogOpen(true)}
              leftIcon={<Plus className="h-4 w-4" />}
            >
              Add User
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <Divider className="mb-4" />
          <div className="space-y-3">
            {users.map((u) => (
              <div key={u.id} className="flex items-center justify-between py-2">
                <div className="flex-1">
                  <p className="font-medium">{u.username}</p>
                  <p className="text-sm text-gray-500">{u.email}</p>
                </div>
                <div className="flex items-center gap-2">
                  <Badge variant={u.role === "admin" ? "primary" : "default"}>
                    {u.role}
                  </Badge>
                  {u.id !== user?.id && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleRemoveUser(u.id)}
                      className="text-red-600 hover:text-red-700"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      <Dialog open={addUserDialogOpen} onOpenChange={setAddUserDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Add User</DialogTitle>
            <DialogDescription>
              Add a new user to your organization
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <FormField label="Username" required>
              <Input
                value={newUser.username}
                onChange={(e) =>
                  setNewUser({ ...newUser, username: e.target.value })
                }
                fullWidth
              />
            </FormField>
            <FormField label="Email" required>
              <Input
                type="email"
                value={newUser.email}
                onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
                fullWidth
              />
            </FormField>
            <FormField label="Password" required>
              <Input
                type="password"
                value={newUser.password}
                onChange={(e) =>
                  setNewUser({ ...newUser, password: e.target.value })
                }
                fullWidth
              />
            </FormField>
            <FormField label="Role">
              <Select
                value={newUser.role}
                onValueChange={(value) => setNewUser({ ...newUser, role: value })}
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
            <Button
              variant="primary"
              onClick={handleAddUser}
              disabled={!newUser.username || !newUser.email || !newUser.password}
            >
              Add User
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default SettingsPage;
