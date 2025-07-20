import React, { useState, useCallback } from 'react';
import { motion } from 'framer-motion';
import MapViewer, { MapMarker } from './MapViewer';
import MapStyleSelector from './MapStyleSelector';
import { MAP_CONFIG, getMapStyle } from '../config/mapConfig';
import { Card, CardContent, CardHeader, CardTitle, Button, Input, Label } from '@zamaz/ui';
import { Circle, Square, Pentagon, Route, Check, X, Trash2 } from 'lucide-react';

export interface SpatialQuery {
  id: string;
  type: 'radius' | 'polygon' | 'rectangle' | 'corridor';
  name: string;
  geometry: any;
  filters?: {
    deviceTypes?: string[];
    valueRange?: { min: number; max: number };
    timeRange?: { start: Date; end: Date };
  };
}

interface SpatialQueryBuilderProps {
  onQueryCreate: (query: SpatialQuery) => void;
  onQueryUpdate?: (query: SpatialQuery) => void;
  onQueryDelete?: (queryId: string) => void;
  existingQueries?: SpatialQuery[];
  center?: [number, number];
  zoom?: number;
}

const SpatialQueryBuilder: React.FC<SpatialQueryBuilderProps> = ({
  onQueryCreate,
  onQueryUpdate,
  onQueryDelete,
  existingQueries = [],
  center,
  zoom
}) => {
  const [activeQuery, setActiveQuery] = useState<Partial<SpatialQuery> | null>(null);
  const [drawMode, setDrawMode] = useState<'radius' | 'polygon' | 'rectangle' | 'corridor' | null>(null);
  const [queries, setQueries] = useState<SpatialQuery[]>(existingQueries);
  const [selectedQueryId, setSelectedQueryId] = useState<string | null>(null);
  const [mapStyle, setMapStyle] = useState('cartoLight');
  
  // Drawing state
  const [drawingPoints, setDrawingPoints] = useState<[number, number][]>([]);
  const [radius, setRadius] = useState<number>(1000); // meters
  const [corridorWidth, setCorridorWidth] = useState<number>(500); // meters

  const startDrawing = (mode: typeof drawMode) => {
    setDrawMode(mode);
    setDrawingPoints([]);
    setActiveQuery({
      id: `query-${Date.now()}`,
      type: mode!,
      name: `${mode} Query`,
      geometry: null
    });
  };

  const handleMapClick = useCallback((lng: number, lat: number) => {
    if (!drawMode || !activeQuery) return;

    const newPoint: [number, number] = [lng, lat];

    switch (drawMode) {
      case 'radius':
        // Only need center point for radius
        setActiveQuery({
          ...activeQuery,
          geometry: {
            type: 'circle',
            center: newPoint,
            radius: radius
          }
        });
        finishDrawing();
        break;

      case 'rectangle':
        if (drawingPoints.length === 0) {
          setDrawingPoints([newPoint]);
        } else {
          // Second click completes rectangle
          const [first] = drawingPoints;
          setActiveQuery({
            ...activeQuery,
            geometry: {
              type: 'rectangle',
              bounds: [
                [Math.min(first[0], newPoint[0]), Math.min(first[1], newPoint[1])],
                [Math.max(first[0], newPoint[0]), Math.max(first[1], newPoint[1])]
              ]
            }
          });
          finishDrawing();
        }
        break;

      case 'polygon':
      case 'corridor':
        setDrawingPoints([...drawingPoints, newPoint]);
        break;
    }
  }, [drawMode, activeQuery, drawingPoints, radius]);

  const finishDrawing = () => {
    if (!activeQuery || !drawMode) return;

    let finalGeometry;

    switch (drawMode) {
      case 'polygon':
        if (drawingPoints.length >= 3) {
          finalGeometry = {
            type: 'polygon',
            coordinates: [...drawingPoints, drawingPoints[0]] // Close polygon
          };
        }
        break;

      case 'corridor':
        if (drawingPoints.length >= 2) {
          finalGeometry = {
            type: 'corridor',
            path: drawingPoints,
            width: corridorWidth
          };
        }
        break;
    }

    if (finalGeometry || activeQuery.geometry) {
      const completeQuery: SpatialQuery = {
        id: activeQuery.id!,
        type: activeQuery.type!,
        name: activeQuery.name!,
        geometry: finalGeometry || activeQuery.geometry,
        filters: activeQuery.filters
      };

      setQueries([...queries, completeQuery]);
      onQueryCreate(completeQuery);
    }

    // Reset drawing state
    setDrawMode(null);
    setActiveQuery(null);
    setDrawingPoints([]);
  };

  const cancelDrawing = () => {
    setDrawMode(null);
    setActiveQuery(null);
    setDrawingPoints([]);
  };

  const deleteQuery = (queryId: string) => {
    setQueries(queries.filter(q => q.id !== queryId));
    if (onQueryDelete) {
      onQueryDelete(queryId);
    }
    if (selectedQueryId === queryId) {
      setSelectedQueryId(null);
    }
  };

  const updateQueryFilters = (queryId: string, filters: SpatialQuery['filters']) => {
    const updatedQueries = queries.map(q => 
      q.id === queryId ? { ...q, filters } : q
    );
    setQueries(updatedQueries);
    
    const updatedQuery = updatedQueries.find(q => q.id === queryId);
    if (updatedQuery && onQueryUpdate) {
      onQueryUpdate(updatedQuery);
    }
  };

  // Convert queries to map markers/shapes for visualization
  const getQueryMarkers = (): MapMarker[] => {
    return queries
      .filter(q => q.geometry.type === 'circle')
      .map(q => ({
        id: q.id,
        lng: q.geometry.center[0],
        lat: q.geometry.center[1],
        color: '#FF9800',
        icon: '🎯',
        properties: {
          label: q.name,
          description: `Radius: ${q.geometry.radius}m`
        }
      }));
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Spatial Query Builder</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-5">
          <div className="flex gap-3">
            <Button
              variant={drawMode === 'radius' ? 'primary' : 'secondary'}
              onClick={() => startDrawing('radius')}
              disabled={drawMode !== null && drawMode !== 'radius'}
              leftIcon={<Circle className="h-4 w-4" />}
            >
              Radius
            </Button>
            <Button
              variant={drawMode === 'rectangle' ? 'primary' : 'secondary'}
              onClick={() => startDrawing('rectangle')}
              disabled={drawMode !== null && drawMode !== 'rectangle'}
              leftIcon={<Square className="h-4 w-4" />}
            >
              Rectangle
            </Button>
            <Button
              variant={drawMode === 'polygon' ? 'primary' : 'secondary'}
              onClick={() => startDrawing('polygon')}
              disabled={drawMode !== null && drawMode !== 'polygon'}
              leftIcon={<Pentagon className="h-4 w-4" />}
            >
              Polygon
            </Button>
            <Button
              variant={drawMode === 'corridor' ? 'primary' : 'secondary'}
              onClick={() => startDrawing('corridor')}
              disabled={drawMode !== null && drawMode !== 'corridor'}
              leftIcon={<Route className="h-4 w-4" />}
            >
              Corridor
            </Button>
          </div>

          {drawMode && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              className="bg-gray-50 p-4 rounded-lg flex justify-between items-center"
            >
              <div className="space-y-3">
                <p className="text-sm text-gray-600">Drawing: <span className="font-medium text-gray-900">{drawMode}</span></p>
                {drawMode === 'radius' && (
                  <div className="flex items-center gap-3">
                    <Label htmlFor="radius">Radius (m):</Label>
                    <Input
                      id="radius"
                      type="number"
                      value={radius}
                      onChange={(e) => setRadius(Number(e.target.value))}
                      min="100"
                      max="10000"
                      step="100"
                      className="w-32"
                    />
                  </div>
                )}
                {drawMode === 'corridor' && (
                  <div className="flex items-center gap-3">
                    <Label htmlFor="width">Width (m):</Label>
                    <Input
                      id="width"
                      type="number"
                      value={corridorWidth}
                      onChange={(e) => setCorridorWidth(Number(e.target.value))}
                      min="100"
                      max="5000"
                      step="100"
                      className="w-32"
                    />
                  </div>
                )}
                {(drawMode === 'polygon' || drawMode === 'corridor') && drawingPoints.length > 0 && (
                  <p className="text-sm text-gray-600">Points: <span className="font-medium">{drawingPoints.length}</span></p>
                )}
              </div>
              <div className="flex gap-2">
                {(drawMode === 'polygon' || drawMode === 'corridor') && drawingPoints.length >= 2 && (
                  <Button variant="success" size="sm" onClick={finishDrawing} leftIcon={<Check className="h-4 w-4" />}>
                    Finish
                  </Button>
                )}
                <Button variant="danger" size="sm" onClick={cancelDrawing} leftIcon={<X className="h-4 w-4" />}>
                  Cancel
                </Button>
              </div>
            </motion.div>
          )}

          <div className="relative">
            <MapViewer
              center={center || MAP_CONFIG.bounds.stamfordCT.center as [number, number]}
              zoom={zoom || 12}
              markers={getQueryMarkers()}
              height="400px"
              onMapClick={handleMapClick}
              style={getMapStyle(mapStyle)}
            />
            <MapStyleSelector
              currentStyle={mapStyle}
              onStyleChange={setMapStyle}
              position="top-right"
            />
          </div>

          <div className="border-t pt-5">
            <h4 className="text-lg font-semibold mb-4">Active Queries ({queries.length})</h4>
            <div className="space-y-3">
              {queries.map(query => (
                <motion.div
                  key={query.id}
                  className={`bg-gray-50 border rounded-lg p-3 cursor-pointer transition-colors hover:border-primary-500 ${selectedQueryId === query.id ? 'border-primary-500 bg-primary-50' : 'border-gray-200'}`}
                  onClick={() => setSelectedQueryId(query.id)}
                  whileHover={{ scale: 1.02 }}
                >
                  <div className="flex items-center gap-3">
                    <span className="text-xl">
                      {query.type === 'radius' && <Circle className="h-5 w-5 text-gray-600" />}
                      {query.type === 'rectangle' && <Square className="h-5 w-5 text-gray-600" />}
                      {query.type === 'polygon' && <Pentagon className="h-5 w-5 text-gray-600" />}
                      {query.type === 'corridor' && <Route className="h-5 w-5 text-gray-600" />}
                    </span>
                    <span className="flex-1 font-medium">{query.name}</span>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        deleteQuery(query.id);
                      }}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                  
                  {selectedQueryId === query.id && (
                    <motion.div
                      initial={{ height: 0 }}
                      animate={{ height: 'auto' }}
                      className="mt-3 pt-3 border-t border-gray-200 overflow-hidden"
                    >
                      <div className="space-y-3">
                        <h5 className="text-sm font-medium text-gray-700">Filters</h5>
                        <div>
                          <Label htmlFor={`device-types-${query.id}`}>Device Types:</Label>
                          <select
                            id={`device-types-${query.id}`}
                            multiple
                            value={query.filters?.deviceTypes || []}
                            onChange={(e) => {
                              const selected = Array.from(e.target.selectedOptions, option => option.value);
                              updateQueryFilters(query.id, {
                                ...query.filters,
                                deviceTypes: selected
                              });
                            }}
                            className="mt-1 w-full p-2 border border-gray-300 rounded-md text-sm"
                          >
                            <option value="temperature">Temperature</option>
                            <option value="pressure">Pressure</option>
                            <option value="humidity">Humidity</option>
                            <option value="speed">Speed</option>
                          </select>
                        </div>
                        
                        <div>
                          <Label>Value Range:</Label>
                          <div className="flex items-center gap-2 mt-1">
                            <Input
                              type="number"
                              placeholder="Min"
                              value={query.filters?.valueRange?.min || ''}
                              onChange={(e) => updateQueryFilters(query.id, {
                                ...query.filters,
                                valueRange: {
                                  min: Number(e.target.value),
                                  max: query.filters?.valueRange?.max || 100
                                }
                              })}
                              className="flex-1"
                            />
                            <span className="text-gray-500">-</span>
                            <Input
                              type="number"
                              placeholder="Max"
                              value={query.filters?.valueRange?.max || ''}
                              onChange={(e) => updateQueryFilters(query.id, {
                                ...query.filters,
                                valueRange: {
                                  min: query.filters?.valueRange?.min || 0,
                                  max: Number(e.target.value)
                                }
                              })}
                              className="flex-1"
                            />
                          </div>
                        </div>
                      </div>
                    </motion.div>
                  )}
                </motion.div>
              ))}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

export default SpatialQueryBuilder;