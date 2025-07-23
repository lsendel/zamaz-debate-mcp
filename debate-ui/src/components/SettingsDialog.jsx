import React, { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "./ui/dialog";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import {
  SettingOutlined,
  MobileOutlined,
  DownloadOutlined,
  DeleteOutlined,
  SyncOutlined,
  WifiOutlined,
  DisconnectOutlined,
  DatabaseOutlined,
  BellOutlined,
  BellFilled,
} from "@ant-design/icons";
import { usePWA } from "../hooks/usePWA";

const SettingsDialog = () => {
  const {
    isOnline,
    isInstalled,
    installPrompt,
    updateAvailable,
    cacheStatus,
    installPWA,
    updatePWA,
    clearCache,
    getCacheInfo,
  } = usePWA();

  const [open, setOpen] = useState(false);
  const [cacheInfo, setCacheInfo] = useState(null);
  const [notificationsEnabled, setNotificationsEnabled] = useState(false);
  const [loading, setLoading] = useState(false);

  // Check notification permission status
  useEffect(() => {
    if ("Notification" in window) {
      setNotificationsEnabled(Notification.permission === "granted");
    }
  }, []);

  // Load cache info when dialog opens
  useEffect(() => {
    if (open) {
      loadCacheInfo();
    }
  }, [open]);

  const loadCacheInfo = async () => {
    try {
      const info = await getCacheInfo();
      setCacheInfo(info);
    } catch (error) {
      console.error("Failed to load cache info:", error);
    }
  };

  const handleInstallPWA = async () => {
    setLoading(true);
    try {
      await installPWA();
    } catch (error) {
      console.error("Failed to install PWA:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdatePWA = async () => {
    setLoading(true);
    try {
      await updatePWA();
    } catch (error) {
      console.error("Failed to update PWA:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleClearCache = async () => {
    if (
      window.confirm(
        "Are you sure you want to clear all cached data? This will reload the page.",
      )
    ) {
      setLoading(true);
      try {
        await clearCache();
      } catch (error) {
        console.error("Failed to clear cache:", error);
      } finally {
        setLoading(false);
      }
    }
  };

  const handleNotificationPermission = async () => {
    if ("Notification" in window) {
      try {
        const permission = await Notification.requestPermission();
        setNotificationsEnabled(permission === "granted");

        if (permission === "granted") {
          // Register for push notifications
          // This would typically involve registering with your push service
          console.log("Notifications enabled");
        }
      } catch (error) {
        console.error("Failed to request notification permission:", error);
      }
    }
  };

  const formatCacheSize = (entries) => {
    if (!entries) return "0 items";
    return `${entries.length} item${entries.length !== 1 ? "s" : ""}`;
  };

  const getCacheStatusColor = (status) => {
    switch (status) {
      case "active":
        return "bg-green-100 text-green-800";
      case "failed":
        return "bg-red-100 text-red-800";
      case "cleared":
        return "bg-blue-100 text-blue-800";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          <SettingOutlined style={{ fontSize: '16px', marginRight: '8px' }} />
          Settings
        </Button>
      </DialogTrigger>

      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center space-x-2">
            <SettingOutlined style={{ fontSize: '20px' }} />
            <span>App Settings</span>
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          {/* Connection Status */}
          <div className="bg-gray-50 rounded-lg p-4">
            <h3 className="font-medium text-gray-900 mb-3 flex items-center space-x-2">
              {isOnline ? (
                <WifiOutlined style={{ fontSize: '16px' }} />
              ) : (
                <DisconnectOutlined style={{ fontSize: '16px' }} />
              )}
              <span>Connection Status</span>
            </h3>

            <div className="flex items-center space-x-2">
              <Badge
                className={
                  isOnline
                    ? "bg-green-100 text-green-800"
                    : "bg-red-100 text-red-800"
                }
              >
                {isOnline ? "Online" : "Offline"}
              </Badge>
              {!isOnline && (
                <span className="text-sm text-gray-600">
                  Some features may be limited
                </span>
              )}
            </div>
          </div>

          {/* PWA Installation */}
          <div className="bg-gray-50 rounded-lg p-4">
            <h3 className="font-medium text-gray-900 mb-3 flex items-center space-x-2">
              <MobileOutlined style={{ fontSize: '16px' }} />
              <span>Progressive Web App</span>
            </h3>

            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-900">
                    Installation Status
                  </p>
                  <p className="text-sm text-gray-600">
                    {isInstalled ? "App is installed" : "App not installed"}
                  </p>
                </div>
                <Badge
                  className={
                    isInstalled
                      ? "bg-green-100 text-green-800"
                      : "bg-gray-100 text-gray-800"
                  }
                >
                  {isInstalled ? "Installed" : "Not Installed"}
                </Badge>
              </div>

              {!isInstalled && installPrompt && (
                <Button
                  onClick={handleInstallPWA}
                  disabled={loading}
                  className="w-full"
                >
                  <DownloadOutlined style={{ fontSize: '16px', marginRight: '8px' }} />
                  {loading ? "Installing..." : "Install App"}
                </Button>
              )}

              {updateAvailable && (
                <Button
                  onClick={handleUpdatePWA}
                  disabled={loading}
                  variant="outline"
                  className="w-full"
                >
                  <SyncOutlined style={{ fontSize: '16px', marginRight: '8px' }} />
                  {loading ? "Updating..." : "Update Available - Install Now"}
                </Button>
              )}
            </div>
          </div>

          {/* Notifications */}
          <div className="bg-gray-50 rounded-lg p-4">
            <h3 className="font-medium text-gray-900 mb-3 flex items-center space-x-2">
              {notificationsEnabled ? (
                <BellOutlined style={{ fontSize: '16px' }} />
              ) : (
                <BellFilled style={{ fontSize: '16px' }} />
              )}
              <span>Notifications</span>
            </h3>

            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-900">
                    Push Notifications
                  </p>
                  <p className="text-sm text-gray-600">
                    Get notified about debate updates
                  </p>
                </div>
                <Badge
                  className={
                    notificationsEnabled
                      ? "bg-green-100 text-green-800"
                      : "bg-gray-100 text-gray-800"
                  }
                >
                  {notificationsEnabled ? "Enabled" : "Disabled"}
                </Badge>
              </div>

              {!notificationsEnabled && (
                <Button
                  onClick={handleNotificationPermission}
                  variant="outline"
                  className="w-full"
                >
                  <BellOutlined style={{ fontSize: '16px', marginRight: '8px' }} />
                  Enable Notifications
                </Button>
              )}
            </div>
          </div>

          {/* Cache Management */}
          <div className="bg-gray-50 rounded-lg p-4">
            <h3 className="font-medium text-gray-900 mb-3 flex items-center space-x-2">
              <DatabaseOutlined style={{ fontSize: '16px' }} />
              <span>Cache Management</span>
            </h3>

            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-900">
                    Cache Status
                  </p>
                  <p className="text-sm text-gray-600">Offline data storage</p>
                </div>
                <Badge className={getCacheStatusColor(cacheStatus)}>
                  {cacheStatus}
                </Badge>
              </div>

              {cacheInfo && (
                <div className="space-y-2">
                  <p className="text-sm font-medium text-gray-900">
                    Cached Data:
                  </p>
                  {Object.entries(cacheInfo.caches || {}).map(
                    ([cacheName, info]) => (
                      <div
                        key={cacheName}
                        className="flex justify-between text-sm"
                      >
                        <span className="text-gray-600 truncate">
                          {cacheName
                            .replace("zamaz-debate-", "")
                            .replace("-v1.0.0", "")}
                        </span>
                        <span className="text-gray-800 font-mono">
                          {formatCacheSize(info.entries)}
                        </span>
                      </div>
                    ),
                  )}
                </div>
              )}

              <div className="flex space-x-2">
                <Button
                  onClick={loadCacheInfo}
                  variant="outline"
                  size="sm"
                  className="flex-1"
                >
                  <SyncOutlined style={{ fontSize: '16px', marginRight: '8px' }} />
                  Refresh
                </Button>
                <Button
                  onClick={handleClearCache}
                  variant="outline"
                  size="sm"
                  className="flex-1 text-red-600 hover:text-red-700"
                  disabled={loading}
                >
                  <DeleteOutlined style={{ fontSize: '16px', marginRight: '8px' }} />
                  Clear Cache
                </Button>
              </div>
            </div>
          </div>

          {/* App Information */}
          <div className="bg-gray-50 rounded-lg p-4">
            <h3 className="font-medium text-gray-900 mb-3">App Information</h3>

            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">Version:</span>
                <span className="text-gray-900 font-mono">1.0.0</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Build:</span>
                <span className="text-gray-900 font-mono">
                  {process.env.REACT_APP_BUILD_DATE || "Development"}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Environment:</span>
                <span className="text-gray-900 font-mono">
                  {process.env.NODE_ENV}
                </span>
              </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};

export default SettingsDialog;
