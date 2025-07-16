import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import organizationClient, { Organization } from '../../api/organizationClient';

interface OrganizationState {
  organizations: Organization[];
  currentOrganization: Organization | null;
  loading: boolean;
  error: string | null;
}

const initialState: OrganizationState = {
  organizations: [],
  currentOrganization: null,
  loading: false,
  error: null,
};

export const fetchOrganizations = createAsyncThunk(
  'organization/fetchAll',
  async () => {
    const organizations = await organizationClient.listOrganizations();
    return organizations;
  }
);

export const switchOrganization = createAsyncThunk(
  'organization/switch',
  async (organizationId: string) => {
    await organizationClient.switchOrganization(organizationId);
    localStorage.setItem('currentOrgId', organizationId);
    const organization = await organizationClient.getOrganization(organizationId);
    return organization;
  }
);

export const createOrganization = createAsyncThunk(
  'organization/create',
  async (data: { name: string; description?: string }) => {
    const organization = await organizationClient.createOrganization(data);
    return organization;
  }
);

export const generateApiKey = createAsyncThunk(
  'organization/generateApiKey',
  async () => {
    const result = await organizationClient.generateApiKey();
    return result.apiKey;
  }
);

const organizationSlice = createSlice({
  name: 'organization',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    // Fetch organizations
    builder
      .addCase(fetchOrganizations.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchOrganizations.fulfilled, (state, action) => {
        state.loading = false;
        state.organizations = action.payload;
        
        // Set current organization if not set
        const currentOrgId = localStorage.getItem('currentOrgId');
        if (currentOrgId && action.payload.length > 0) {
          const currentOrg = action.payload.find(org => org.id === currentOrgId);
          state.currentOrganization = currentOrg || action.payload[0];
        } else if (action.payload.length > 0) {
          state.currentOrganization = action.payload[0];
          localStorage.setItem('currentOrgId', action.payload[0].id);
        }
      })
      .addCase(fetchOrganizations.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || 'Failed to fetch organizations';
      });

    // Switch organization
    builder
      .addCase(switchOrganization.fulfilled, (state, action) => {
        state.currentOrganization = action.payload;
      });

    // Create organization
    builder
      .addCase(createOrganization.fulfilled, (state, action) => {
        state.organizations.push(action.payload);
        state.currentOrganization = action.payload;
        localStorage.setItem('currentOrgId', action.payload.id);
      });

    // Generate API key
    builder
      .addCase(generateApiKey.fulfilled, (state, action) => {
        if (state.currentOrganization) {
          state.currentOrganization.apiKey = action.payload;
        }
      });
  },
});

export const { clearError } = organizationSlice.actions;
export default organizationSlice.reducer;