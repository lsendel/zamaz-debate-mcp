
import {  getProfessionalMapStyle } from './professionalMapStyles';

// Map configuration for OpenMapTiles and other providers
export const MAP_CONFIG = {
  // Default map style URLs
  styles: {
    // OpenMapTiles hosted styles
    openMapTiles: {
      streets: 'https://api.maptiler.com/maps/streets/style.json',
      basic: 'https://api.maptiler.com/maps/basic/style.json',
      bright: 'https://api.maptiler.com/maps/bright/style.json',
      satellite: 'https://api.maptiler.com/maps/hybrid/style.json',
      topo: 'https://api.maptiler.com/maps/topo/style.json'
    },
    // Self-hosted OpenMapTiles
    selfHosted: {
      default: '/tiles/style.json',
      custom: '/tiles/custom-style.json'
    }
  },

  // Map bounds for different regions
  bounds: {
    northAmerica: {
      bounds: [[-170, 15], [-50, 75]],
      center: [-98.5795, 39.8283],
      zoom: 3
    },
    europe: {
      bounds: [[-10, 35], [40, 70]],
      center: [10, 50],
      zoom: 4
    },
    stamfordCT: {
      bounds: [[-73.63, 40.96], [-73.33, 41.20]],
      center: [-73.5387, 41.0534],
      zoom: 12
    }
  },

  // Tile server configuration
  tileServers: {
    // For self-hosted OpenMapTiles server
    local: {
      url: process.env.REACT_APP_TILE_SERVER_URL || 'http://localhost:8080',
      key: process.env.REACT_APP_TILE_SERVER_KEY || ''
    },
    // For cloud-hosted tiles (MapTiler, Mapbox, etc.)
    cloud: {
      maptiler: {
        url: 'https://api.maptiler.com',
        key: process.env.REACT_APP_MAPTILER_KEY || 'YOUR_MAPTILER_KEY'
      }
    }
  },

  // Zoom level constraints
  zoomLevels: {
    min: 0,
    max: 14,
    default: 10,
    cluster: {
      start: 14,
      end: 8
    }
  },

  // Default marker styles
  markerStyles: {
    telemetry: {
      color: '#2196F3',
      icon: '📡',
      size: 24
    },
    alert: {
      color: '#f44336',
      icon: '⚠️',
      size: 28
    },
    device: {
      color: '#4CAF50',
      icon: '📱',
      size: 24
    },
    workflow: {
      color: '#FF9800',
      icon: '⚡',
      size: 26
    }
  },

  // Layer configurations
  layers: {
    telemetry: {
      id: 'telemetry-layer',
      name: 'Telemetry Data',
      type: 'circle',
      paint: {
        'circle-radius': [
          'interpolate',
          ['linear'],
          ['zoom'],
          8, 2,
          14, 8
        ],
        'circle-color': [
          'interpolate',
          ['linear'],
          ['get', 'value'],
          0, '#2196F3',
          50, '#FF9800',
          100, '#f44336'
        ],
        'circle-opacity': 0.8
      }
    },
    heatmap: {
      id: 'telemetry-heatmap',
      name: 'Telemetry Heatmap',
      type: 'heatmap',
      paint: {
        'heatmap-intensity': [
          'interpolate',
          ['linear'],
          ['zoom'],
          0, 1,
          14, 3
        ],
        'heatmap-radius': [
          'interpolate',
          ['linear'],
          ['zoom'],
          0, 2,
          14, 20
        ]
      }
    },
    cluster: {
      id: 'telemetry-cluster',
      name: 'Device Clusters',
      type: 'circle',
      filter: ['has', 'point_count'],
      paint: {
        'circle-color': [
          'step',
          ['get', 'point_count'],
          '#51bbd6',
          10, '#f1f075',
          50, '#f28cb1'
        ],
        'circle-radius': [
          'step',
          ['get', 'point_count'],
          20,
          10, 30,
          50, 40
        ]
      }
    }
  },

  // Performance optimization settings
  performance: {
    // Maximum number of markers to render without clustering
    maxMarkersWithoutClustering: 100,
    // Tile cache size in MB
    tileCacheSize: 50,
    // Worker pool size for tile processing
    workerPoolSize: 4,
    // Animation duration in ms
    animationDuration: 300
  }
};

// Helper function to get style URL with API key
export const getStyleUrl = (style: keyof typeof MAP_CONFIG.styles.openMapTiles): string => {
  const baseUrl = MAP_CONFIG.styles.openMapTiles[style];
  const key = MAP_CONFIG.tileServers.cloud.maptiler.key;
  return `${baseUrl}?key=${key}`;
};

// Helper function to check if using self-hosted tiles
export const isSelfHosted = (): boolean => {
  return process.env.REACT_APP_USE_SELF_HOSTED_TILES === 'true';
};


// Get appropriate style URL based on configuration
export const getMapStyle = (styleName?: string): any => {
  if (isSelfHosted()) {
    return styleName ? MAP_CONFIG.styles.selfHosted[styleName as keyof typeof MAP_CONFIG.styles.selfHosted] 
                     : MAP_CONFIG.styles.selfHosted.default;
  }
  
  // Use professional CARTO Light style by default (free, no API key required)
  const defaultStyle = styleName || 'cartoLight';
  
  // Check if we have API keys for premium providers
  const mapboxKey = process.env.REACT_APP_MAPBOX_KEY;
  const maptilerKey = process.env.REACT_APP_MAPTILER_KEY;
  
  // If we have premium keys, use them for better quality
  if (mapboxKey && styleName?.startsWith('mapbox-')) {
    return getProfessionalMapStyle('mapbox', styleName.replace('mapbox-', ''), mapboxKey);
  }
  
  if (maptilerKey && styleName?.startsWith('maptiler-')) {
    return getProfessionalMapStyle('maptiler', styleName.replace('maptiler-', ''), maptilerKey);
  }
  
  // Otherwise use free professional styles
  return getProfessionalMapStyle('free', defaultStyle);
};

// Export professional styles for use in components
export const AVAILABLE_MAP_STYLES = {
  light: {
    name: 'Professional Light',
    style: 'cartoLight',
    preview: '🌅'
  },
  dark: {
    name: 'Professional Dark',
    style: 'cartoDark',
    preview: '🌃'
  },
  technical: {
    name: 'Technical/Blueprint',
    style: 'toner',
    preview: '📐'
  },
  artistic: {
    name: 'Watercolor',
    style: 'watercolor',
    preview: '🎨'
  }
};