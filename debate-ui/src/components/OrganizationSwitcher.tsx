import React from 'react';
import { useAppSelector, useAppDispatch } from '../store';
import { switchOrganization } from '../store/slices/organizationSlice';
import { fetchDebates } from '../store/slices/debateSlice';
import { Select, Badge, Typography } from 'antd';
import { BankOutlined } from '@ant-design/icons';

const { Text } = Typography;

const OrganizationSwitcher: React.FC = () => {
  const dispatch = useAppDispatch();
  const { organizations, currentOrganization } = useAppSelector(state => state.organization);

  const handleChange = async (orgId: string) => {
    await dispatch(switchOrganization(orgId));
    // Refresh debates for the new organization
    dispatch(fetchDebates());
  };

  if (organizations.length === 0) {
    return (
      <div style={{ padding: '16px', textAlign: 'center' }}>
        <Text type='secondary'>No organizations available</Text>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
      <Text
        strong
        style={{
          fontSize: '12px',
          textTransform: 'uppercase',
          color: '#666',
          letterSpacing: '0.05em',
        }}
      >
        Organization
      </Text>
      <Select
        value={currentOrganization?.id || ''}
        onChange={handleChange}
        placeholder='Select organization'
        style={{ width: '100%' }}
        suffixIcon={<BankOutlined />}
      >
        {organizations.map(org => (
          <Select.Option key={org.id} value={org.id}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {org.name}
              </span>
              {org.apiKey && (
                <Badge
                  count='API'
                  style={{
                    backgroundColor: '#f0f0f0',
                    color: '#666',
                    fontSize: '10px',
                    marginLeft: '8px',
                  }}
                />
              )}
            </div>
          </Select.Option>
        ))}
      </Select>
    </div>
  );
};

export default OrganizationSwitcher;
