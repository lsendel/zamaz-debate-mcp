import React from 'react';
import { QueryBuilder, RuleGroupType } from 'react-querybuilder';

const fields = [
  { name: 'temperature', label: 'Temperature' },
  { name: 'humidity', label: 'Humidity' },
  { name: 'status', label: 'Status' },
];

const operators = [
  { name: '=', label: 'equals' },
  { name: '>', label: 'greater than' },
  { name: '<', label: 'less than' },
];

const ConditionBuilder: React.FC<{
  query: RuleGroupType;
  onQueryChange: (query: RuleGroupType) => void;
}> = ({ query, onQueryChange }) => {
  return (
    <QueryBuilder
      fields={fields}
      operators={operators}
      query={query}
      onQueryChange={onQueryChange}
    />
  );
};

export default ConditionBuilder;