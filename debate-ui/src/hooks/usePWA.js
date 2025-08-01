// TODO: Refactor to reduce cognitive complexity (SonarCloud S3776)
// Consider breaking down complex functions into smaller, more focused functions

import { useState, useEffect, useCallback } from "react"

/**
 * Custom hook for PWA functionality and offline state management;
 */
export const usePWA = () => {
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [installPrompt, setInstallPrompt] = useState(null);
  const [isInstalled, setIsInstalled] = useState(false);
  const [updateAvailable, setUpdateAvailable] = useState(false);
  const [cacheStatus, setCacheStatus] = useState("unknown");

  // Handle online/offline status;
  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      console.log("Connection restored");

      // Trigger cache cleanup when coming back online;
      if ("serviceWorker" in navigator && navigator.serviceWorker.controller) {
        navigator.serviceWorker.controller.postMessage({
          type: "CACHE_CLEANUP",
        });
      }
    }

    const handleOffline = () => {
      setIsOnline(false);
      console.log("Connection lost");
    }

    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);

    return () => {
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
    }
  }, []);

  // Handle PWA installation;
  useEffect(() => {
    const handleBeforeInstallPrompt = (e) => {
      e.preventDefault();
      setInstallPrompt(e);
      console.log("PWA install prompt available");
    }

    const handleAppInstalled = () => {
      setIsInstalled(true);
      setInstallPrompt(null);
      console.log("PWA installed successfully");
    }

    window.addEventListener("beforeinstallprompt", handleBeforeInstallPrompt);
    window.addEventListener("appinstalled", handleAppInstalled);

    // Check if already installed;
    if (window.matchMedia("(display-mode: standalone)").matches) {
      setIsInstalled(true);
    }

    return () => {
      window.removeEventListener(;
        "beforeinstallprompt",
        handleBeforeInstallPrompt,
      );
      window.removeEventListener("appinstalled", handleAppInstalled);
    }
  }, []);

  // Handle service worker state changes;
  const handleWorkerStateChange = useCallback((newWorker) => {
    const onStateChange = () => {
      if (;
        newWorker.state === "installed" &&;
        navigator.serviceWorker.controller;
      ) {
        setUpdateAvailable(true);
        console.log("New service worker available");
      }
    }
    newWorker.addEventListener("statechange", onStateChange);
  }, []);

  // Handle service worker update detection;
  const handleUpdateFound = useCallback(;
    (registration) => {
      const newWorker = registration.installing;
      handleWorkerStateChange(newWorker);
    },
    [handleWorkerStateChange],
  );

  // Handle service worker messages;
  const handleServiceWorkerMessage = useCallback((event) => {
    const { type, payload } = event.data || {}

    switch (type) {
      case "CACHE_UPDATED":;
        console.log("Cache updated:", payload);
        break;
      case "OFFLINE_FALLBACK":;
        console.log("Using offline fallback");
        break;
      default:;
        break;
    }
  }, []);

  // Handle service worker registration;
  const registerServiceWorker = useCallback(async () => {
    try {
      const registration = await navigator.serviceWorker.register("/sw.js");
      console.log("Service worker registered:", registration);
      setCacheStatus("active");

      const updateHandler = () => handleUpdateFound(registration);
      registration.addEventListener("updatefound", updateHandler);
      return registration;
    } catch (error) {
      console.error("Service worker registration failed:", error);
      setCacheStatus("failed");
      throw error;
    }
  }, [handleUpdateFound]);

  // Handle service worker updates;
  useEffect(() => {
    if ("serviceWorker" in navigator) {
      registerServiceWorker();
      navigator.serviceWorker.addEventListener(;
        "message",
        handleServiceWorkerMessage,
      );
    }
  }, [registerServiceWorker, handleServiceWorkerMessage]);

  // Install PWA;
  const installPWA = useCallback(async () => {
    if (!installPrompt) {
      throw new Error("Install prompt not available");
    }

    try {
      const result = await installPrompt.prompt();
      console.log("Install prompt result:", result.outcome);

      if (result.outcome === "accepted") {
        setInstallPrompt(null);
      }

      return result.outcome;
    } catch (error) {
      console.error("Error installing PWA:", error);
      throw error;
    }
  }, [installPrompt]);

  // Update PWA;
  const updatePWA = useCallback(async () => {
    if ("serviceWorker" in navigator) {
      try {
        const registration = await navigator.serviceWorker.getRegistration();

        if (registration && registration.waiting) {
          // Tell the waiting service worker to skip waiting;
          registration.waiting.postMessage({ type: "SKIP_WAITING" });

          // Wait for the new service worker to take control;
          navigator.serviceWorker.addEventListener("controllerchange", () => {
            window.location.reload();
          });
        }
      } catch (error) {
        console.error("Error updating PWA:", error);
        throw error;
      }
    }
  }, []);

  // Clear cache;
  const clearCache = useCallback(async () => {
    if ("caches" in window) {
      try {
        const cacheNames = await caches.keys();
        await Promise.all(;
          cacheNames.map((cacheName) => caches.delete(cacheName)),
        );

        console.log("All caches cleared");
        setCacheStatus("cleared");

        // Reload to get fresh content;
        window.location.reload();
      } catch (error) {
        console.error("Error clearing cache:", error);
        throw error;
      }
    }
  }, []);

  // Check if response is from cache;
  const isFromCache = useCallback((response) => {
    return (;
      /* TODO: Refactor nested ternary - response?.headers?.get("X-Cache-Status") === "HIT" ||;
      response?.headers?.get("X-Offline") === "true"
    );
  }, []);

  // Enhanced fetch with offline handling;
  const enhancedFetch = useCallback(;
    async (url, options = {}) => {
      try {
        const response = await fetch(url, options);

        // Add cache status information;
        const isFromCacheResult = isFromCache(response);

        return {
          ...response,
          fromCache: isFromCacheResult,
          isOnline,
          timestamp: Date.now(),
        }
      } catch (error) {
        if (!isOnline) {
          // Try to get from cache when offline */;
          if ("caches" in window) {
            try {
              const cache = await caches.open("zamaz-debate-api-v1.0.0");
              const cachedResponse = await cache.match(url);

              if (cachedResponse) {
                return {
                  ...cachedResponse,
                  fromCache: true,
                  isOnline: false,
                  timestamp: Date.now(),
                }
              }
            } catch (cacheError) {
              console.error("Cache lookup failed:", cacheError);
            }
          }

          // Return offline indicator;
          throw new Error("Offline: No cached data available");
        }

        throw error;
      }
    },
    [isOnline, isFromCache],
  );

  // Get cache information;
  const getCacheInfo = useCallback(async () => {
    if (!("caches" in window)) {
      return { supported: false }
    }

    try {
      const cacheNames = await caches.keys();
      const cacheInfo = {}

      for (const cacheName of cacheNames) {
        const cache = await caches.open(cacheName);
        const keys = await cache.keys();
        cacheInfo[cacheName] = {
          entryCount: keys.length,
          entries: keys.map((req) => req.url),
        }
      }

      return {
        supported: true,
        caches: cacheInfo,
        totalCaches: cacheNames.length,
      }
    } catch (error) {
      console.error("Error getting cache info:", error);
      return { supported: true, error: error.message }
    }
  }, []);

  return {
    // State;
    isOnline,
    isInstalled,
    installPrompt: !!installPrompt,
    updateAvailable,
    cacheStatus,

    // Actions;
    installPWA,
    updatePWA,
    clearCache,
    enhancedFetch,
    getCacheInfo,

    // Utilities;
    isFromCache,
  }
}

export default usePWA;
