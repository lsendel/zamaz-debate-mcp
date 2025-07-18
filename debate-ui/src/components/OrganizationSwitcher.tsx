import React from "react";
import {
  FormControl,
  Select,
  MenuItem,
  SelectChangeEvent,
  Box,
  Typography,
  Chip,
} from "@mui/material";
import { Business as BusinessIcon } from "@mui/icons-material";
import { useAppSelector, useAppDispatch } from "../store";
import { switchOrganization } from "../store/slices/organizationSlice";
import { fetchDebates } from "../store/slices/debateSlice";

const OrganizationSwitcher: React.FC = () => {
  const dispatch = useAppDispatch();
  const { organizations, currentOrganization } = useAppSelector(
    (state) => state.organization,
  );

  const handleChange = async (event: SelectChangeEvent) => {
    const orgId = event.target.value;
    await dispatch(switchOrganization(orgId));
    // Refresh debates for the new organization
    dispatch(fetchDebates());
  };

  if (organizations.length === 0) {
    return (
      <Box sx={{ p: 2, textAlign: "center" }}>
        <Typography variant="body2" color="text.secondary">
          No organizations available
        </Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="subtitle2" color="text.secondary" gutterBottom>
        Organization
      </Typography>
      <FormControl fullWidth size="small">
        <Select
          value={currentOrganization?.id || ""}
          onChange={handleChange}
          displayEmpty
          startAdornment={
            <BusinessIcon sx={{ mr: 1, color: "action.active" }} />
          }
        >
          {organizations.map((org) => (
            <MenuItem key={org.id} value={org.id}>
              <Box
                sx={{ display: "flex", alignItems: "center", width: "100%" }}
              >
                <Typography variant="body2" sx={{ flexGrow: 1 }}>
                  {org.name}
                </Typography>
                {org.apiKey && (
                  <Chip
                    label="API"
                    size="small"
                    color="primary"
                    variant="outlined"
                  />
                )}
              </Box>
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </Box>
  );
};

export default OrganizationSwitcher;
