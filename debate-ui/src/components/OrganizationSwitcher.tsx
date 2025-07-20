import React from 'react';
import { useAppSelector, useAppDispatch } from '../store';
import { switchOrganization } from '../store/slices/organizationSlice';
import { fetchDebates } from '../store/slices/debateSlice';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Badge,
} from '@zamaz/ui';
import { Building2 } from 'lucide-react';

const OrganizationSwitcher: React.FC = () => {
  const dispatch = useAppDispatch();
  const { organizations, currentOrganization } = useAppSelector(
    (state) => state.organization
  );

  const handleChange = async (orgId: string) => {
    await dispatch(switchOrganization(orgId));
    // Refresh debates for the new organization
    dispatch(fetchDebates());
  };

  if (organizations.length === 0) {
    return (
      <div className="p-4 text-center">
        <p className="text-sm text-gray-500">No organizations available</p>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <label className="text-xs font-medium text-gray-600 uppercase tracking-wider">
        Organization
      </label>
      <Select
        value={currentOrganization?.id || ''}
        onValueChange={handleChange}
      >
        <SelectTrigger className="w-full">
          <div className="flex items-center gap-2">
            <Building2 className="h-4 w-4 text-gray-500" />
            <SelectValue placeholder="Select organization" />
          </div>
        </SelectTrigger>
        <SelectContent>
          {organizations.map((org) => (
            <SelectItem key={org.id} value={org.id}>
              <div className="flex items-center justify-between w-full gap-2">
                <span className="truncate">{org.name}</span>
                {org.apiKey && (
                  <Badge variant="outline" className="text-xs">
                    API
                  </Badge>
                )}
              </div>
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
};

export default OrganizationSwitcher;