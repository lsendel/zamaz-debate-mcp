// Spatial analysis utilities

// Calculate distance between two points using Haversine formula
export function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371e3; // Earth's radius in meters
  const φ1 = lat1 * Math.PI / 180;
  const φ2 = lat2 * Math.PI / 180;
  const Δφ = (lat2 - lat1) * Math.PI / 180;
  const Δλ = (lon2 - lon1) * Math.PI / 180;

  const a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
            Math.cos(φ1) * Math.cos(φ2) *
            Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c; // Distance in meters
}

// Find devices within a certain radius
export function findNearbyDevices(centerDevice: any, allDevices: any[], radius: number): any[] {
  return allDevices
    .filter(device => device.id !== centerDevice.id)
    .map(device => {
      const distance = calculateDistance(
        centerDevice.location.lat,
        centerDevice.location.lng,
        device.location.lat,
        device.location.lng
      );
      return { ...device, distance };
    })
    .filter(device => device.distance <= radius)
    .sort((a, b) => a.distance - b.distance);
}

// Check if a point is inside a polygon
export function isPointInPolygon(point: [number, number], polygon: [number, number][]): boolean {
  let inside = false;
  const [x, y] = point;

  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const [xi, yi] = polygon[i];
    const [xj, yj] = polygon[j];

    const intersect = ((yi > y) !== (yj > y))
        && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
    if (intersect) inside = !inside;
  }

  return inside;
}

// Check if a point is inside a rectangle
export function isPointInRectangle(
  point: [number, number], 
  bounds: [[number, number], [number, number]]
): boolean {
  const [x, y] = point;
  const [[minX, minY], [maxX, maxY]] = bounds;
  return x >= minX && x <= maxX && y >= minY && y <= maxY;
}

// Check if a point is inside a circle
export function isPointInCircle(
  point: [number, number], 
  center: [number, number], 
  radius: number
): boolean {
  const distance = calculateDistance(point[1], point[0], center[1], center[0]);
  return distance <= radius;
}

// Calculate distance from point to line segment
export function distanceToLineSegment(
  point: [number, number],
  lineStart: [number, number],
  lineEnd: [number, number]
): number {
  const [x, y] = point;
  const [x1, y1] = lineStart;
  const [x2, y2] = lineEnd;

  const A = x - x1;
  const B = y - y1;
  const C = x2 - x1;
  const D = y2 - y1;

  const dot = A * C + B * D;
  const lenSq = C * C + D * D;
  let param = -1;

  if (lenSq !== 0) {
    param = dot / lenSq;
  }

  let xx, yy;

  if (param < 0) {
    xx = x1;
    yy = y1;
  } else if (param > 1) {
    xx = x2;
    yy = y2;
  } else {
    xx = x1 + param * C;
    yy = y1 + param * D;
  }

  const dx = x - xx;
  const dy = y - yy;

  return Math.sqrt(dx * dx + dy * dy) * 111320; // Convert degrees to meters (approximate)
}

// Check if a point is within a corridor
export function isPointInCorridor(
  point: [number, number],
  path: [number, number][],
  width: number
): boolean {
  for (let i = 0; i < path.length - 1; i++) {
    const distance = distanceToLineSegment(point, path[i], path[i + 1]);
    if (distance <= width / 2) {
      return true;
    }
  }
  return false;
}

// Create heatmap data from device locations
export function createHeatmapData(devices: any[]): any[] {
  return devices.map(device => ({
    lat: device.location.lat,
    lng: device.location.lng,
    intensity: device.lastValue || 1
  }));
}

// Calculate bounding box for a set of points
export function calculateBoundingBox(points: [number, number][]): [[number, number], [number, number]] {
  if (points.length === 0) {
    return [[0, 0], [0, 0]];
  }

  let minLng = points[0][0];
  let maxLng = points[0][0];
  let minLat = points[0][1];
  let maxLat = points[0][1];

  points.forEach(([lng, lat]) => {
    minLng = Math.min(minLng, lng);
    maxLng = Math.max(maxLng, lng);
    minLat = Math.min(minLat, lat);
    maxLat = Math.max(maxLat, lat);
  });

  return [[minLng, minLat], [maxLng, maxLat]];
}

// Export geographic data to various formats
export function exportGeographicData(data: any[], format: 'geojson' | 'csv' | 'kml'): string {
  switch (format) {
    case 'geojson':
      return JSON.stringify({
        type: 'FeatureCollection',
        features: data.map(item => ({
          type: 'Feature',
          geometry: {
            type: 'Point',
            coordinates: [item.location.lng, item.location.lat]
          },
          properties: {
            id: item.id,
            name: item.name,
            value: item.lastValue,
            ...item.properties
          }
        }))
      }, null, 2);

    case 'csv':
      const headers = ['id', 'name', 'latitude', 'longitude', 'value'];
      const rows = data.map(item => 
        [item.id, item.name, item.location.lat, item.location.lng, item.lastValue || ''].join(',')
      );
      return [headers.join(','), ...rows].join('\n');

    case 'kml':
      const kmlPlacemarks = data.map(item => `
        <Placemark>
          <name>${item.name}</name>
          <description>${item.id} - Value: ${item.lastValue || 'N/A'}</description>
          <Point>
            <coordinates>${item.location.lng},${item.location.lat},0</coordinates>
          </Point>
        </Placemark>
      `).join('');

      return `<?xml version="1.0" encoding="UTF-8"?>
        <kml xmlns="http://www.opengis.net/kml/2.2">
          <Document>
            <name>Telemetry Data Export</name>
            ${kmlPlacemarks}
          </Document>
        </kml>`;

    default:
      throw new Error(`Unsupported format: ${format}`);
  }
}