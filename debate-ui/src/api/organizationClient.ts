import BaseApiClient from "./baseClient";

export interface Organization {
  id: string;
  name: string;
  description?: string;
  apiKey?: string;
  settings?: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}

export interface User {
  id: string;
  username: string;
  email: string;
  organizationId: string;
  role: string;
  createdAt: string;
}

export interface CreateOrganizationRequest {
  name: string;
  description?: string;
  settings?: Record<string, any>;
}

export interface AuthRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
  organization: Organization;
}

class OrganizationClient extends BaseApiClient {
  constructor() {
    super("/api/v1");
  }

  // Authentication
  async login(credentials: AuthRequest): Promise<AuthResponse> {
    // For testing with simple backend, return mock response
    // In production, this would connect to a real auth service
    return Promise.resolve({
      token: "simple-test-token",
      user: {
        id: "user-001",
        username: credentials.username,
        email: "admin@acme.com",
        organizationId: "org-001",
        role: "admin",
        createdAt: "2024-01-01T00:00:00Z"
      },
      organization: {
        id: "org-001",
        name: "Acme Corporation",
        description: "Leading technology company",
        apiKey: "ak_test_abc123",
        createdAt: "2024-01-01T00:00:00Z",
        updatedAt: "2024-01-01T00:00:00Z"
      }
    });
  }

  async register(
    data: AuthRequest & { email: string; organizationName: string },
  ): Promise<AuthResponse> {
    const response = await this.client.post("/auth/register", data);
    return response.data;
  }

  // Organization management
  async createOrganization(
    data: CreateOrganizationRequest,
  ): Promise<Organization> {
    return this.callTool("create_organization", data);
  }

  async getOrganization(id: string): Promise<Organization> {
    const response = await this.client.get(`/organizations/${id}`);
    return response.data;
  }

  async updateOrganization(
    id: string,
    data: Partial<Organization>,
  ): Promise<Organization> {
    const response = await this.client.put(`/organizations/${id}`, data);
    return response.data;
  }

  async listOrganizations(): Promise<Organization[]> {
    const response = await this.client.get("/organizations");
    return response.data;
  }

  async switchOrganization(organizationId: string): Promise<void> {
    return this.callTool("switch_organization", {
      organization_id: organizationId,
    });
  }

  // User management
  async addUser(data: {
    username: string;
    email: string;
    password: string;
    role: string;
  }): Promise<User> {
    return this.callTool("add_user", data);
  }

  async listUsers(): Promise<User[]> {
    const response = await this.client.get("/users");
    return response.data;
  }

  async removeUser(userId: string): Promise<void> {
    return this.callTool("remove_user", { user_id: userId });
  }

  // API key management
  async generateApiKey(): Promise<{ apiKey: string }> {
    return this.callTool("generate_api_key", {});
  }

  async revokeApiKey(): Promise<void> {
    return this.callTool("revoke_api_key", {});
  }
}

export default new OrganizationClient();
