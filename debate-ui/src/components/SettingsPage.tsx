import React, { useState } from "react";
import {
  Card,
  Input,
  Button,
  Divider,
  Alert,
  Badge,
  Modal,
  Select,
  Form,
} from "antd";
import {
  DeleteOutlined,
  PlusOutlined,
  CopyOutlined,
  EyeOutlined,
  EyeInvisibleOutlined,
} from "@ant-design/icons";

const { TextArea } = Input;
const { Option } = Select;
import { useAppSelector, useAppDispatch } from "../store";
import { generateApiKey } from "../store/slices/organizationSlice";
import { addNotification } from "../store/slices/uiSlice";
import organizationClient from "../api/organizationClient";

const SettingsPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { currentOrganization } = useAppSelector((state) => state.organization);
  const { user } = useAppSelector((state) => state.auth);

// //   const [showApiKey, setShowApiKey] = useState(false); // SonarCloud: removed useless assignment // SonarCloud: removed useless assignment
  const [users, setUsers] = useState<any[]>([]);
  const [addUserModalOpen, setAddUserModalOpen] = useState(false);
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
      setAddUserModalOpen(false);
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
        <h1 style={{ fontSize: '30px', fontWeight: 'bold', marginBottom: '16px' }}>Settings</h1>
        <Alert message="No organization selected" type="warning" />
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <h1 style={{ fontSize: '30px', fontWeight: 'bold', margin: 0 }}>Settings</h1>

      <Card title="Organization Settings">
        <Form layout="vertical">
          <Form.Item label="Organization Name">
            <Input
              value={orgSettings.name}
              onChange={(e) =>
                setOrgSettings({ ...orgSettings, name: e.target.value })
              }
            />
          </Form.Item>
          <Form.Item label="Description">
            <TextArea
              value={orgSettings.description}
              onChange={(e) =>
                setOrgSettings({ ...orgSettings, description: e.target.value })
              }
              rows={3}
            />
          </Form.Item>
          <Button
            type="primary"
            onClick={handleUpdateOrganization}
          >
            Update Settings
          </Button>
        </Form>
      </Card>

      <Card title="API Configuration">
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Input.Password
            value={currentOrganization.apiKey || "No API key generated"}
            readOnly
            style={{ flex: 1 }}
            iconRender={(visible) => (visible ? <EyeOutlined /> : <EyeInvisibleOutlined />)}
          />
          {currentOrganization.apiKey && (
            <Button
              icon={<CopyOutlined />}
              onClick={handleCopyApiKey}
            />
          )}
          <Button type="default" onClick={handleGenerateApiKey}>
            {currentOrganization.apiKey ? "Regenerate" : "Generate"}
          </Button>
        </div>
        {currentOrganization.apiKey && (
          <Alert
            message="Keep your API key secure. It provides full access to your organization's resources."
            type="warning"
            style={{ marginTop: '16px' }}
          />
        )}
      </Card>

      <Card
        title="Users"
        extra={
          <Button
            type="default"
            size="small"
            onClick={() => setAddUserModalOpen(true)}
            icon={<PlusOutlined />}
          >
            Add User
          </Button>
        }
      >
        <Divider style={{ margin: '0 0 16px 0' }} />
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {users.map((u) => (
            <div key={u.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 0' }}>
              <div style={{ flex: 1 }}>
                <p style={{ fontWeight: 500, margin: 0 }}>{u.username}</p>
                <p style={{ fontSize: '14px', color: '#666', margin: 0 }}>{u.email}</p>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Badge color={u.role === "admin" ? "blue" : "default"} text={u.role} />
                {u.id !== user?.id && (
                  <Button
                    type="text"
                    size="small"
                    danger
                    onClick={() => handleRemoveUser(u.id)}
                    icon={<DeleteOutlined />}
                  />
                )}
              </div>
            </div>
          ))}
        </div>
      </Card>

      <Modal
        title="Add User"
        open={addUserModalOpen}
        onCancel={() => setAddUserModalOpen(false)}
        onOk={handleAddUser}
        okText="Add User"
        okButtonProps={{ disabled: !newUser.username || !newUser.email || !newUser.password }}
      >
        <p style={{ marginBottom: '16px' }}>
          Add a new user to your organization
        </p>
        <Form layout="vertical">
          <Form.Item label="Username" required>
            <Input
              value={newUser.username}
              onChange={(e) =>
                setNewUser({ ...newUser, username: e.target.value })
              }
            />
          </Form.Item>
          <Form.Item label="Email" required>
            <Input
              type="email"
              value={newUser.email}
              onChange={(e) => setNewUser({ ...newUser, email: e.target.value })}
            />
          </Form.Item>
          <Form.Item label="Password" required>
            <Input.Password
              value={newUser.password}
              onChange={(e) =>
                setNewUser({ ...newUser, password: e.target.value })
              }
            />
          </Form.Item>
          <Form.Item label="Role">
            <Select
              value={newUser.role}
              onChange={(value) => setNewUser({ ...newUser, role: value })}
              style={{ width: '100%' }}
            >
              <Option value="member">Member</Option>
              <Option value="admin">Admin</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default SettingsPage;
