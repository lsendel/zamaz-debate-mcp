import React, { useState, useCallback } from 'react';
import QueryBuilder, { 
  RuleGroupType, 
  Field, 
  formatQuery,
  defaultOperators,
  RuleType,
} from 'react-querybuilder';
import { motion, AnimatePresence } from 'framer-motion';
import 'react-querybuilder/dist/query-builder.css';

interface ConditionBuilderProps {
  initialQuery?: RuleGroupType;
  fields?: Field[];
  onQueryChange: (query: RuleGroupType) => void;
  onSave?: (query: RuleGroupType) => void;
  onCancel?: () => void;
  telemetryFields?: Field[];
}

const defaultFields: Field[] = [
  { name: 'temperature', label: 'Temperature', inputType: 'number' },
  { name: 'pressure', label: 'Pressure', inputType: 'number' },
  { name: 'humidity', label: 'Humidity', inputType: 'number' },
  { name: 'speed', label: 'Speed', inputType: 'number' },
  { name: 'location.lat', label: 'Latitude', inputType: 'number' },
  { name: 'location.lng', label: 'Longitude', inputType: 'number' },
  { name: 'status', label: 'Status', valueEditorType: 'select', values: [
    { name: 'active', label: 'Active' },
    { name: 'inactive', label: 'Inactive' },
    { name: 'error', label: 'Error' }
  ]},
  { name: 'deviceId', label: 'Device ID', inputType: 'text' },
  { name: 'timestamp', label: 'Timestamp', inputType: 'datetime-local' },
];

const customOperators = [
  ...defaultOperators,
  { name: 'contains', label: 'contains' },
  { name: 'beginsWith', label: 'begins with' },
  { name: 'endsWith', label: 'ends with' },
  { name: 'doesNotContain', label: 'does not contain' },
  { name: 'within', label: 'within radius' },
  { name: 'outside', label: 'outside radius' },
];

const ConditionBuilder: React.FC<ConditionBuilderProps> = ({
  initialQuery,
  fields = defaultFields,
  onQueryChange,
  onSave,
  onCancel,
  telemetryFields
}) => {
  const [query, setQuery] = useState<RuleGroupType>(
    initialQuery || {
      combinator: 'and',
      rules: []
    }
  );
  
  const [testResult, setTestResult] = useState<boolean | null>(null);
  const [testData, setTestData] = useState<string>('{}');
  const [showTestPanel, setShowTestPanel] = useState(false);

  const allFields = [...fields, ...(telemetryFields || [])];

  const handleQueryChange = useCallback((newQuery: RuleGroupType) => {
    setQuery(newQuery);
    onQueryChange(newQuery);
    setTestResult(null);
  }, [onQueryChange]);

  const handleTest = () => {
    try {
      const data = JSON.parse(testData);
      const sql = formatQuery(query, 'sql');
      console.log('Testing query:', sql);
      console.log('Test data:', data);
      
      // Simple evaluation logic (in production, this would be done server-side)
      const result = evaluateQuery(query, data);
      setTestResult(result);
    } catch (error) {
      console.error('Test error:', error);
      setTestResult(false);
    }
  };

  const evaluateQuery = (query: RuleGroupType, data: any): boolean => {
    const { combinator, rules } = query;
    
    if (rules.length === 0) return true;
    
    const results = rules.map((rule) => {
      if ('rules' in rule) {
        return evaluateQuery(rule as RuleGroupType, data);
      }
      
      const ruleTyped = rule as RuleType;
      const fieldValue = getNestedValue(data, ruleTyped.field);
      
      switch (ruleTyped.operator) {
        case '=':
          return fieldValue == ruleTyped.value;
        case '!=':
          return fieldValue != ruleTyped.value;
        case '<':
          return Number(fieldValue) < Number(ruleTyped.value);
        case '>':
          return Number(fieldValue) > Number(ruleTyped.value);
        case '<=':
          return Number(fieldValue) <= Number(ruleTyped.value);
        case '>=':
          return Number(fieldValue) >= Number(ruleTyped.value);
        case 'contains':
          return String(fieldValue).includes(String(ruleTyped.value));
        case 'beginsWith':
          return String(fieldValue).startsWith(String(ruleTyped.value));
        case 'endsWith':
          return String(fieldValue).endsWith(String(ruleTyped.value));
        case 'in':
          return String(ruleTyped.value).split(',').map(v => v.trim()).includes(String(fieldValue));
        case 'notIn':
          return !String(ruleTyped.value).split(',').map(v => v.trim()).includes(String(fieldValue));
        default:
          return false;
      }
    });
    
    return combinator === 'and' 
      ? results.every(Boolean)
      : results.some(Boolean);
  };

  const getNestedValue = (obj: any, path: string): any => {
    return path.split('.').reduce((current, key) => current?.[key], obj);
  };

  return (
    <div className="condition-builder-container">
      <div className="condition-builder-header">
        <h3>Condition Builder</h3>
        <div className="header-actions">
          <button 
            className="test-button"
            onClick={() => setShowTestPanel(!showTestPanel)}
          >
            🧪 Test
          </button>
          {onSave && (
            <button 
              className="save-button"
              onClick={() => onSave(query)}
            >
              💾 Save
            </button>
          )}
          {onCancel && (
            <button 
              className="cancel-button"
              onClick={onCancel}
            >
              ✕ Cancel
            </button>
          )}
        </div>
      </div>

      <div className="query-builder-wrapper">
        <QueryBuilder
          fields={allFields}
          query={query}
          onQueryChange={handleQueryChange}
          operators={customOperators}
          controlClassnames={{
            queryBuilder: 'queryBuilder-branches',
            ruleGroup: 'ruleGroup-preview',
            rule: 'rule-preview',
            fields: 'fields-preview',
            operators: 'operators-preview',
            value: 'value-preview',
          }}
        />
      </div>

      <AnimatePresence>
        {showTestPanel && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="test-panel"
          >
            <div className="test-panel-content">
              <h4>Test Your Condition</h4>
              <div className="test-input">
                <textarea
                  value={testData}
                  onChange={(e) => setTestData(e.target.value)}
                  placeholder='{"temperature": 25, "humidity": 60, "status": "active"}'
                  rows={5}
                />
              </div>
              <button className="run-test-button" onClick={handleTest}>
                Run Test
              </button>
              {testResult !== null && (
                <div className={`test-result ${testResult ? 'success' : 'failure'}`}>
                  <span className="result-icon">{testResult ? '✓' : '✗'}</span>
                  Result: {testResult ? 'TRUE' : 'FALSE'}
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="query-preview">
        <h4>SQL Preview:</h4>
        <pre>{formatQuery(query, 'sql')}</pre>
      </div>

      <style>{`
        .condition-builder-container {
          background: white;
          border-radius: 8px;
          padding: 20px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .condition-builder-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 20px;
        }

        .condition-builder-header h3 {
          margin: 0;
          color: #333;
        }

        .header-actions {
          display: flex;
          gap: 10px;
        }

        .test-button, .save-button, .cancel-button {
          padding: 8px 16px;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
          transition: all 0.2s;
        }

        .test-button {
          background: #2196F3;
          color: white;
        }

        .test-button:hover {
          background: #1976D2;
        }

        .save-button {
          background: #4CAF50;
          color: white;
        }

        .save-button:hover {
          background: #45a049;
        }

        .cancel-button {
          background: #f44336;
          color: white;
        }

        .cancel-button:hover {
          background: #da190b;
        }

        .query-builder-wrapper {
          margin-bottom: 20px;
        }

        .queryBuilder-branches {
          border: 1px solid #ddd;
          border-radius: 4px;
          padding: 15px;
          background: #f9f9f9;
        }

        .ruleGroup-preview {
          border: 1px solid #e0e0e0;
          border-radius: 4px;
          padding: 10px;
          margin: 10px 0;
          background: white;
        }

        .rule-preview {
          display: flex;
          align-items: center;
          gap: 10px;
          margin: 5px 0;
          padding: 8px;
          background: #f5f5f5;
          border-radius: 4px;
        }

        .test-panel {
          overflow: hidden;
        }

        .test-panel-content {
          padding: 20px;
          background: #f5f5f5;
          border-radius: 4px;
          margin-top: 20px;
        }

        .test-panel h4 {
          margin-top: 0;
        }

        .test-input {
          margin-bottom: 15px;
        }

        .test-input label {
          display: block;
          margin-bottom: 5px;
          font-weight: 500;
        }

        .test-input textarea {
          width: 100%;
          padding: 10px;
          border: 1px solid #ddd;
          border-radius: 4px;
          font-family: monospace;
          font-size: 14px;
        }

        .run-test-button {
          padding: 10px 20px;
          background: #FF9800;
          color: white;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
          transition: background 0.2s;
        }

        .run-test-button:hover {
          background: #F57C00;
        }

        .test-result {
          margin-top: 15px;
          padding: 15px;
          border-radius: 4px;
          display: flex;
          align-items: center;
          gap: 10px;
          font-weight: 500;
        }

        .test-result.success {
          background: #e8f5e9;
          color: #2e7d32;
          border: 1px solid #4caf50;
        }

        .test-result.failure {
          background: #ffebee;
          color: #c62828;
          border: 1px solid #f44336;
        }

        .result-icon {
          font-size: 20px;
        }

        .query-preview {
          margin-top: 20px;
          padding: 15px;
          background: #f5f5f5;
          border-radius: 4px;
        }

        .query-preview h4 {
          margin-top: 0;
          margin-bottom: 10px;
          color: #666;
        }

        .query-preview pre {
          margin: 0;
          padding: 10px;
          background: white;
          border: 1px solid #ddd;
          border-radius: 4px;
          overflow-x: auto;
          font-size: 13px;
        }
      `}</style>
    </div>
  );
};

export default ConditionBuilder;