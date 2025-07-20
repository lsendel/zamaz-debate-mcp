// Professional map style configurations for various providers
// These offer more polished, enterprise-ready visualizations

export const PROFESSIONAL_MAP_STYLES = {
  // 1. Mapbox GL JS (Most Professional - Requires API Key)
  mapbox: {
    streets: 'mapbox://styles/mapbox/streets-v12',
    light: 'mapbox://styles/mapbox/light-v11',
    dark: 'mapbox://styles/mapbox/dark-v11',
    satellite: 'mapbox://styles/mapbox/satellite-streets-v12',
    navigation: 'mapbox://styles/mapbox/navigation-night-v1',
    // Professional custom styles
    blueprint: 'mapbox://styles/mapbox/blueprint-v1', // Technical/CAD look
    decimal: 'mapbox://styles/mapbox/decimal-v1', // Data visualization focused
  },

  // 2. MapTiler Cloud (Professional Alternative - Free tier available)
  maptiler: {
    streets: 'https://api.maptiler.com/maps/streets-v2/style.json',
    basic: 'https://api.maptiler.com/maps/basic-v2/style.json',
    bright: 'https://api.maptiler.com/maps/bright-v2/style.json',
    positron: 'https://api.maptiler.com/maps/positron/style.json', // Clean, minimal
    darkmatter: 'https://api.maptiler.com/maps/darkmatter/style.json', // Dark professional
    voyager: 'https://api.maptiler.com/maps/voyager/style.json', // Subtle, professional
    topo: 'https://api.maptiler.com/maps/topo-v2/style.json', // Topographic
    hybrid: 'https://api.maptiler.com/maps/hybrid/style.json', // Satellite + labels
  },

  // 3. Stadia Maps (Professional, affordable alternative)
  stadia: {
    // Requires API key but has generous free tier
    alidade_smooth: 'https://tiles.stadiamaps.com/styles/alidade_smooth.json',
    alidade_smooth_dark: 'https://tiles.stadiamaps.com/styles/alidade_smooth_dark.json',
    outdoors: 'https://tiles.stadiamaps.com/styles/outdoors.json',
    osm_bright: 'https://tiles.stadiamaps.com/styles/osm_bright.json',
  },

  // 4. CARTO (Professional data visualization focused)
  carto: {
    positron: 'https://basemaps.cartocdn.com/gl/positron-gl-style/style.json',
    darkmatter: 'https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json',
    voyager: 'https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json',
  },

  // 5. Esri/ArcGIS (Enterprise-grade)
  esri: {
    streets: 'https://basemaps-api.arcgis.com/arcgis/rest/services/styles/ArcGIS:Streets',
    navigation: 'https://basemaps-api.arcgis.com/arcgis/rest/services/styles/ArcGIS:Navigation',
    topographic: 'https://basemaps-api.arcgis.com/arcgis/rest/services/styles/ArcGIS:Topographic',
    lightGray: 'https://basemaps-api.arcgis.com/arcgis/rest/services/styles/ArcGIS:LightGray',
    darkGray: 'https://basemaps-api.arcgis.com/arcgis/rest/services/styles/ArcGIS:DarkGray',
    imagery: 'https://basemaps-api.arcgis.com/arcgis/rest/services/styles/ArcGIS:Imagery',
  },

  // 6. Free Professional-Looking Alternatives
  free: {
    // Stamen (Now hosted by Stadia Maps - free with attribution)
    toner: {
      version: 8,
      name: 'Stamen Toner',
      sources: {
        'stamen-toner': {
          type: 'raster',
          tiles: [
            'https://tiles.stadiamaps.com/tiles/stamen_toner/{z}/{x}/{y}.png'
          ],
          tileSize: 256,
          attribution: '&copy; <a href="https://stadiamaps.com/">Stadia Maps</a> &copy; <a href="https://stamen.com/">Stamen Design</a> &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }
      },
      layers: [{
        id: 'stamen-toner-layer',
        type: 'raster',
        source: 'stamen-toner',
        minzoom: 0,
        maxzoom: 20
      }]
    },
    
    // CARTO Light (No API key required)
    cartoLight: {
      version: 8,
      name: 'CARTO Light',
      sources: {
        'carto-light': {
          type: 'raster',
          tiles: [
            'https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png',
            'https://b.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png',
            'https://c.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png'
          ],
          tileSize: 256,
          attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
        }
      },
      layers: [{
        id: 'carto-light-layer',
        type: 'raster',
        source: 'carto-light',
        minzoom: 0,
        maxzoom: 20
      }]
    },

    // CARTO Dark (No API key required)
    cartoDark: {
      version: 8,
      name: 'CARTO Dark',
      sources: {
        'carto-dark': {
          type: 'raster',
          tiles: [
            'https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png',
            'https://b.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png',
            'https://c.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png'
          ],
          tileSize: 256,
          attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
        }
      },
      layers: [{
        id: 'carto-dark-layer',
        type: 'raster',
        source: 'carto-dark',
        minzoom: 0,
        maxzoom: 20
      }]
    },

    // Watercolor style (artistic but professional)
    watercolor: {
      version: 8,
      name: 'Stamen Watercolor',
      sources: {
        'stamen-watercolor': {
          type: 'raster',
          tiles: [
            'https://tiles.stadiamaps.com/tiles/stamen_watercolor/{z}/{x}/{y}.jpg'
          ],
          tileSize: 256,
          attribution: '&copy; <a href="https://stadiamaps.com/">Stadia Maps</a> &copy; <a href="https://stamen.com/">Stamen Design</a> &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }
      },
      layers: [{
        id: 'stamen-watercolor-layer',
        type: 'raster',
        source: 'stamen-watercolor',
        minzoom: 0,
        maxzoom: 20
      }]
    }
  }
};

// Helper function to get professional map style
export const getProfessionalMapStyle = (provider: string, style: string, apiKey?: string): any => {
  switch (provider) {
    case 'mapbox':
      if (!apiKey) {
        console.warn('Mapbox requires an API key. Falling back to free alternative.');
        return PROFESSIONAL_MAP_STYLES.free.cartoLight;
      }
      return `${PROFESSIONAL_MAP_STYLES.mapbox[style as keyof typeof PROFESSIONAL_MAP_STYLES.mapbox]}?access_token=${apiKey}`;
    
    case 'maptiler':
      if (!apiKey) {
        console.warn('MapTiler works better with an API key. Using free alternative.');
        return PROFESSIONAL_MAP_STYLES.free.cartoLight;
      }
      return `${PROFESSIONAL_MAP_STYLES.maptiler[style as keyof typeof PROFESSIONAL_MAP_STYLES.maptiler]}?key=${apiKey}`;
    
    case 'carto':
      // CARTO vector styles are free
      return PROFESSIONAL_MAP_STYLES.carto[style as keyof typeof PROFESSIONAL_MAP_STYLES.carto];
    
    case 'free':
      return PROFESSIONAL_MAP_STYLES.free[style as keyof typeof PROFESSIONAL_MAP_STYLES.free];
    
    default:
      return PROFESSIONAL_MAP_STYLES.free.cartoLight;
  }
};

// Recommended professional styles for different use cases
export const MAP_STYLE_RECOMMENDATIONS = {
  telemetry: {
    light: PROFESSIONAL_MAP_STYLES.free.cartoLight,
    dark: PROFESSIONAL_MAP_STYLES.free.cartoDark,
    technical: PROFESSIONAL_MAP_STYLES.free.toner,
  },
  
  geospatial: {
    standard: PROFESSIONAL_MAP_STYLES.free.cartoLight,
    satellite: 'mapbox-satellite', // Requires Mapbox key
    terrain: 'maptiler-topo', // Requires MapTiler key
  },
  
  analytics: {
    minimal: PROFESSIONAL_MAP_STYLES.free.cartoLight,
    contrast: PROFESSIONAL_MAP_STYLES.free.cartoDark,
    monochrome: PROFESSIONAL_MAP_STYLES.free.toner,
  }
};

// Configuration for adding custom map controls and overlays
export const MAP_OVERLAY_STYLES = {
  telemetryHeatmap: {
    gradient: {
      0.0: 'rgba(33, 102, 172, 0)',
      0.2: 'rgb(103, 169, 207)',
      0.4: 'rgb(209, 229, 240)',
      0.6: 'rgb(253, 219, 199)',
      0.8: 'rgb(239, 138, 98)',
      1.0: 'rgb(178, 24, 43)'
    },
    radius: 20,
    blur: 15,
    maxOpacity: 0.8
  },
  
  clusterStyle: {
    small: {
      radius: 20,
      color: '#51bbd6',
      strokeColor: '#fff',
      strokeWidth: 2
    },
    medium: {
      radius: 30,
      color: '#f1f075',
      strokeColor: '#fff',
      strokeWidth: 2
    },
    large: {
      radius: 40,
      color: '#f28cb1',
      strokeColor: '#fff',
      strokeWidth: 2
    }
  },
  
  markerStyles: {
    telemetry: {
      default: '#2196F3',
      warning: '#FF9800',
      alert: '#F44336',
      success: '#4CAF50'
    },
    
    workflow: {
      active: '#00BCD4',
      pending: '#FFC107',
      completed: '#8BC34A',
      error: '#E91E63'
    }
  }
};