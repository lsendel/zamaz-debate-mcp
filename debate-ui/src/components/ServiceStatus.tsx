import React, { useEffect, useState } from 'react';
import { Alert, Button, Space } from 'antd';

interface ServiceStatusProps {
  serviceName: string;
  serviceUrl: string;
  onRetry?: () => void;
}

const ServiceStatus: React.FC<ServiceStatusProps> = ({ serviceName, serviceUrl, onRetry }) => {
  const [status, setStatus] = useState<'checking' | 'online' | 'offline'>('checking');
  const [error, setError] = useState<string | null>(null);

  const checkService = async () => {
    setStatus('checking');
    setError(null);
    
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000);
      
      // Try health endpoint first, then fall back to checking the actual API
      let response;
      try {
        response = await fetch(`${serviceUrl}/actuator/health`, {
          signal: controller.signal,
        });
      } catch (healthError) {
        // Log error for debugging
        console.error('[ServiceStatus] Error:', healthError);
        // Rethrow if critical
        if (healthError.critical) throw healthError;
          console.error("Error:", e);
        // If health endpoint fails, try the actual API endpoint
        response = await fetch(`${serviceUrl}/api/v1/debates`, {
          signal: controller.signal,
        });
        console.error("Error:", error);
      }
      
      clearTimeout(timeoutId);
      
      if (response.ok) {
        setStatus('online');
      } else if (response.status === 404) {
        // 404 on health check might mean service is running but health endpoint doesn't exist
        // Try to fetch debates to confirm
        try {
          const debatesResponse = await fetch(`${serviceUrl}/api/v1/debates`);
          if (debatesResponse.ok) {
            setStatus('online');
          } else {
            setStatus('offline');
            setError(`Service returned status ${debatesResponse.status}`);
          }
        } catch {
          setStatus('offline');
          setError('Service health check endpoint not found');
        }
      } else {
        setStatus('offline');
        setError(`Service returned status ${response.status}`);
      }
    } catch (err) {
      setStatus('offline');
      if (err instanceof Error) {
        if (err.name === 'AbortError') {
          setError('Service request timed out');
        } else if (err.message.includes('Failed to fetch')) {
          setError('Service is not running or not accessible');
        } else {
          setError(err.message);
        }
      }
    }
  };

  useEffect(() => {
    checkService();
  }, [serviceUrl]);

  if (status === 'checking') {
    return null;
  }

  if (status === 'offline') {
    return (
      <Alert
        message={`${serviceName} Service Unavailable`}
        description={
          <div>
            <p>{error || 'The service is not responding'}</p>
            <p>Please ensure the backend service is running on {serviceUrl}</p>
            <Space style={{ marginTop: '8px' }}>
              <Button 
                size="small" 
                icon={<ReloadOutlined />} 
                onClick={() => {
                  checkService();
                  if (onRetry) onRetry();
                }}
              >
                Retry Connection
              </Button>
            </Space>
          </div>
        }
        type="error"
        showIcon
        icon={<CloseCircleOutlined />}
        style={{ marginBottom: '16px' }}
      />
    );
  }

  return null;
};

export default ServiceStatus;