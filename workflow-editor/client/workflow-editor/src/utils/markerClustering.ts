import { MapMarker } from '../components/MapViewer';

export interface ClusterMarker extends MapMarker {
  pointCount?: number;
  clusterExpansionZoom?: number;
}

export interface ClusterOptions {
  radius: number;
  maxZoom: number;
  minPoints: number;
}

const DEFAULT_OPTIONS: ClusterOptions = {
  radius: 60,
  maxZoom: 14,
  minPoints: 2
};

// Simple clustering algorithm using grid-based approach
export function clusterMarkers(
  markers: MapMarker[], 
  bounds: { north: number; south: number; east: number; west: number },
  zoom: number,
  options: Partial<ClusterOptions> = {}
): ClusterMarker[] {
  const opts = { ...DEFAULT_OPTIONS, ...options };
  
  if (zoom >= opts.maxZoom || markers.length < opts.minPoints) {
    return markers;
  }

  // Calculate grid cell size based on zoom level
  const cellSize = opts.radius / Math.pow(2, zoom);
  
  // Group markers into grid cells
  const grid = new Map<string, MapMarker[]>();
  
  markers.forEach(marker => {
    const cellX = Math.floor(marker.lng / cellSize);
    const cellY = Math.floor(marker.lat / cellSize);
    const cellKey = `${cellX},${cellY}`;
    
    if (!grid.has(cellKey)) {
      grid.set(cellKey, []);
    }
    grid.get(cellKey)!.push(marker);
  });

  // Create clusters and individual markers
  const result: ClusterMarker[] = [];
  
  grid.forEach((cellMarkers, cellKey) => {
    if (cellMarkers.length >= opts.minPoints) {
      // Create cluster
      const centerLng = cellMarkers.reduce((sum, m) => sum + m.lng, 0) / cellMarkers.length;
      const centerLat = cellMarkers.reduce((sum, m) => sum + m.lat, 0) / cellMarkers.length;
      
      result.push({
        id: `cluster-${cellKey}`,
        lng: centerLng,
        lat: centerLat,
        pointCount: cellMarkers.length,
        clusterExpansionZoom: Math.min(zoom + 2, opts.maxZoom),
        color: getClusterColor(cellMarkers.length),
        properties: {
          label: `${cellMarkers.length} devices`,
          isCluster: true
        }
      });
    } else {
      // Add individual markers
      result.push(...cellMarkers);
    }
  });

  return result;
}

// Get color based on cluster size
function getClusterColor(count: number): string {
  if (count < 10) return '#51bbd6';
  if (count < 50) return '#f1f075';
  return '#f28cb1';
}

// Calculate cluster expansion bounds
export function getClusterExpansionBounds(
  clusterId: string,
  markers: MapMarker[]
): { north: number; south: number; east: number; west: number } | null {
  const clusterMarkers = markers.filter(m => 
    m.properties?.clusterId === clusterId
  );
  
  if (clusterMarkers.length === 0) return null;
  
  let north = -90, south = 90, east = -180, west = 180;
  
  clusterMarkers.forEach(marker => {
    north = Math.max(north, marker.lat);
    south = Math.min(south, marker.lat);
    east = Math.max(east, marker.lng);
    west = Math.min(west, marker.lng);
  });
  
  // Add padding
  const latPadding = (north - south) * 0.1;
  const lngPadding = (east - west) * 0.1;
  
  return {
    north: north + latPadding,
    south: south - latPadding,
    east: east + lngPadding,
    west: west - lngPadding
  };
}

// Check if a point is within bounds
export function isInBounds(
  marker: MapMarker,
  bounds: { north: number; south: number; east: number; west: number }
): boolean {
  return marker.lat >= bounds.south && 
         marker.lat <= bounds.north &&
         marker.lng >= bounds.west && 
         marker.lng <= bounds.east;
}

// Custom marker style generator
export function generateMarkerStyle(marker: ClusterMarker): any {
  if (marker.pointCount) {
    // Cluster style
    const size = 20 + (marker.pointCount / 10) * 5;
    return {
      width: `${size}px`,
      height: `${size}px`,
      backgroundColor: marker.color,
      borderRadius: '50%',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      color: 'white',
      fontWeight: 'bold',
      fontSize: '14px',
      border: '2px solid white',
      boxShadow: '0 2px 4px rgba(0,0,0,0.3)',
      cursor: 'pointer',
      transition: 'transform 0.2s',
      ':hover': {
        transform: 'scale(1.1)'
      }
    };
  }
  
  // Individual marker style
  return {
    width: '24px',
    height: '24px',
    backgroundColor: marker.color || '#2196F3',
    borderRadius: '50%',
    border: '2px solid white',
    cursor: 'pointer',
    boxShadow: '0 2px 4px rgba(0,0,0,0.3)',
    transition: 'transform 0.2s',
    ':hover': {
      transform: 'scale(1.2)'
    }
  };
}