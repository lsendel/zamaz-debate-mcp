import React, { useRef, useEffect, useState } from 'react';
import maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { motion } from 'framer-motion';
import { getMapStyle } from '../config/mapConfig';

export interface MapMarker {
  id: string;
  lng: number;
  lat: number;
  type?: string;
  properties?: Record<string, any>;
  color?: string;
  icon?: string;
}

export interface MapLayer {
  id: string;
  name: string;
  type: 'markers' | 'heatmap' | 'cluster' | 'line' | 'polygon';
  visible: boolean;
  data?: any;
}

interface MapViewerProps {
  center?: [number, number];
  zoom?: number;
  markers?: MapMarker[];
  layers?: MapLayer[];
  height?: string;
  onMarkerClick?: (marker: MapMarker) => void;
  onMapClick?: (lng: number, lat: number) => void;
  onBoundsChange?: (bounds: maplibregl.LngLatBounds) => void;
  style?: string | any;
  minZoom?: number;
  maxZoom?: number;
}

const MapViewer: React.FC<MapViewerProps> = ({
  center = [-73.98, 41.08], // Default to Stamford, CT
  zoom = 12,
  markers = [],
  layers = [],
  height = '500px',
  onMarkerClick,
  onMapClick,
  onBoundsChange,
  style,
  minZoom = 0,
  maxZoom = 18
}) => {
  const mapContainer = useRef<HTMLDivElement>(null);
  const map = useRef<maplibregl.Map | null>(null);
  const markersRef = useRef<Map<string, maplibregl.Marker>>(new Map());
  
  const [mapLoaded, setMapLoaded] = useState(false);
  const [activeMarkerId, setActiveMarkerId] = useState<string | null>(null);
  const [layerVisibility, setLayerVisibility] = useState<Record<string, boolean>>({});

  // Initialize map
  useEffect(() => {
    if (!mapContainer.current || map.current) return;

    // Use provided style or default to free OpenStreetMap style
    const mapStyle = style || getMapStyle();

    map.current = new maplibregl.Map({
      container: mapContainer.current,
      style: mapStyle,
      center: center,
      zoom: zoom,
      minZoom: minZoom,
      maxZoom: maxZoom,
      attributionControl: false
    });

    // Add navigation controls
    map.current.addControl(new maplibregl.NavigationControl(), 'top-right');
    
    // Add scale control
    map.current.addControl(new maplibregl.ScaleControl({
      maxWidth: 200,
      unit: 'metric'
    }), 'bottom-left');

    // Add attribution control with custom position
    map.current.addControl(new maplibregl.AttributionControl({
      compact: true
    }), 'bottom-right');

    // Map event handlers
    map.current.on('load', () => {
      setMapLoaded(true);
      
      // Add building extrusion layer for 3D effect
      if (map.current?.getLayer('building')) {
        map.current.setPaintProperty('building', 'fill-extrusion-height', [
          'interpolate',
          ['linear'],
          ['zoom'],
          12, 0,
          14, ['get', 'height']
        ]);
        map.current.setPaintProperty('building', 'fill-extrusion-opacity', 0.6);
      }
    });

    map.current.on('click', (e) => {
      if (onMapClick) {
        onMapClick(e.lngLat.lng, e.lngLat.lat);
      }
    });

    map.current.on('moveend', () => {
      if (map.current && onBoundsChange) {
        onBoundsChange(map.current.getBounds());
      }
    });

    return () => {
      map.current?.remove();
      map.current = null;
    };
  }, []);

  // Update markers
  useEffect(() => {
    if (!map.current || !mapLoaded) return;

    // Remove old markers
    markersRef.current.forEach((marker, id) => {
      if (!markers.find(m => m.id === id)) {
        marker.remove();
        markersRef.current.delete(id);
      }
    });

    // Add or update markers
    markers.forEach(markerData => {
      let marker = markersRef.current.get(markerData.id);
      
      if (!marker) {
        // Create custom marker element
        const el = document.createElement('div');
        el.className = 'custom-marker';
        el.style.backgroundColor = markerData.color || '#2196F3';
        el.style.width = '24px';
        el.style.height = '24px';
        el.style.borderRadius = '50%';
        el.style.border = '2px solid white';
        el.style.cursor = 'pointer';
        el.style.boxShadow = '0 2px 4px rgba(0,0,0,0.3)';
        
        if (markerData.icon) {
          el.innerHTML = markerData.icon;
          el.style.display = 'flex';
          el.style.alignItems = 'center';
          el.style.justifyContent = 'center';
          el.style.fontSize = '14px';
        }

        marker = new maplibregl.Marker({ element: el })
          .setLngLat([markerData.lng, markerData.lat]);

        if (markerData.properties?.label) {
          marker.setPopup(
            new maplibregl.Popup({ offset: 25 })
              .setHTML(`
                <div style="padding: 10px;">
                  <h4 style="margin: 0 0 5px 0;">${markerData.properties.label}</h4>
                  ${markerData.properties.description || ''}
                </div>
              `)
          );
        }

        marker.addTo(map.current!);
        
        el.addEventListener('click', () => {
          setActiveMarkerId(markerData.id);
          if (onMarkerClick) {
            onMarkerClick(markerData);
          }
        });

        markersRef.current.set(markerData.id, marker);
      } else {
        // Update existing marker position
        marker.setLngLat([markerData.lng, markerData.lat]);
      }
    });
  }, [markers, mapLoaded, onMarkerClick]);

  // Update center and zoom
  useEffect(() => {
    if (map.current && mapLoaded) {
      map.current.flyTo({
        center: center,
        zoom: zoom,
        duration: 1000
      });
    }
  }, [center, zoom, mapLoaded]);

  // Handle layer visibility
  const toggleLayer = (layerId: string) => {
    if (!map.current) return;
    
    const visibility = !layerVisibility[layerId];
    setLayerVisibility({ ...layerVisibility, [layerId]: visibility });
    
    // Toggle layer visibility on map
    if (map.current.getLayer(layerId)) {
      map.current.setLayoutProperty(
        layerId,
        'visibility',
        visibility ? 'visible' : 'none'
      );
    }
  };

  return (
    <div className="map-viewer-container" style={{ position: 'relative', height }}>
      <div ref={mapContainer} style={{ width: '100%', height: '100%' }} />
      
      {layers.length > 0 && (
        <motion.div 
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          className="layer-control"
        >
          <h4>Layers</h4>
          {layers.map(layer => (
            <label key={layer.id} className="layer-toggle">
              <input
                type="checkbox"
                checked={layerVisibility[layer.id] !== false}
                onChange={() => toggleLayer(layer.id)}
              />
              <span>{layer.name}</span>
            </label>
          ))}
        </motion.div>
      )}

      {activeMarkerId && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="marker-info"
        >
          <button
            className="close-button"
            onClick={() => setActiveMarkerId(null)}
          >
            ✕
          </button>
          <h4>Selected Marker</h4>
          <p>ID: {activeMarkerId}</p>
        </motion.div>
      )}

      <style>{`
        .map-viewer-container {
          border-radius: 8px;
          overflow: hidden;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .maplibregl-ctrl-top-right {
          top: 10px;
          right: 10px;
        }

        .maplibregl-ctrl-bottom-left {
          bottom: 30px;
          left: 10px;
        }

        .maplibregl-ctrl-bottom-right {
          bottom: 10px;
          right: 10px;
        }

        .layer-control {
          position: absolute;
          top: 10px;
          left: 10px;
          background: white;
          padding: 15px;
          border-radius: 8px;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
          max-width: 200px;
        }

        .layer-control h4 {
          margin: 0 0 10px 0;
          font-size: 14px;
          color: #333;
        }

        .layer-toggle {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 8px;
          cursor: pointer;
          font-size: 13px;
        }

        .layer-toggle input[type="checkbox"] {
          cursor: pointer;
        }

        .marker-info {
          position: absolute;
          bottom: 40px;
          left: 50%;
          transform: translateX(-50%);
          background: white;
          padding: 15px;
          border-radius: 8px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
          min-width: 200px;
        }

        .marker-info h4 {
          margin: 0 0 10px 0;
          font-size: 16px;
          color: #333;
        }

        .marker-info p {
          margin: 5px 0;
          font-size: 14px;
          color: #666;
        }

        .close-button {
          position: absolute;
          top: 10px;
          right: 10px;
          background: none;
          border: none;
          font-size: 16px;
          cursor: pointer;
          color: #999;
          padding: 0;
          width: 20px;
          height: 20px;
        }

        .close-button:hover {
          color: #333;
        }

        .custom-marker {
          transition: transform 0.2s;
        }

        .custom-marker:hover {
          transform: scale(1.2);
        }

        .maplibregl-popup-content {
          border-radius: 8px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
        }
      `}</style>
    </div>
  );
};

export default MapViewer;