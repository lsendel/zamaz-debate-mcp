import React, { useState } from 'react';
import {
  CloseOutlined,
  DownloadOutlined,
  MobileOutlined,
  DesktopOutlined,
  WifiOutlined,
  DisconnectOutlined,
} from '@ant-design/icons';
import { usePWA } from '../hooks/usePWA';

const PWAPrompt = () => {
  const { isOnline, isInstalled, installPrompt, updateAvailable, installPWA, updatePWA } = usePWA();

  const [showInstallPrompt, setShowInstallPrompt] = useState(false);
  const [showUpdatePrompt, setShowUpdatePrompt] = useState(false);
  const [installing, setInstalling] = useState(false);
  const [updating, setUpdating] = useState(false);

  // Show install prompt when available
  React.useEffect(() => {
    if (installPrompt && !isInstalled) {
      setShowInstallPrompt(true);
    }
  }, [installPrompt, isInstalled]);

  // Show update prompt when available
  React.useEffect(() => {
    if (updateAvailable) {
      setShowUpdatePrompt(true);
    }
  }, [updateAvailable]);

  const handleInstall = async () => {
    setInstalling(true);
    try {
      const outcome = await installPWA();
      if (outcome === 'accepted') {
        setShowInstallPrompt(false);
      }
    } catch (error) {
      console.error('Failed to install PWA:', error);
    } finally {
      setInstalling(false);
    }
  };

  const handleUpdate = async () => {
    setUpdating(true);
    try {
      await updatePWA();
      setShowUpdatePrompt(false);
    } catch (error) {
      console.error('Failed to update PWA:', error);
    } finally {
      setUpdating(false);
    }
  };

  return (
    <>
      {/* Connection Status Bar */}
      {!isOnline && (
        <div className='fixed top-0 left-0 right-0 z-50 bg-red-600 text-white py-2 px-4 text-center text-sm'>
          <div className='flex items-center justify-center space-x-2'>
            <DisconnectOutlined style={{ fontSize: '16px' }} />
            <span>You're offline. Some features may be limited.</span>
          </div>
        </div>
      )}

      {/* Install Prompt */}
      {showInstallPrompt && (
        <div className='fixed bottom-4 left-4 right-4 z-40 bg-white border border-gray-200 rounded-lg shadow-lg p-4 max-w-md mx-auto'>
          <div className='flex items-start space-x-3'>
            <div className='flex-shrink-0'>
              <div className='w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center'>
                <DownloadOutlined style={{ fontSize: '20px', color: '#2563eb' }} />
              </div>
            </div>

            <div className='flex-1 min-w-0'>
              <h3 className='text-sm font-medium text-gray-900 mb-1'>Install Debate System</h3>
              <p className='text-sm text-gray-600 mb-3'>
                Install our app for a better experience with offline access and push notifications.
              </p>

              <div className='flex items-center space-x-2 text-xs text-gray-500 mb-3'>
                <DesktopOutlined style={{ fontSize: '12px' }} />
                <span>Works on desktop</span>
                <MobileOutlined style={{ fontSize: '12px', marginLeft: '8px' }} />
                <span>and mobile</span>
              </div>

              <div className='flex space-x-2'>
                <button
                  onClick={handleInstall}
                  disabled={installing}
                  className='flex-1 bg-blue-600 text-white px-3 py-2 rounded-md text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed'
                >
                  {installing ? 'Installing...' : 'Install'}
                </button>
                <button
                  onClick={() => setShowInstallPrompt(false)}
                  className='px-3 py-2 text-gray-600 hover:text-gray-800 text-sm'
                >
                  Not now
                </button>
              </div>
            </div>

            <button
              onClick={() => setShowInstallPrompt(false)}
              className='flex-shrink-0 text-gray-400 hover:text-gray-600'
            >
              <CloseOutlined style={{ fontSize: '16px' }} />
            </button>
          </div>
        </div>
      )}

      {/* Update Prompt */}
      {showUpdatePrompt && (
        <div className='fixed bottom-4 left-4 right-4 z-40 bg-blue-50 border border-blue-200 rounded-lg shadow-lg p-4 max-w-md mx-auto'>
          <div className='flex items-start space-x-3'>
            <div className='flex-shrink-0'>
              <div className='w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center'>
                <DownloadOutlined style={{ fontSize: '20px', color: '#2563eb' }} />
              </div>
            </div>

            <div className='flex-1 min-w-0'>
              <h3 className='text-sm font-medium text-gray-900 mb-1'>Update Available</h3>
              <p className='text-sm text-gray-600 mb-3'>
                A new version of the app is available with improvements and bug fixes.
              </p>

              <div className='flex space-x-2'>
                <button
                  onClick={handleUpdate}
                  disabled={updating}
                  className='flex-1 bg-blue-600 text-white px-3 py-2 rounded-md text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed'
                >
                  {updating ? 'Updating...' : 'Update Now'}
                </button>
                <button
                  onClick={() => setShowUpdatePrompt(false)}
                  className='px-3 py-2 text-gray-600 hover:text-gray-800 text-sm'
                >
                  Later
                </button>
              </div>
            </div>

            <button
              onClick={() => setShowUpdatePrompt(false)}
              className='flex-shrink-0 text-gray-400 hover:text-gray-600'
            >
              <CloseOutlined style={{ fontSize: '16px' }} />
            </button>
          </div>
        </div>
      )}

      {/* Connection Status Indicator */}
      <div className='fixed top-4 right-4 z-30'>
        <div
          className={`flex items-center space-x-1 px-2 py-1 rounded-full text-xs font-medium ${
            isOnline ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
          }`}
        >
          {isOnline ? (
            <>
              <WifiOutlined style={{ fontSize: '12px' }} />
              <span>Online</span>
            </>
          ) : (
            <>
              <DisconnectOutlined style={{ fontSize: '12px' }} />
              <span>Offline</span>
            </>
          )}
        </div>
      </div>
    </>
  );
};

export default PWAPrompt;
