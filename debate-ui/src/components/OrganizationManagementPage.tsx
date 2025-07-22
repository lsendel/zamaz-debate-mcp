import React, { useState, useEffect } from "react";
import {
  Card,
  Button,
  Modal,
  Input,
  Badge,
  Alert,
  Tabs,
  Table,
  Select,
  Form,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  MoreOutlined,
  BankOutlined,
  UsergroupAddOutlined,
  KeyOutlined,
  SettingOutlined,
  ThunderboltOutlined,
  
} from "@ant-design/icons";

const { TextArea } = Input;
const { Option } = Select;
const { TabPane } = Tabs;
import { useAppSelector, useAppDispatch } from "../store";
import {
  fetchOrganizations,
  createOrganization,
  generateApiKey,
} from "../store/slices/organizationSlice";
import { addNotification } from "../store/slices/uiSlice";
import organizationClient, {
  
  User,
} from "../api/organizationClient";
import LLMPresetConfig from "./LLMPresetConfig";

const OrganizationManagementPage: React.FC = () => {
  const dispatch = useAppDispatch();
//   const { organizations, currentOrganization, loading } = useAppSelector( // SonarCloud: removed useless assignment
    (state) => state.organization,
  );
  const { user } = useAppSelector((state) => state.auth);

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [addUserModalOpen, setAddUserModalOpen] = useState(false);
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
      setCreateModalOpen(false);
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
      setAddUserModalOpen(false);
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
      <div style={{ padding: '24px' }}>
        <Alert
          message="You don't have permission to access organization management."
          type="error"
        />
      </div>
    );
  }

  const userColumns: ColumnsType<User> = [
    {
      key: 'username',
      title: 'Username',
      dataIndex: 'username',
    },
    {
      key: 'email',
      title: 'Email',
      dataIndex: 'email',
    },
    {
      key: 'role',
      title: 'Role',
      dataIndex: 'role',
      render: (role: string) => (
        <Badge color={role === "admin" ? "blue" : "default"} text={role} />
      ),
    },
    {
      key: 'createdAt',
      title: 'Created',
      dataIndex: 'createdAt',
      render: (date: string) => new Date(date).toLocaleDateString(),
    },
    {
      key: 'actions',
      title: 'Actions',
      render: (_: any, user: User) => (
        <div style={{ display: 'flex', gap: '8px' }}>
          <Button type="text" icon={<EditOutlined />} size="small" />
          <Button type="text" danger icon={<DeleteOutlined />} size="small" />
        </div>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <h1 style={{ fontSize: '30px', fontWeight: 'bold', margin: 0 }}>Organization Management</h1>
        <Button
          type="primary"
          onClick={() => setCreateModalOpen(true)}
          icon={<PlusOutlined />}
        >
          Create Organization
        </Button>
      </div>

      <Tabs defaultActiveKey="organizations">

        <TabPane
          tab={
            <span>
              <BankOutlined />
              Organizations
            </span>
          }
          key="organizations"
        >
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '16px', marginTop: '16px' }}>
            {organizations.map((org) => (
              <Card 
                key={org.id}
                title={
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                    <div>
                      <h3>{org.name}</h3>
                      <p style={{ color: '#666', marginTop: '4px' }}>
                        {org.description || "No description"}
                      </p>
                    </div>
                    <Button type="text" icon={<MoreOutlined />} size="small" />
                  </div>
                }
                actions={[
                  <Button key="edit" type="link" size="small">
                    Edit
                  </Button>,
                  <Button key="delete" type="link" danger size="small">
                    Delete
                  </Button>,
                ]}
              >
                <div style={{ display: 'flex', gap: '8px', marginBottom: '8px' }}>
                  <Badge
                    color={org.id === currentOrganization?.id ? "blue" : "default"}
                    text={org.id === currentOrganization?.id ? "Current" : "Available"}
                  />
                  {org.apiKey && (
                    <Badge color="purple" text="API Key" />
                  )}
                </div>
                <p style={{ fontSize: '14px', color: '#666' }}>
                  Created: {new Date(org.createdAt).toLocaleDateString()}
                </p>
              </Card>
            ))}
          </div>
        </TabPane>

        <TabPane
          tab={
            <span>
              <UsergroupAddOutlined />
              Users
            </span>
          }
          key="users"
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '16px 0' }}>
            <h2 style={{ fontSize: '20px', fontWeight: '600', margin: 0 }}>
              Users in {currentOrganization?.name}
            </h2>
            <Button
              type="primary"
              size="small"
              onClick={() => setAddUserModalOpen(true)}
              icon={<PlusOutlined />}
            >
              Add User
            </Button>
          </div>

          <Card>
            <Table dataSource={users} columns={userColumns} rowKey="id" />
          </Card>
        </TabPane>

        <TabPane
          tab={
            <span>
              <KeyOutlined />
              API Keys
            </span>
          }
          key="api-keys"
        >
          <div style={{ marginBottom: '24px', marginTop: '16px' }}>
            <h2 style={{ fontSize: '20px', fontWeight: '600', marginBottom: '8px' }}>API Key Management</h2>
            <p style={{ color: '#666' }}>
              Generate and manage API keys for programmatic access to your
              organization.
            </p>
          </div>

          <Card title="Current API Key">
            {currentOrganization?.apiKey ? (
              <div>
                <p style={{ fontFamily: 'monospace', fontSize: '14px', background: '#f5f5f5', padding: '8px', borderRadius: '4px', marginBottom: '16px' }}>
                  {currentOrganization.apiKey}
                </p>
                <Button danger size="small">
                  Revoke Key
                </Button>
              </div>
            ) : (
              <div>
                <p style={{ color: '#666', marginBottom: '16px' }}>
                  No API key generated yet.
                </p>
                <Button
                  type="primary"
                  onClick={handleGenerateApiKey}
                  icon={<KeyOutlined />}
                >
                  Generate API Key
                </Button>
              </div>
            )}
          </Card>
        </TabPane>

        <TabPane
          tab={
            <span>
              <ThunderboltOutlined />
              LLM Presets
            </span>
          }
          key="llm-presets"
        >
          <div style={{ marginBottom: '24px', marginTop: '16px' }}>
            <h2 style={{ fontSize: '20px', fontWeight: '600', marginBottom: '8px' }}>
              LLM Preset Management
            </h2>
            <p style={{ color: '#666' }}>
              Create and manage LLM presets for your organization's debates. These presets define
              the AI models, parameters, and prompts used by debate participants.
            </p>
          </div>

          <LLMPresetConfig
            organizationId={currentOrganization?.id}
            onSave={(preset) => {
              dispatch(addNotification({
                type: 'success',
                message: `LLM preset '${preset.name}' saved successfully`,
              }));
            }}
          />
        </TabPane>

        <TabPane
          tab={
            <span>
              <SettingOutlined />
              Settings
            </span>
          }
          key="settings"
        >
          <h2 style={{ fontSize: '20px', fontWeight: '600', margin: '16px 0' }}>Organization Settings</h2>
          <p style={{ color: '#666', marginBottom: '16px' }}>
            Configure organization-specific settings and preferences.
          </p>
          <Card title="General Settings">
            <Form layout="vertical">
              <Form.Item label="Organization Name">
                <Input value={currentOrganization?.name} disabled />
              </Form.Item>
              <Form.Item label="Description">
                <TextArea value={currentOrganization?.description} disabled rows={3} />
              </Form.Item>
              <Form.Item label="Created">
                <Input value={currentOrganization?.createdAt ? new Date(currentOrganization.createdAt).toLocaleString() : ''} disabled />
              </Form.Item>
              <Form.Item label="Organization Scope">
                <Badge color="blue" text="Organization-wide debates and presets" />
              </Form.Item>
            </Form>
          </Card>
        </TabPane>
      </Tabs>

      {/* Create Organization Modal */}
      <Modal
        title="Create New Organization"
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        onOk={handleCreateOrg}
        okText="Create"
      >
        <p style={{ marginBottom: '16px' }}>
          Create a new organization to manage debates and users
        </p>
        <Form layout="vertical">
          <Form.Item label="Organization Name" required>
            <Input
              value={newOrgForm.name}
              onChange={(e) =>
                setNewOrgForm({ ...newOrgForm, name: e.target.value })
              }
            />
          </Form.Item>
          <Form.Item label="Description">
            <TextArea
              value={newOrgForm.description}
              onChange={(e) =>
                setNewOrgForm({ ...newOrgForm, description: e.target.value })
              }
              rows={3}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* Add User Modal */}
      <Modal
        title="Add User"
        open={addUserModalOpen}
        onCancel={() => setAddUserModalOpen(false)}
        onOk={handleAddUser}
        okText="Add User"
      >
        <p style={{ marginBottom: '16px' }}>
          Add a new user to your organization
        </p>
        <Form layout="vertical">
          <Form.Item label="Username" required>
            <Input
              value={newUserForm.username}
              onChange={(e) =>
                setNewUserForm({ ...newUserForm, username: e.target.value })
              }
            />
          </Form.Item>
          <Form.Item label="Email" required>
            <Input
              type="email"
              value={newUserForm.email}
              onChange={(e) =>
                setNewUserForm({ ...newUserForm, email: e.target.value })
              }
            />
          </Form.Item>
          <Form.Item label="Password" required>
            <Input.Password
              value={newUserForm.password}
              onChange={(e) =>
                setNewUserForm({ ...newUserForm, password: e.target.value })
              }
            />
          </Form.Item>
          <Form.Item label="Role">
            <Select
              value={newUserForm.role}
              onChange={(value) =>
                setNewUserForm({ ...newUserForm, role: value })
              }
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

export default OrganizationManagementPage;