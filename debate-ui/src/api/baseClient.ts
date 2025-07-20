import axios, { AxiosInstance, AxiosError } from "axios";

export interface ApiError {
  message: string;
  error?: string;
  error_type?: string;
  details?: Record<string, any>;
  request_id?: string;
}

class BaseApiClient {
  protected client: AxiosInstance;

  constructor(baseURL: string) {
    this.client = axios.create({
      baseURL,
      timeout: 30000,
      headers: {
        "Content-Type": "application/json",
      },
    });

    // Request interceptor
    this.client.interceptors.request.use(
      (config) => {
        // Add auth token if available
        const token = localStorage.getItem("authToken");
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }

        // Add organization ID if available
        const orgId = localStorage.getItem("currentOrgId");
        if (orgId) {
          config.headers["X-Organization-Id"] = orgId;
        }

        return config;
      },
      (error) => {
        return Promise.reject(error);
      },
    );

    // Response interceptor
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError<ApiError>) => {
        if (error.response) {
          const apiError: ApiError = {
            message: error.response.data?.message || error.message,
            error: error.response.data?.error,
            error_type: error.response.data?.error_type,
            details: error.response.data?.details,
            request_id: error.response.data?.request_id,
          };

          // Handle specific error types
          if (error.response.status === 401) {
            // Unauthorized - clear auth and redirect to login
            localStorage.removeItem("authToken");
            window.location.href = "/login";
          }

          return Promise.reject(apiError);
        }

        // In development, check if this is a connection error to backend
        if (
          error.message.includes("ECONNREFUSED") ||
          error.code === "ERR_NETWORK" ||
          error.message.includes("Network Error")
        ) {
          console.log(
            "🔧 Backend service not available",
          );
          return Promise.reject({
            message: "Backend service is not available. Please ensure all services are running.",
            error_type: "ServiceUnavailable",
            details: {
              originalError: error.message,
              serviceUrl: error.config?.baseURL,
            }
          } as ApiError);
        }

        return Promise.reject({
          message: error.message || "Network error",
          error_type: "NetworkError",
        } as ApiError);
      },
    );
  }

  // MCP-specific methods for tool calling
  async callTool(toolName: string, args: Record<string, any>) {
    const response = await this.client.post(`/tools/${toolName}`, args);
    return response.data;
  }

  // MCP-specific methods for resource access
  async getResource(resourceUri: string) {
    const response = await this.client.get(
      `/resources/${encodeURIComponent(resourceUri)}`,
    );
    return response.data;
  }

  async listResources() {
    const response = await this.client.get("/resources");
    return response.data;
  }
}

export default BaseApiClient;
